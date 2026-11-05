package engine.rendering;

import engine.AssetManager;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

/** Quarter-resolution separable HDR glow, supported by OpenGL 4.1 without compute shaders. */
public final class CoralGlow {
    private final int[] textures = {glGenTextures(), glGenTextures()};
    private final int[] targets = {glGenFramebuffers(), glGenFramebuffers()};
    private final Shader shader;
    private int width, height;

    public CoralGlow() {
        AssetManager assets = AssetManager.get();
        shader = assets.getShader(assets.LoadShader(Path.of("assets/shaders/internal/framebuffer.vert"),
                Path.of("assets/shaders/coral/glow.frag")));
    }

    private void resize(int fullWidth, int fullHeight) {
        int w = Math.max(1,fullWidth/4), h = Math.max(1,fullHeight/4);
        if (width == w && height == h) return;
        width=w; height=h;
        for (int i=0; i<2; i++) {
            glBindTexture(GL_TEXTURE_2D,textures[i]);
            glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA16F,w,h,0,GL_RGBA,GL_FLOAT,(ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
            glBindFramebuffer(GL_FRAMEBUFFER,targets[i]);
            glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,textures[i],0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Coral glow framebuffer incomplete");
        }
    }

    public int render(int source, int fullWidth, int fullHeight, Mesh quad) {
        resize(fullWidth,fullHeight);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glViewport(0,0,width,height);
        shader.use();
        shader.setInt("Source",0);
        for (int pass=0; pass<5; pass++) {
            int target=pass%2;
            glBindFramebuffer(GL_FRAMEBUFFER,targets[target]);
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D,pass==0?source:textures[1-target]);
            shader.setInt("Pass",pass==0?0:(pass%2==1?1:2));
            shader.setFloat("TexelX",1.0f/(pass==0?fullWidth:width));
            shader.setFloat("TexelY",1.0f/(pass==0?fullHeight:height));
            quad.draw();
        }
        glBindFramebuffer(GL_FRAMEBUFFER,0);
        glViewport(0,0,fullWidth,fullHeight);
        return textures[0];
    }

    public void close() {
        for (int texture:textures) glDeleteTextures(texture);
        for (int target:targets) glDeleteFramebuffers(target);
    }
}
