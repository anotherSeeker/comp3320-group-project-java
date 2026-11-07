package scripts.water;
import java.nio.*;
import java.nio.file.Path;
import java.util.*;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.GL_TEXTURE2;
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
import engine.GLErrors;

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
    public static final int localGroupSize = 8;

    //physical size of patch
    public static int patchLength = 1024;

    //scales our displacement textures when we merge together
    public static float displacementScale_lambda = -7.5f;

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

    public static int ifftBufferTexID;
    public static int ifftBufferIndexID;

    public static int displacementTexID;
    public static int displacementIndexID;

    public static int derivativeTexID;
    public static int derivativeIndexID;

    public static int turbulenceTexID;
    public static int turbulenceIndexID;

    //compute shader details
    public static boolean spectraShaderCreated = false;
    public static boolean fComponentShaderCreated = false;
    public static boolean butterflyShaderCreated = false;
    public static boolean ifftShaderCreated = false;
    public static boolean recombineShaderCreated = false;
    public static boolean texMergeShaderCreated = false;

    public static int wavespectraComputeID;
    public static int wavespectraProgramID;
    public static Path wavespectraComputePath = Path.of("assets/shaders/water/wavespectra.comp");

    public static int spectraconjComputeID;
    public static int spectraconjProgramID;
    public static Path spectraconjComputePath = Path.of("assets/shaders/water/wavespectraConj.comp");

    public static int fComponentComputeID;
    public static int fComponentProgramID;
    public static Path fComponentComputePath = Path.of("assets/shaders/water/fourierComponents.comp");

    public static int butterflyComputeID;
    public static int butterflyProgramID;
    public static Path butterflyComputePath = Path.of("assets/shaders/water/precomputeFFTBuffer.comp");

    public static int  ifftComputeID;
    public static int  ifftProgramID;
    public static Path ifftComputePath = Path.of("assets/shaders/water/IFFT.comp");

    public static int  texmergeComputeID;
    public static int  texmergeProgramID;
    public static Path texmergeComputePath = Path.of("assets/shaders/water/displacementTexMerger.comp");

    public static Path waveVertexPath = Path.of("assets/shaders/water/water.vert");
    public static Path waveFragPath = Path.of("assets/shaders/water/water.frag");

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
        //System.out.println("start wave spectra");
        setupSpectra();
        GLErrors.printIfError("setupSpectra");

        //dispatches and waits for completion
        dispatchWaveSpectra();
        GLErrors.printIfError("dispatch waveSpec");


        System.out.println("end compute spectra");
        GLErrors.printIfError("end cWaveSpec");
    }

    private static void setupSpectra()
    {
        if (!spectraShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(wavespectraComputePath);
            
            wavespectraComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(wavespectraComputeID, source);
            glCompileShader(wavespectraComputeID);

            wavespectraProgramID = glCreateProgram();
            glAttachShader(wavespectraProgramID, wavespectraComputeID);
            glLinkProgram(wavespectraProgramID);
            if (GLErrors.printIfError("compile + link setupSpectra") != 0)
            {
                System.out.println(glGetShaderInfoLog(wavespectraComputeID,1024));
                System.out.println(glGetProgramInfoLog(wavespectraProgramID,1024));
            }

            //make the textures
            genSpectrumTex();
            GLErrors.printIfError("genTex setupSpectra");
            
            spectraShaderCreated = true;
        }

        //System.out.println("setup");
    }

    private static void dispatchWaveSpectra()
    {        
        //use our program and bind our textures
        useSpectraShader();
        GLErrors.printIfError("useSpectra");
        System.out.println(glGetShaderInfoLog(wavespectraComputeID,1024));
        System.out.println(glGetProgramInfoLog(wavespectraProgramID,1024));
        //send it
        //System.out.println("dispatch");
        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
        GLErrors.printIfError("gldispatch wave spec");

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
    }

    public static void conjSpectra()
    {
        String source = AssetManager.get().ReadCompute(spectraconjComputePath);

        spectraconjComputeID = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(spectraconjComputeID, source);
        glCompileShader(spectraconjComputeID);

        spectraconjProgramID = glCreateProgram();
        glAttachShader(spectraconjProgramID, spectraconjComputeID);
        glLinkProgram(spectraconjProgramID);

        GLErrors.printIfError("beetio");
        glUseProgram( spectraconjProgramID );

        // System.out.println(glGetProgramInfoLog(spectraconjProgramID,1024));
        // System.out.println(glGetShaderInfoLog(spectraconjComputeID,1024));

        GLErrors.printIfError("hmmmmm");
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, h0TexId);
        glBindImageTexture( 0, h0TexId, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );

        GLErrors.printIfError("asdasdasd");
        glUniform1i(glGetUniformLocation(spectraconjProgramID, "textureSize"), textureSize);
        GLErrors.printIfError("I'm gonna scream");
        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
        GLErrors.printIfError("AHHHHHHHHHHHHHHHHHHHHHH");
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
        //store and load is not allowed for 3 valued formats like this apparently, so we're using RGBA instead of RGB
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 1, waveVecTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

        h0TexId = glGenTextures();
        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, h0TexId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
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
        GLErrors.printIfError("use");

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, noiseTexId);
        glBindImageTexture( 0, noiseTexId, 0, false, 0, GL_READ_ONLY, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+1);
        glBindTexture(GL_TEXTURE_2D, waveVecTexID);
        glBindImageTexture( 1, waveVecTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );
        
        glActiveTexture(GL_TEXTURE0+2);
	    glBindTexture(GL_TEXTURE_2D, h0TexId);
        //need 4 values for h0(k) and h0(-k)
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );
        GLErrors.printIfError("pre");
        
        //size of our textures and physical size of the patch of water
        glUniform1i(glGetUniformLocation(wavespectraProgramID, "patchLength"), patchLength);
        glUniform1i(glGetUniformLocation(wavespectraProgramID, "textureSize"), textureSize);
        GLErrors.printIfError("int");
        //distance to shore from the direction of wind
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "fetch_F"), fetch);
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "windSpeed_U"), windSpeed);
        glUniform1f(glGetUniformLocation(wavespectraProgramID, "depth_H"), depth);
        GLErrors.printIfError("f");

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
            
            if (GLErrors.printIfError("fcomp compile: ") != 0)
                System.out.println(glGetShaderInfoLog(fComponentComputeID,1024));


            fComponentProgramID = glCreateProgram();
            glAttachShader(fComponentProgramID, fComponentComputeID);
            glLinkProgram(fComponentProgramID);

            if (GLErrors.printIfError("fcomp compile: ") != 0)
                System.out.println(glGetProgramInfoLog(fComponentProgramID,1024));

            //make the textures
            genFComponentTex();

            GLErrors.printIfError("fcomp genTex: ");

            fComponentShaderCreated = true;
            GLErrors.printIfError("end setupFComp");
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
        glBindImageTexture( 0, waveVecTexID, 0, false, 0, GL_READ_ONLY, GL_RGBA32F );

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
        glUniform1i(glGetUniformLocation(fComponentProgramID, "patchLength"), patchLength);
    }

    public static void dispatchFComponentShader()
    {
        //System.out.println("start dispatch FComponent");
        float currentTime = (float) glfwGetTime();

        //use our program and bind our textures
        useFComponentShader(currentTime);
        GLErrors.printIfError("useFcomp dispatchFComp");
        //send it
        //System.out.println("dispatch");
        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
        GLErrors.printIfError("postDisptach dispatchFComp");

        //System.out.println("end compute Fourier Components");
    }

    public static void fourierComponentCompute()
    {
        WaterFFT.setupFourierComponentShader();
        WaterFFT.dispatchFComponentShader();
    }

    private static void setupButterflyCompute()
    {
        if (!fComponentShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(butterflyComputePath);
            
            butterflyComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(butterflyComputeID, source);
            glCompileShader(butterflyComputeID);
            
            if (GLErrors.printIfError("butterfly compile: ") != 0)
                System.out.println(glGetShaderInfoLog(butterflyComputeID,1024));

            butterflyProgramID = glCreateProgram();
            glAttachShader(butterflyProgramID, butterflyComputeID);
            glLinkProgram(butterflyProgramID);
            if (GLErrors.printIfError("butterfly link: ") != 0)
                System.out.println(glGetProgramInfoLog(butterflyProgramID,1024));

            //make the textures
            genButterflyTexture();

            butterflyShaderCreated = true;
            System.out.println("end setupButterFly");
        }
        GLErrors.printIfError("end setupButtefly");
    }

    private static void genButterflyTexture()
    {
        butterflyTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, butterflyTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, texSizePower, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 0, butterflyTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );

        butterflyIndexID = AssetManager.get().AddTexture("butterflyTexture",butterflyTexID);
    }

    private static void useButterflyShader()
    {
        glUseProgram( butterflyProgramID );
        //inputs
        glUniform1i(glGetUniformLocation(butterflyProgramID, "texSize"), textureSize);
        //glUniform1i(glGetUniformLocation(butterflyProgramID, "sizePow"), texSizePower);

        //outputs
        glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, butterflyTexID);
        glBindImageTexture( 0, butterflyTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );
    }


    private static void dispatchButterflyPrecomputeShader()
    {
        //use our program and bind our textures
        useButterflyShader();
        if (GLErrors.printIfError("useButterflyShader dispatchButterflyPrecomputeShader") != 0)
        {
            System.out.println(glGetProgramInfoLog(butterflyProgramID,1024));
            System.out.println(glGetShaderInfoLog(butterflyComputeID,1024));
        }

        //dispatch - x = stages size is log2(N) and y = the varying coord size up to N/2 as for n <= N/2 point n+N/2 = -n for our 1D ffts, so we do iffts horizontally and then we do iffts vertically
        glDispatchCompute(texSizePower, textureSize/localGroupSize, 1);

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);

        System.out.println("end precompute butterfly texture");
        if (GLErrors.printIfError("end dispatchButterflyPrecomputeShader") != 0)
        {
            System.out.println(glGetProgramInfoLog(butterflyProgramID,1024));
            System.out.println(glGetShaderInfoLog(butterflyComputeID,1024));
        }
    }

    public static void precomputeButterflyTexture()
    {
        setupButterflyCompute();
        dispatchButterflyPrecomputeShader();
    }

    public static void setupIFFT()
    {
        if (!ifftShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(ifftComputePath);
            
            ifftComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(ifftComputeID, source);
            glCompileShader(ifftComputeID);
            
            if (GLErrors.printIfError("ifft compile: ") != 0)
                System.out.println(glGetShaderInfoLog(ifftComputeID,1024));

            ifftProgramID = glCreateProgram();
            glAttachShader(ifftProgramID, ifftComputeID);
            glLinkProgram(ifftProgramID);
            if (GLErrors.printIfError("ifft link: ") != 0)
                System.out.println(glGetProgramInfoLog(ifftProgramID,1024));

            genIFFTBuffer();

            ifftShaderCreated = true;
            System.out.println("end setupIFFT");

            GLErrors.printIfError("end setupIFFT");
        }
    }

    private static void genIFFTBuffer()
    {
        ifftBufferTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, ifftBufferTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);

        ifftBufferIndexID = AssetManager.get().AddTexture("ifftBuffer",ifftBufferTexID);
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
        glBindImageTexture( 1, inputTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, bufferTexID);
        glBindImageTexture( 2, bufferTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );

        glUniform1i(glGetUniformLocation(ifftProgramID, "size"), textureSize);
        GLErrors.printIfError("beep");
    }

    private static void doIFFT(int inputTexID, int bufferTexID, int pingpong, boolean outputToInput)
    {
        ifft_use(inputTexID, bufferTexID, ifftProgramID); 

        int stageLocation = glGetUniformLocation(ifftProgramID, "stage");
        int ppongLocation = glGetUniformLocation(ifftProgramID, "pingpong");
        int HorizVertPermuteLocation = glGetUniformLocation(ifftProgramID, "H_or_V");

        //do horizontal step
        glUniform1i(HorizVertPermuteLocation, 0);
        for (int i = 0; i < texSizePower; i++)
        {
            pingpong = (pingpong+1) % 2;
            glUniform1i(stageLocation, i);
            glUniform1i(ppongLocation, pingpong);

            glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
            
            glMemoryBarrier(GL_ALL_BARRIER_BITS);
            //glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
        }

        //do vertical step
        glUniform1i(HorizVertPermuteLocation, 1);
        for (int i = 0; i < texSizePower; i++)
        {
            pingpong = (pingpong+1) % 2;
            glUniform1i(stageLocation, i);
            glUniform1i(ppongLocation, pingpong);

            glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
           
            glMemoryBarrier(GL_ALL_BARRIER_BITS);
            //glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
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

        //I have literally no idea what the "permutation and inversion step is" nothing I've read has talked about it but all the code I'm referencing does it
            //hacky method to do this so I don't have to make a whole new shader rn also we're always treating buffer0 as our output
        if (outputToInput)
        {
            //0 = horiz 1 = vert anything else = permute
            //glUniform1i(HorizVertPermuteLocation, 2);
            //glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
            //glMemoryBarrier(GL_ALL_BARRIER_BITS);
        }
    }

    public static void computeWaveFFT()
    {
        setupIFFT();

        doIFFT(dxdzTexID, ifftBufferTexID, 0, true);
        GLErrors.printIfError("doIFFT dxdz: ");
        GLErrors.shaderlogIfError(ifftComputeID);
        GLErrors.programlogIfError(ifftProgramID);

        doIFFT(dydxzTexID, ifftBufferTexID, 0, true);
        GLErrors.printIfError("doIFFT dydxz: ");
        GLErrors.shaderlogIfError(ifftComputeID);
        GLErrors.programlogIfError(ifftProgramID);

        doIFFT(dyxdyzTexID, ifftBufferTexID, 0, true);
        GLErrors.printIfError("doIFFT dyxdyz: ");
        GLErrors.shaderlogIfError(ifftComputeID);
        GLErrors.programlogIfError(ifftProgramID);
            
        doIFFT(dxxdzzTexID, ifftBufferTexID, 0, true);
        GLErrors.printIfError("doIFFT dxxdzz: ");
        GLErrors.shaderlogIfError(ifftComputeID);
        GLErrors.programlogIfError(ifftProgramID);
    }

    public static void setupTexMerge()
    {
        if (!texMergeShaderCreated)
        {
            String source = AssetManager.get().ReadCompute(texmergeComputePath);
            
            texmergeComputeID = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(texmergeComputeID, source);
            glCompileShader(texmergeComputeID);
            GLErrors.printIfError("texmerge compile: ");

            texmergeProgramID = glCreateProgram();
            glAttachShader(texmergeProgramID, texmergeComputeID);
            glLinkProgram(texmergeProgramID);
            GLErrors.printIfError("texmerge link: ");
            
            genWaveShaderTex();

            GLErrors.programlogIfError(texmergeProgramID);
            GLErrors.shaderlogIfError(texmergeComputeID);

            texMergeShaderCreated = true;
            GLErrors.printIfError("end setupWaveShaderMaps");
            System.out.println("end setupWaveShaderMaps");
        }
    }

    private static void genWaveShaderTex()
    {
        displacementTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, displacementTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 0, displacementTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );

        derivativeTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, derivativeTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 1, derivativeTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );

        turbulenceTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, turbulenceTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R32F, textureSize, textureSize, 0, GL_RED, GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 2, turbulenceTexID, 0, false, 0, GL_READ_WRITE, GL_R32F );

        displacementIndexID = AssetManager.get().AddTexture("displacementTexID",displacementTexID);
        derivativeIndexID = AssetManager.get().AddTexture("derivativeTexID",derivativeTexID);
        turbulenceIndexID = AssetManager.get().AddTexture("turbulenceTexID",turbulenceTexID);

        GLErrors.printIfError("merge gen");
    }

    private static void useTexMerge(double deltatime)
    {
        glUseProgram( texmergeProgramID );
        GLErrors.printIfError("e");

        glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, displacementTexID);
        glBindImageTexture( 0, displacementTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );
        GLErrors.printIfError("d");

        glActiveTexture(GL_TEXTURE0+1);
	    glBindTexture(GL_TEXTURE_2D, derivativeTexID);
        glBindImageTexture( 1, derivativeTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F );
        GLErrors.printIfError("k");

        glActiveTexture(GL_TEXTURE0+2);
	    glBindTexture(GL_TEXTURE_2D, turbulenceTexID);
        glBindImageTexture( 2, turbulenceTexID, 0, false, 0, GL_READ_WRITE, GL_R32F );
        GLErrors.printIfError("11");

        glActiveTexture(GL_TEXTURE0+3);
	    glBindTexture(GL_TEXTURE_2D, dxdzTexID);
        glBindImageTexture( 3, dxdzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );
        GLErrors.printIfError("ab");

        glActiveTexture(GL_TEXTURE0+4);
	    glBindTexture(GL_TEXTURE_2D, dydxzTexID);
        glBindImageTexture( 4, dydxzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );
        GLErrors.printIfError("11ba");

        glActiveTexture(GL_TEXTURE0+5);
	    glBindTexture(GL_TEXTURE_2D, dyxdyzTexID);
        glBindImageTexture( 5, dyxdyzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );
        GLErrors.printIfError("23232");

        glActiveTexture(GL_TEXTURE0+6);
	    glBindTexture(GL_TEXTURE_2D, dxxdzzTexID);
        glBindImageTexture( 6, dxxdzzTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F );
        GLErrors.printIfError("11111");

        glUniform1f(glGetUniformLocation(texmergeProgramID, "lambda"), displacementScale_lambda);
        glUniform1f(glGetUniformLocation(texmergeProgramID, "deltaTime"), (float) deltatime);
        GLErrors.printIfError("uniforms merge");
    }

    public static void mergeWaveTextures(double deltatime)
    {
        useTexMerge(deltatime);
        GLErrors.printIfError("useTexMerge");
        GLErrors.programlogIfError(texmergeProgramID);
        GLErrors.shaderlogIfError(texmergeComputeID);

        glDispatchCompute(textureSize/localGroupSize, textureSize/localGroupSize, 1);
        GLErrors.printIfError("disp merge");

        //wait for completion
        glMemoryBarrier(GL_ALL_BARRIER_BITS);
        GLErrors.printIfError("post merge");
        GLErrors.programlogIfError(texmergeProgramID);
        GLErrors.shaderlogIfError(texmergeComputeID);
    }
}