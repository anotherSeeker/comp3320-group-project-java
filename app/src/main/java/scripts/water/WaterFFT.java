package scripts.water;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BORDER_COLOR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameterfv;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import engine.AssetManager;

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //noise texture parameters

    public static int textureSize = 256;
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
            noiseX = (float) r.nextGaussian();
            noiseY = (float) r.nextGaussian();

            noiseFloats.put(noiseX);
            noiseFloats.put(noiseY);

            //System.out.println(noiseX + " : " + noiseY);
        }

        noiseBytes.rewind();

        System.out.println("Finished Generating 2D Gaussian Noise values");

        return noiseFloats;
    }

    // public static double[] genGaussianNoise2D()
    // {
    //     System.out.println("Generating 2D Gaussian Noise values for our waves");

    //     java.util.Random r = new java.util.Random();
    //     double noiseX;
    //     double noiseY;

    //     noiseArray = new double[textureSize*textureSize*2];

    //     for (int i = 0; i < textureSize*textureSize; i++)
    //     {
    //         //values between -1 and 1 normally distributed
    //         noiseX = r.nextGaussian();
    //         noiseY = r.nextGaussian();

    //         //rendering this as a texture will have a lot of low values due to our distribution, test it scaled as well later and we probably don't want to treat this data as a texture anyway?
    //         noiseArray[i*2] = noiseX;
    //         noiseArray[i*2 + 1] = noiseY;

    //         System.out.println(noiseX + " : " + noiseY);
    //     }

    //     System.out.println("Finished Generating 2D Gaussian Noise values");

    //     return noiseArray;
    // }
}