package scripts.water;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL30.*;

import engine.AssetManager;

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //noise texture parameters

    public static int textureSize = 128;
    //public static int textureSize = 16;
    public static int noiseTexId;

    //not deleting the old array for now just in case, delete it later
    public static double[] noiseArray;

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

    public void calculateJonswap()
    {

    }
}