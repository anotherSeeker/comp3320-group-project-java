package engine.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.AssetManager;

public class ShadowMap {
    private static AssetManager asset = AssetManager.get();
    public static Shader shadowPass = asset.getShader(asset.shadowPassShader);

    public int FBO;
    public int shadowTexture;

    public int width;
    public int height;

    public Matrix4f lightMatrix = new Matrix4f();

    public ShadowMap(int width, int height) {
        this.width = width;
        this.height = height;

        this.shadowTexture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, this.shadowTexture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, this.width, this.height, 0, GL_DEPTH_COMPONENT,
                GL_UNSIGNED_INT, (ByteBuffer) null
        );

        this.FBO = glGenFramebuffers();

        glBindFramebuffer(GL_FRAMEBUFFER, this.FBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, this.shadowTexture, 0);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        var FBOStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if (FBOStatus != GL_FRAMEBUFFER_COMPLETE)
            System.out.println(FBOStatus);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void onResize(int width, int height) {
        this.width = width;
        this.height = height;

        glBindTexture(GL_TEXTURE_2D, this.shadowTexture);
        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, this.width, this.height, 0, GL_DEPTH_COMPONENT,
                GL_UNSIGNED_INT,
                (ByteBuffer) null
        );
    }

    private Matrix4f computeLightMatrix(Vector3f lightDirection) {
        return null;
    }

    public void genShadowsFromDirection(Vector3f lightDirection) {
        glBindTexture(GL_TEXTURE_2D, this.shadowTexture);
        glBindFramebuffer(GL_FRAMEBUFFER, this.FBO);
        glViewport(0, 0, this.width, this.height);
        glClear(GL_DEPTH_BUFFER_BIT);

        this.lightMatrix = computeLightMatrix(lightDirection);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glDepthFunc(GL_LESS);

        shadowPass.use();
        shadowPass.setMatrix4("LightMatrix", lightMatrix);
    }

    public void end() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

}
