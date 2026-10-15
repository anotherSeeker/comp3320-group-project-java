package scripts.water;
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

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //noise texture parameters

    public static int textureSize = 4;
    //public static int textureSize = 16;
    public static int noiseTexId;
    public static double[] noiseArray;
    

    public WaterFFT() {}

    public static boolean noiseSetup()
    {
        genGaussianNoise2D();

        //we want to generate a normally distributed 2d noise texture, we don't need to render this out we just need to generate it and write it to screen with nearest mapping
        noiseTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexId);

        //texture is clamped in its range
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);	
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        //texture is pure blue outside that range
        float borderColor[] = { 0.0f, 0.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor); 

        //we are using nearest filtering as this should just be individual pixels of different colours 
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        //actually generating the thing
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG, textureSize, textureSize, 0, GL_RG, GL_DOUBLE, noiseArray);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        return true;
    }

    public static double[] genGaussianNoise2D()
    {
        System.out.println("Generating 2D Gaussian Noise values for our waves");

        java.util.Random r = new java.util.Random();
        double noiseX;
        double noiseY;

        noiseArray = new double[textureSize*textureSize*2];

        for (int i = 0; i < textureSize*textureSize; i++)
        {
            //values between -1 and 1 normally distributed
            noiseX = r.nextGaussian();
            noiseY = r.nextGaussian();

            //rendering this as a texture will have a lot of low values due to our distribution, test it scaled as well later and we probably don't want to treat this data as a texture anyway?
            noiseArray[i*2] = noiseX;
            noiseArray[i*2 + 1] = noiseY;

            System.out.println(noiseX + " : " + noiseY);
        }

        System.out.println("Finished Generating 2D Gaussian Noise values");

        return noiseArray;
    }
}