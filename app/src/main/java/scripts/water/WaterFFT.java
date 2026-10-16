package scripts.water;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Vector2f;
import static org.lwjgl.opengl.GL30.GL_RG;

import engine.AssetManager;

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //physical parameters for the ocean
    public static double gravity = 9.81;
    public static double waterDensity = 1000;
    public static double surfaceTensionCoefficient = 0.074;

    //I picked depth wholly at random, windspeed was an loose example stated in the wikiwaves page on jonswap kind of as a throwaway line but that's good enough for me
    public static double depth = 50;
    public static double windSpeed = 20;

    //acerola/jump trajectory used 4 waves of different length to cascade and hide the tiling properties of sinewaves over large spaces. acerola used 1024*1024 textures but we're gonna start smaller
    public static int textureSize = 512;

    //texture ids for sending to shaders
    public static int noiseTexId;
    public static int waveVectorTexId;

    //bytebuffers for creating our textures
    public static ByteBuffer noiseBytes;
    public static FloatBuffer noiseFloats;
    

    public WaterFFT() {}

    public static boolean noiseSetup()
    {
        genGaussianNoise2D();

        noiseTexId = AssetManager.get().createTexture(noiseBytes, textureSize, GL_RG, "gaussianNoise_Water");
        
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

        System.out.println("Finished Generating 2D Gaussian Noise values");

        return noiseFloats;
    }

    //working through everything in my head on the page here
    public void computeWaveVectorMap(Vector2f gaussianNoise, Vector2f wavenumbers)
    {
        double aFreq = dispersionRelation(wavenumbers);

        double h_0 = 1/(Math.sqrt(2)) * (gaussianNoise.x + gaussianNoise.y) * Math.sqrt(doJONSWAP() * doDirectionalSpread(wavenumbers));


    }

    public double dispersionRelation(Vector2f k)
    {
        double magnitude = waveVectorMagnitude(k);

        double aFreq = Math.sqrt(gravity * magnitude * Math.tanh(magnitude * depth));
        //includes variables for capilliary waves? might look better?
        //double aFreq = Math.sqrt((gravity * magnitude + (surfaceTensionCoefficient / waterDensity) * Math.pow(magnitude, 3))* Math.tanh(magnitude * depth));

        return aFreq;
    }

    public double waveVectorMagnitude(Vector2f k)
    {
        return Math.sqrt(k.x*k.x + k.y*k.y);
    }

    public double wavelength(Vector2f k)
    {
        return 1.0;
    }

    public double doJONSWAP()
    {
        return 1.0;
    }

    public double doDirectionalSpread(Vector2f k)
    {
        double angularFrequency = dispersionRelation(k);

        return 1.0;
    }
}