package engine;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_READ_WRITE;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ShaderPipeline {
    private int FBO;
    private int RBO;
    private int colourBuffer;
    private int pingBuffer;
    private int pongBuffer;
    private Mesh screenQuad;
    private Shader framebufferShader;

    private ArrayList<Shader> shaders = new ArrayList<>();

    private int screenWidth;
    private int screenHeight;

    ShaderPipeline(Shader framebufferShader, int width, int height) {
        this.FBO = glGenFramebuffers();
        this.colourBuffer = glGenTextures();
        this.pingBuffer = glGenTextures();
        this.pongBuffer = glGenTextures();

        this.screenQuad = Mesh.Quad();

        this.framebufferShader = framebufferShader;

        this.screenWidth = width;
        this.screenHeight = height;

        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glBindTexture(GL_TEXTURE_2D, colourBuffer);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colourBuffer, 0);

        glBindTexture(GL_TEXTURE_2D, pingBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, pongBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

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
        this.screenWidth = width;
        this.screenHeight = height;

        glBindTexture(GL_TEXTURE_2D, colourBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, pingBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, pongBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glBindRenderbuffer(GL_RENDERBUFFER, RBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
    }

    public void attach(Shader shader) {
        shaders.add(shader);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    int outputTexture;

    private void runShader(Shader shader, int inputTexture, int outputTexture) {
        glBindImageTexture(0, inputTexture, 0, false, 0, GL_READ_ONLY, GL_RGBA8);
        glBindImageTexture(1, outputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA8);
        glBindImageTexture(2, colourBuffer, 0, false, 0, GL_READ_ONLY, GL_RGBA8);

        shader.use();
        shader.dispatchCompute((this.screenWidth + 7) / 8, (this.screenHeight + 7) / 8, 1);
    }

    public void process() {
        if (shaders.isEmpty()) {
            outputTexture = colourBuffer;
            return;
        }

        int inputTexture = colourBuffer;
        int writeTexture = pongBuffer;

        for (Shader shader : shaders) {

            runShader(shader, inputTexture, writeTexture);

            inputTexture = writeTexture;

            if (writeTexture == pingBuffer) {
                writeTexture = pongBuffer;
            } else {
                writeTexture = pingBuffer;
            }
        }

        outputTexture = inputTexture;
    }

    public void render() {
        unbind();
        glDisable(GL_DEPTH_TEST);
        glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

        screenQuad.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, outputTexture);

        framebufferShader.use();
        framebufferShader.setInt("screenTexture", 0);

        screenQuad.draw();
    }
}
