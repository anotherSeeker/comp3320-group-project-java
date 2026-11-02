package scripts.water;
import java.nio.*;
import java.nio.file.Path;
import java.util.*;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.ARBComputeShader.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.ARBCopyImage.*;

import static org.lwjgl.glfw.GLFW.*;

import engine.AssetManager;

// import static org.lwjgl.opengl.GL42.*;
// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// // import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //I picked depth wholly at random, windspeed was an loose example stated in the wikiwaves page on jonswap kind of as a throwaway line but that's good enough for me
    public static float depth = 30.0f; //H or sometimes D: depth of the water, I believe technical constraints mean we can't have a different one for each point on the surface? we'd need to recalculate a new map for every part of the ocean which is kinda infeasible unless I'm horrifically mistaken
    public static float windSpeed = 25.0f; //U: windspeed, I believe meters per second?
    public static float fetch = 800000.0f; //F: distance to shore from the direction that the wind is blowing from

    //acerola/jump trajectory used 4 waves of different length to cascade and hide the tiling properties of sinewaves over large spaces. 
    //acerola used 1024*1024 textures but we're gonna start smaller and scale up if feasible
    public static int texSizePower = 8; //textureSize = 2^texSizePower;
    public static int textureSize = 256; //gotta be a power of 2
    //to dispatch the correct number of work group based on their local size.
    public static int localGroupSize = 8;

    //Patches are 1km by 1km in physical size
    public static float patchLength = 1000;

    //the wavenumbers, kx, ky, and angular frequency / dispersion relation omega
    public static ArrayList<Vector3f> waveVectorList;

    //texture ids for sending to shaders
    public static int noiseTexId;
    public static int waveVecTexID;
    public static int h0TexId;

    public static int noiseIndexId;
    public static int waveVecIndexID;
    public static int h0IndexID;

    public static int dxdzTexID;
    public static int dxdzIndexID;

    public static int dydxzTexID;
    public static int dydxzIndexID;

    public static int dyxdyzTexID;
    public static int dyxdyzIndexID;

    public static int dxxdzzTexID;
    public static int dxxdzzIndexID;

    public static int butterflyTexID;
    public static int butterflyIndexID;

    //compute shader details
    public static boolean spectraShaderCreated = false;
    public static boolean fComponentShaderCreated = false;
    public static boolean fftShaderCreated = false;

    public static int wavespectraComputeID;
    public static int wavespectraProgramID;
    public static Path wavespectraComputePath = Path.of("assets/shaders/water/wavespectra.comp");

    public static int fComponentComputeID;
    public static int fComponentProgramID;
    public static Path fComponentComputePath = Path.of("assets/shaders/water/fourierComponents.comp");//Path.of("assets/shaders/water/waveTimeVariant.comp");

    public static int butterflyComputeID;
    public static int butterflyProgramID;
    public static Path butterflyComputePath = Path.of("assets/shaders/water/precomputeFFTBuffer.comp");

    public static int ifftHorizComputeID;
    public static int ifftHorizProgramID;
    public static Path ifftHorizComputePath = Path.of("assets/shaders/water/horizIFFT.comp");

    public static int ifftVertComputeID;
    public static int ifftVertProgramID;
    public static Path ifftVertComputePath = Path.of("assets/shaders/water/vertIFFT.comp");

    //bytebuffers for creating our textures
    public static ByteBuffer noiseBytes;
    public static FloatBuffer noiseFloats;    

    public WaterFFT() {}

    public static void setSizeParams(int power)
    {
        texSizePower = power;
        textureSize = 1;
        for (int i = 0; i < texSizePower; i++)
        {
            textureSize *= 2;
        }
    }

    public static boolean noiseSetup()
    {
        genGaussianNoise2D();
        
        return true;
    }

    public static FloatBuffer genGaussianNoise2D()
    {
        System.out.println("Generating 2D Gaussian Noise values for our waves");

        java.util.Random r = new java.util.Random();
        float noiseX;
        float noiseY;

        //width*height*2*4 for r and g channels as floats (4 bytes)
        int numEntries = textureSize*textureSize*2;
        int bufferSize = numEntries*4;

        noiseBytes = ByteBuffer.allocateDirect(bufferSize);
        noiseFloats = noiseBytes.asFloatBuffer();

        for (int i = 0; i < textureSize*textureSize; i++)
        {
            //values between -1 and 1 normally distributed
            noiseX = (float) (r.nextGaussian());
            noiseY = (float) (r.nextGaussian());

            noiseFloats.put(noiseX);
            noiseFloats.put(noiseY);

            //System.out.println(noiseX + " : " + noiseY);
        }

        noiseBytes.rewind();

        noiseIndexId = AssetManager.get().createTexture(noiseBytes, textureSize, GL_RG32F, GL_RG, "gaussianData");
        noiseTexId = AssetManager.get().getTexture(noiseIndexId).ID;

        System.out.println("Finished Generating 2D Gaussian Noise values");

        return noiseFloats;
    }

    public static void computeWaveSpectrum()
    {
        setupSpectraShader();
        //dispatches and waits for completion
        dispatchWaveSpectra();
        System.out.println("compute spectra done");
    }

    private static void setupSpectraShader()
    {
        if (!spectraShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(wavespectraComputePath);
            
            wavespectraComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(wavespectraComputeID, source);
            glCompileShader(wavespectraComputeID);
            
            System.out.println(glGetShaderInfoLog(wavespectraComputeID,1024));

            wavespectraProgramID = glCreateProgram();
            glAttachShader(wavespectraProgramID, wavespectraComputeID);
            glLinkProgram(wavespectraProgramID);

            System.out.println(glGetProgramInfoLog(wavespectraProgramID,1024));

            //make the textures
            genSpectrumTex();
            
            spectraShaderCreated = true;
        }

        //System.out.println("setup");
    }

    private static void dispatchWaveSpectra()
    {        
        //use our program and bind our textures
        useSpectraShader();
        //send it
        //System.out.println("dispatch");
        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
        System.out.println(glGetShaderInfoLog(wavespectraComputeID,1024));
    }

    //binds our kxkyfreq and complex displacement textures so we can use them in wavespectra.comp
    private static void genSpectrumTex()
    {
        //create
        waveVecTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+1);
        glBindTexture(GL_TEXTURE_2D, waveVecTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        //glTexStorage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize);
        //store and load is not allowed for 3 valued formats like this apparently, so we're using RGBA instead of RGB
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 1, waveVecTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

        h0TexId = glGenTextures();
        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, h0TexId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        //glTexStorage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize);
        //We actually do need RGBA for h0(k) and h0(-k)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

        //add to texture index for engine
        waveVecIndexID = AssetManager.get().AddTexture("waveDisplacementTex",waveVecTexID);
        h0IndexID = AssetManager.get().AddTexture("h0DisplacementTex",h0TexId);
    }

    public static void useSpectraShader()
    {
        glUseProgram( wavespectraProgramID );

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, noiseTexId);
        glBindImageTexture( 0, noiseTexId, 0, false, 0, GL_READ_ONLY, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+1);
        glBindTexture(GL_TEXTURE_2D, waveVecTexID);
        glBindImageTexture( 1, waveVecTexID, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F );
        
        glActiveTexture(GL_TEXTURE0+2);
	    glBindTexture(GL_TEXTURE_2D, h0TexId);
        //need 4 values for h0(k) and h0(-k)
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F );
        
        //size of our textures and physical size of the patch of water
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "patchLength"), patchLength);
        glUniform1i(glGetUniformLocation(wavespectraProgramID, "textureSize"), textureSize);
        //distance to shore from the direction of wind
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "fetch_F"), fetch);
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "windSpeed_U"), windSpeed);
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "depth_H"), depth);

        //System.out.println("bind");
    }

    //h(k,t) textures gen

    public static void setupFourierComponentShader()
    {
        if (!fComponentShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(fComponentComputePath);
            
            fComponentComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(fComponentComputeID, source);
            glCompileShader(fComponentComputeID);
            
            System.out.println(glGetShaderInfoLog(fComponentComputeID,1024));

            fComponentProgramID = glCreateProgram();
            glAttachShader(fComponentProgramID, fComponentComputeID);
            glLinkProgram(fComponentProgramID);

            System.out.println(glGetProgramInfoLog(fComponentProgramID,1024));

            //make the textures
            genFComponentTex();

            fComponentShaderCreated = true;
        }
    }

    public static void genFComponentTex()
    {
        //create
        dxdzTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, dxdzTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 2, dxdzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F);

        dydxzTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+3);
        glBindTexture(GL_TEXTURE_2D, dydxzTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 3, dydxzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F);

        dyxdyzTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+4);
        glBindTexture(GL_TEXTURE_2D, dyxdyzTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 4, dyxdyzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F);

        dxxdzzTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+5);
	    glBindTexture(GL_TEXTURE_2D, dxxdzzTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 5, dxxdzzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        //add to texture index for engine
        dxdzIndexID = AssetManager.get().AddTexture("dxdz_Tex",dxdzTexID);
        dydxzIndexID = AssetManager.get().AddTexture("dydxz_Tex",dydxzTexID);
        dyxdyzIndexID = AssetManager.get().AddTexture("dyxdyz_Tex",dyxdyzTexID);
        dxxdzzIndexID = AssetManager.get().AddTexture("dxxdzz_Tex",dxxdzzTexID);
    }

    private static void useFComponentShader(float time)
    {
        glUseProgram( fComponentProgramID );

        //inputs
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, waveVecTexID);
        glBindImageTexture( 0, waveVecTexID, 0, false, 0, GL_READ_ONLY, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+1);
        glBindTexture(GL_TEXTURE_2D, h0TexId);
        glBindImageTexture( 1, h0TexId, 0, false, 0, GL_READ_ONLY, GL_RGBA32F );

        //outputs
        glActiveTexture(GL_TEXTURE0+2);
	    glBindTexture(GL_TEXTURE_2D, dxdzTexID);
        glBindImageTexture( 2, dxdzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+3);
	    glBindTexture(GL_TEXTURE_2D, dydxzTexID);
        glBindImageTexture( 3, dydxzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+4);
	    glBindTexture(GL_TEXTURE_2D, dyxdyzTexID);
        glBindImageTexture( 4, dyxdyzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+5);
	    glBindTexture(GL_TEXTURE_2D, dxxdzzTexID);
        glBindImageTexture( 5, dxxdzzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glUniform1f(glGetUniformLocation(fComponentProgramID, "time"), time);
    }

    public static void dispatchFComponentShader()
    {
        float currentTime = (float) glfwGetTime();
        //use our program and bind our textures
        useFComponentShader(currentTime);
        //send it
        //System.out.println("dispatch");
        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);

        System.out.println("compute Fourier Components done");
    }

    public static void useButterflyShader()
    {
        
    }


    public static void dispatchButterflyPrecomputeShader()
    {
        //use our program and bind our textures
        useButterflyShader();

        //dispatch - x size is log2(N) and y size is N/2
        glDispatchCompute(texSizePower, textureSize/2/localGroupSize, 1);

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);

        System.out.println("precompute fourier buffer done");
    }


    private static void ifft_use(int inputTexID, int bufferTexID, int ifftProgramID)
    {
        glUseProgram( ifftProgramID );

        //inputs
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, butterflyTexID);
        glBindImageTexture( 0, butterflyTexID, 0, false, 0, GL_READ_ONLY, GL_RGBA32F );

        glActiveTexture(GL_TEXTURE0+1);
        glBindTexture(GL_TEXTURE_2D, inputTexID);
        glBindImageTexture( 1, inputTexID, 0, false, 0, GL_READ_ONLY, GL_RGBA32F );

        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, bufferTexID);
        glBindImageTexture( 2, bufferTexID, 0, false, 0, GL_READ_ONLY, GL_RGBA32F );

        glUniform1f(glGetUniformLocation(ifftProgramID, "size"), textureSize);
    }

    private static void _IFFT(int inputTexID, int bufferTexID, int pingpong, boolean outputToInput)
    {
        ifft_use(inputTexID, bufferTexID, ifftHorizProgramID); 

        int stepLocation = glGetUniformLocation(ifftHorizProgramID, "step");
        int ppongLocation = glGetUniformLocation(ifftHorizProgramID, "pingpong");
        for (int i = 0; i < texSizePower; i++)
        {
            pingpong = (pingpong+1) % 2;
            glUniform1f(stepLocation, i);
            glUniform1i(ppongLocation, pingpong);

            glDispatchCompute(texSizePower, textureSize/2/localGroupSize, 1);
            //DO WE NEED TO WAIT?? I ASSUME SO??
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        ifft_use(inputTexID, bufferTexID, ifftVertProgramID); 
        stepLocation = glGetUniformLocation(ifftVertProgramID, "step");
        ppongLocation = glGetUniformLocation(ifftVertProgramID, "pingpong");
        for (int i = 0; i < texSizePower; i++)
        {
            pingpong = (pingpong+1) % 2;
            glUniform1f(stepLocation, i);
            glUniform1i(ppongLocation, pingpong);

            glDispatchCompute(texSizePower, textureSize/2/localGroupSize, 1);
            //DO WE NEED TO WAIT?? I ASSUME SO??
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        //if we're treating the input as output, we need to make sure the final buffer is in the input text and vice versa.
        if (pingpong == 1 && outputToInput)
        {
            //copy buffer to input texture
            glCopyImageSubData(bufferTexID, GL_TEXTURE_2D, 0, 0, 0, 0, inputTexID, GL_TEXTURE_2D, 0, 0, 0, 0, textureSize, textureSize, 1);
        }
        if (pingpong == 0 && !outputToInput)
        {
            //copy input to buffer texture
            glCopyImageSubData(inputTexID, GL_TEXTURE_2D, 0, 0, 0, 0, bufferTexID, GL_TEXTURE_2D, 0, 0, 0, 0, textureSize, textureSize, 1);
        }
    }
}