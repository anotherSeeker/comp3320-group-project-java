package scripts;

import static org.lwjgl.glfw.GLFW.*;

public class WaterFFT 
{
    //noise texture parameters
    int textureWidth = 4096;
    int textureHeight = 4096;

    
    public WaterFFT() {

    }

    public void noiseSetup()
    {
        //we want to use the frame buffer to generate and save a noise texture to be used in the fft shader on startup 

        GLuint frameBuffer = 0;
        glGenFramebuffers(1, frameBuffer);
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

        GLuint texture = 0;
        glGenTextures(1, texture);
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, textureWidth, textureHeight, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);

        // Poor filtering. Needed !
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    }
}