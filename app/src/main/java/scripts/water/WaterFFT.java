package scripts.water;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;

public class WaterFFT 
{
    //noise texture parameters
    static int textureSize = 256;
    
    public WaterFFT() {

    }

    public static void noiseSetup()
    {
        //we want to use the frame buffer to generate and save a noise texture to be used in the fft shader on startup 

        int fbid = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbid);

        int texid = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texid);
        
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, textureSize, textureSize, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, texid, fbid, 0);

        // Set the list of draw buffers.
        int DrawBuffers[] = {GL_COLOR_ATTACHMENT0};
        glDrawBuffers(DrawBuffers); // "1" is the size of DrawBuffers


    }

    public static ArrayList<NoiseVector> GaussianNoise2D(int variance, int mean)
    {
        System.out.println("Generating 2D Gaussian Noise Texture for waves");

        java.util.Random r = new java.util.Random();
        double noiseX = 0;
        double noiseY = 0;

        ArrayList<NoiseVector> noiseList = new ArrayList<>();

        for (int i = 0; i < textureSize*textureSize; i++)
        {
            noiseX = r.nextGaussian();
            noiseY = r.nextGaussian();

            NoiseVector noiseVector = new NoiseVector(noiseX, noiseY);
            noiseList.add(noiseVector);
        }

        System.out.println("Finished Generating 2D Gaussian Noise Texture");

        return noiseList;
    }
}