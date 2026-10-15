package scripts.water;

import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glCheckFramebufferStatus;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

// import static org.lwjgl.opengl.GL11.*;
// import static org.lwjgl.opengl.GL15.*;
// import static org.lwjgl.opengl.GL20.*;
// import static org.lwjgl.opengl.GL30.*;

public class WaterFFT 
{
    //noise texture parameters

    //static int textureSize = 256;
    public static int textureSize = 16;
    public static int noiseTexId;
    public static double[] noiseArray;
    

    public WaterFFT() {}

    public static boolean noiseSetup()
    {
        //we want to generate a normally distributed 2d noise texture, we don't need to render this out we just need to generate it and write it to screen with nearest mapping
        noiseTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG, textureSize, textureSize, 0, GL_RG, GL_DOUBLE, genGaussianNoise2D());
        
        return true;
    }

    public static double[] genGaussianNoise2D()
    {
        System.out.println("Generating 2D Gaussian Noise Texture for our waves");

        java.util.Random r = new java.util.Random();
        double noiseX;
        double noiseY;

        //ArrayList<Vector2f> noiseList = new ArrayList<>();

        noiseArray = new double[textureSize*textureSize*2];

        for (int i = 0; i < textureSize*textureSize; i++)
        {
            //values between -1 and 1 normally distributed
            noiseX = r.nextGaussian();
            noiseY = r.nextGaussian();

            //rendering this as a texture will have a lot of low values due to our distribution, test it scaled as well later and we probably don't want to treat this data as a texture anyway?
            noiseArray[i*2] = noiseX;
            noiseArray[i*2 + 1] = noiseY;
        }

        System.out.println("Finished Generating 2D Gaussian Noise Pixel Array");

        return noiseArray;
    }


     public static boolean noiseTestDraw()
    {
        //we want to generate a normally distributed 2d noise texture, we don't need to render this out we just need to generate it and write it to screen with nearest mapping

        int fbid = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbid);

        // Always check that our framebuffer is ok
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
        {
            System.err.println("Framebuffer is not complete! aborting noise draw");
            return false;
        }
        // The fullscreen quad's FBO
        int fullscreenQuadVertsId = glGenVertexArrays();
        glBindVertexArray(fullscreenQuadVertsId);

        float fullscreenQuad[] = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            -1.0f,  1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f,  1.0f, 0.0f,
        };

        int quad_vertexbuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quad_vertexbuffer);
        glBufferData(GL_ARRAY_BUFFER, fullscreenQuad, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, quad_vertexbuffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        // Draw the triangle !
        glDrawArrays(GL_TRIANGLES, 0, 6); // Starting from vertex 0; 3 vertices total -> 1 triangle
        glDisableVertexAttribArray(0);

        // Create and compile our GLSL program from the shaders
        //int shaderID = AssetManager.get().LoadShader(Path.of("assets/shaders/water/noiseTest.vert"), Path.of("assets/shaders/water/noiseTest.frag"));

        return true;
    }
}