package engine;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class ShaderPipeline {
    private int FBO;
    private int RBO;
    private int colourBuffer;
    private Mesh screenQuad;
    private Shader framebufferShader;

    ShaderPipeline(Shader framebufferShader, int width, int height) {
        this.FBO = glGenFramebuffers();
        this.colourBuffer = glGenTextures();
        this.screenQuad = Mesh.Quad();

        this.framebufferShader = framebufferShader;

        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glBindTexture(GL_TEXTURE_2D, colourBuffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colourBuffer, 0);

        this.RBO = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, RBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, RBO);

        var FBOStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if (FBOStatus != GL_FRAMEBUFFER_COMPLETE)
            System.out.println(FBOStatus);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void onResize(int width, int height) {
        glBindTexture(GL_TEXTURE_2D, colourBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindRenderbuffer(GL_RENDERBUFFER, RBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void render() {
        unbind();
        glDisable(GL_DEPTH_TEST);
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        screenQuad.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colourBuffer);

        framebufferShader.use();
        framebufferShader.setInt("screenTexture", 0);

        screenQuad.draw();
    }
}
