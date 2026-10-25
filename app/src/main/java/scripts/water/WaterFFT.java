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
    public static int textureSize = 16; //these have to be powers of 2 or we lose efficiency on the fft

    //the wavenumbers, kx, ky, and angular frequency / dispersion relation omega
    public static ArrayList<Vector3f> waveVectorList;

    //texture ids for sending to shaders
    public static int noiseTexId;
    public static int waveVecTexID;
    public static int h0TexId;

    public static int noiseIndexId;
    public static int waveVecIndexID;
    public static int h0IndexID;

    //compute shader details
    public static boolean spectraShaderCreated = false;
    public static int wavespectraComputeID;
    public static int wavespectraProgramID;
    public static Path wavespectraComputePath = Path.of("assets/shaders/water/wavespectra.comp"); //Path.of("assets/shaders/water/wavespectra.comp"); 

    //bytebuffers for creating our textures
    public static ByteBuffer noiseBytes;
    public static FloatBuffer noiseFloats;

    public static ByteBuffer k;
    public static FloatBuffer kF;

    public static ByteBuffer h0;
    public static FloatBuffer h0F;

    

    // public static ByteBuffer displacementBuffer;
    // public static FloatBuffer displacementFloats;

    // public static ByteBuffer wavevectorBuffer;
    // public static FloatBuffer wavevectorFloats;
    

    public WaterFFT() {}

    public static boolean noiseSetup()
    {
        genGaussianNoise2D();

        //computeSpectrum();
        
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
        //System.out.println("done");

        // //kx,ky,omega so rgb
        // int kSize = textureSize * textureSize*3*4;
        // //h0_real, h0_complex so rg
        // int h0Size = textureSize * textureSize*2*4;

        // k = ByteBuffer.allocateDirect(kSize);

        // h0 = ByteBuffer.allocateDirect(h0Size);

        // glActiveTexture(GL_TEXTURE0+1);
        // glGetTexImage(GL_TEXTURE_2D, 0, GL_RGB, GL_FLOAT, k);

        // glActiveTexture(GL_TEXTURE0+2);
        // glGetTexImage(GL_TEXTURE_2D, 0, GL_RG, GL_FLOAT, h0);

        // h0 = h0.order(ByteOrder.nativeOrder());
        // h0F = h0.asFloatBuffer();

        // k = h0.order(ByteOrder.nativeOrder());
        // kF = k.asFloatBuffer();
        // System.out.println("k");
        // printFloatBuffer(kF);
        // System.out.println("h");
        // printFloatBuffer(h0F);

        // System.out.println("read");
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
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG32F, textureSize, textureSize, 0, GL_RG , GL_FLOAT, (FloatBuffer) null);
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_READ_WRITE, GL_RG32F);

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
        glBindImageTexture( 2, h0TexId, 0, false, 0, GL_WRITE_ONLY, GL_RG32F );
        
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


    //this should all be in a compute shader instead for speed

    // public static boolean precomputeWavenumbers_kx_ky_omega()
    // {
    //     //thinking about our wavevectors and angular freq / dispersion relation / omega

    //     //iterate over - (textureSize/2) -> textureSize/2 in both x, and y
    //     //the i, k values are then our wave vectors and we can plug those values of k in to precalculate our omega value
    //     //then once we got that we write those three to the rgb of a second texture map, 
    //     //should be pretty easy now that I've thought it through and am not tired ?
        
    //     //wait I think we reverse that and iterate from positive to negative, 
    //     //pos "1,1" should be top left and pos "-1,-1" bottom right so yeah

    //     waveVectorList = new ArrayList<>();

    //     for (int ky = textureSize/2; ky > -textureSize/2; ky--)
    //     {
    //         for (int kx = textureSize/2; kx > -textureSize/2; kx-- )
    //         {
    //             //[kx, ky] is a vector pointing in a direction based on the points of the texture, hopefully, unless I'm just that dumb
    //             //we want them to all have different magnitudes based on distance from the center as well so I'm pretty sure that should be fine?

    //             double dispersion_omega = dispersionRelation_omega(kx, ky);

    //             //rgb channels for kx, ky, omega, we still need to calculate h0, but we're precaching these in a texture
    //             waveVectorList.add(new Vector3f((float) kx, (float) ky, (float) dispersion_omega));
    //         }
    //     }
    //     //put in texture dum dum

    //     return true;
    // }

    // public void computeDisplacementMap_h0(Vector2f gaussianNoise, float kx, float ky)
    // {
    //     double omega = dispersionRelation_omega(kx, ky);
    //     double theta = windWaveDirection_Theta(kx, ky);

    //     double xi_x = (double) gaussianNoise.x;
    //     double xi_y = (double) gaussianNoise.y;

    //     //we pregen our wave numbers and angular frequency and we'll pass them into this individually
    //     double h_0_real   = (1.0/(Math.sqrt(2))) * xi_x * Math.sqrt(doJONSWAP(omega) * doDirectionalSpread(omega, theta));
    //     double h_0_complex = (1.0/(Math.sqrt(2))) * xi_y * Math.sqrt(doJONSWAP(omega) * doDirectionalSpread(omega, theta));

    //     //save kx, ky, omega into a texture and h_0_real, ..._complex into another
    // }

    // public static double dispersionRelation_omega(float kx, float ky)
    // {
    //     double magnitude = waveVectorMagnitude_k(kx, ky);

    //     double aFreq = Math.sqrt(gravity * magnitude * Math.tanh(magnitude * depth));
    //     //includes variables for capilliary waves? might look better?
    //     //double aFreq = Math.sqrt((gravity * magnitude + (surfaceTensionCoefficient / waterDensity) * Math.pow(magnitude, 3))* Math.tanh(magnitude * depth));

    //     return aFreq;
    // }

    // public static double waveVectorMagnitude_k(Vector2f k)
    // {
    //     return Math.sqrt(k.x*k.x + k.y*k.y);
    // }

    // public static double waveVectorMagnitude_k(float kx, float ky)
    // {
    //     return Math.sqrt(kx*kx + ky*ky);
    // }

    // //angle of the wave as compared to the wind direction
    // public static double windWaveDirection_Theta(float kx, float ky)
    // {
    //     //theta = arctan(ky/kx)
    //     return Math.atan(ky/kx);
    // }

    // //wavenumbers/vectors kx,ky and angular freq omega
    // public static double doDirectionalSpread(double omega, double theta)
    // {
    //     //decide which spread functions we want to use
    //     double spread = 0;

    //     //for now just using the "positive cosine squared" spreading that tessendorf used   
    //     if (theta > (-Math.PI/2) && theta < (Math.PI/2))
    //     {
    //         spread = (2/Math.PI) * Math.pow(Math.cos(theta), 2);
    //     }

    //     return spread;
    // }

    // //jonswap is based on a bunch of physical parameters for the ocean that we can tweak and the angular frequency of the specific wave we're looking at
    // public static double doJONSWAP(double omega)
    // {
    //     //peak frequency of the wave spectrum
    //     double omegaPeak = omega_p_J();
    //     //amplitude of this wave
    //     double alpha = amplitudeAlpha_J();
    //     //I believe this is the surface tension term?
    //     double sigma = sigma_J(omegaPeak, omega);
    //     //exponent r
    //     double r = exponent_r_J(omegaPeak, omega, sigma);


    //     //the actual equation bits
    //     double a = (alpha*gravity*gravity) / Math.pow(omega, 5);

    //     double b = Math.exp(-5/4 * Math.pow((omegaPeak/omega), 4));

    //     double S_jonswap = a*b*Math.pow(GAMMA_J, r);

    //     return S_jonswap;
    // }

    // //I believe that this is the surface tension parameter and that jonswap uses a slightly different one for some reason?
    // public static double sigma_J(double peakFrequency, double waveFrequency)
    // {
    //     double alpha = 0.09;
    //     if (peakFrequency >= waveFrequency)
    //         alpha = 0.07;

    //     return alpha;
    // }

    // //JONSWAP amplitude
    // public static double amplitudeAlpha_J()
    // {
    //     //as far as I understand 0.076 is the real surface tension of water? perhaps it differs in ocean conditions? again not an oceanographer
    //     return 0.076 * Math.pow((windSpeed*windSpeed)/(fetch*gravity), 0.22);
    // }

    // //e^[stuff] part of the jonswap equation you might not have heard but I'm not an oceanographer
    // public static double exponent_r_J(double omegaPeak, double omega, double sigma)
    // {
    //     double a = Math.pow(omega - omegaPeak, 2);
    //     double b = 2*sigma*sigma*omegaPeak*omegaPeak;

    //     double c = -(a/b);
    //     double r = Math.exp(c);

    //     return r;
    // }

    // //peak angular frequency
    // public static double omega_p_J()
    // {
    //     return (22 * ((gravity*gravity)/(windSpeed * fetch)));
    // }
}