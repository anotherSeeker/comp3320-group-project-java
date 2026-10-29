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


import engine.AssetManager;

// import static org.lwjgl.opengl.GL42.*;
// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// // import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //physical parameters for the ocean
    public static double gravity = 9.81;
    public static double waterDensity = 1000;
    public static double surfaceTensionCoefficient = 0.074;

    //I picked depth wholly at random, windspeed was an loose example stated in the wikiwaves page on jonswap kind of as a throwaway line but that's good enough for me
    public static double depth = 50; //H or sometimes D: depth of the water, I believe technical constraints mean we can't have a different one for each point on the surface? we'd need to recalculate a new map for every part of the ocean which is kinda infeasible unless I'm horrifically mistaken
    public static double windSpeed = 20; //U: windspeed, I believe meters per second?
    public static double fetch = 2000; //F: distance to shore from the direction that the wind is blowing from
    public static final double GAMMA_J = 3.3; //gamma: the "peak enhancement factor", from the JONSWAP spectrum, do not ask me I am not an oceanographer

    //acerola/jump trajectory used 4 waves of different length to cascade and hide the tiling properties of sinewaves over large spaces. 
    //acerola used 1024*1024 textures but we're gonna start smaller and scale up if feasible
    public static int textureSize = 256; //these have to be powers of 2 or we lose efficiency on the fft

    //the wavenumbers, kx, ky, and angular frequency / dispersion relation omega
    public static ArrayList<Vector3f> waveVectorList;

    //texture ids for sending to shaders
    public static int noiseTexId;
    public static int waveVecTexID;
    public static int h0TexId;

    public static int noiseIndexId;
    public static int waveVecIndexID;
    public static int h0IndexID;

    public static int dyTexID;
    public static int dxdzTexID;

    public static int dyIndexID;
    public static int dxdzIndexID;

    //compute shader details
    public static boolean spectraShaderCreated = false;
    public static boolean fComponentShaderCreated = false;
    public static boolean fftShaderCreated = false;

    public static int wavespectraComputeID;
    public static int wavespectraProgramID;
    public static Path wavespectraComputePath = Path.of("assets/shaders/water/wavespectra.comp");

    public static int fComponentComputeID;
    public static int fComponentProgramID;
    public static Path fComponentComputePath = Path.of("assets/shaders/water/waveTimeVariant.comp");

    //bytebuffers for creating our textures
    public static ByteBuffer noiseBytes;
    public static FloatBuffer noiseFloats;    

    public WaterFFT() {}

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
        System.out.println("compute");
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
        glDispatchCompute(textureSize, textureSize, 1);

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
            //store and load is not allowed for 3 valued formats like this apparently, so we're using RGBA instead of RGB
        glBindImageTexture( 1, waveVecTexID, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F );
        
        glActiveTexture(GL_TEXTURE0+2);
	    glBindTexture(GL_TEXTURE_2D, h0TexId);
        //need 4 values for h0(k) and h0(-k)
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F );
        
        System.out.println("bind");
    }

    private static void printFloatBuffer(FloatBuffer buffer)
    {
        while (buffer.hasRemaining())
        {
            System.out.println(buffer.get());
        }

        buffer.rewind();
    }


    //h(k,t) textures gen

    private static void setupFourierComponentShader()
    {
        if (!fComponentShaderCreated)
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
    }

    private static void genFComponentTex()
    {
        //create
        dyTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+2);
        glBindTexture(GL_TEXTURE_2D, waveVecTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 2, waveVecTexID, 0, false, 0, GL_READ_WRITE, GL_RG32F);

        dxdzTexID = glGenTextures();
        glActiveTexture(GL_TEXTURE0+3);
        glBindTexture(GL_TEXTURE_2D, dxdzTexID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, textureSize, textureSize, 0, GL_RGBA , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 3, dxdzTexID, 0, false, 0, GL_READ_WRITE, GL_RGBA32F);

        //add to texture index for engine
        dyIndexID = AssetManager.get().AddTexture("dyTex",dyTexID);
        dxdzIndexID = AssetManager.get().AddTexture("dxdzTex",dxdzTexID);
    }
}