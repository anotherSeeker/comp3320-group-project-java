package engine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CubeTexture {
    public int ID;

    public CubeTexture(FloatBuffer data, int width, int height) {
        int hdrTexture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, hdrTexture);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, data);
        glGenerateMipmap(GL_TEXTURE_2D);

        int FBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);

        ID = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, ID);
        for (int i = 0; i < 6; i++) {
            glTexImage2D(
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, 512, 512, 0, GL_RGB, GL_FLOAT, (ByteBuffer) null
            );
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        Matrix4f captureProjection = new Matrix4f().perspective((float) Math.toRadians(90.0), 1.0f, 0.1f, 10.0f);
        Matrix4f[] captureViews = new Matrix4f[6];
        captureViews[0] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(0, -1, 0));
        captureViews[1] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, -1, 0));
        captureViews[2] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1));
        captureViews[3] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0, -1, 0), new Vector3f(0, 0, -1));
        captureViews[4] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, -1, 0));
        captureViews[5] = new Matrix4f().lookAt(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, -1, 0));

        AssetManager assets = AssetManager.get();
        Shader cubemapShader = assets.getShader(assets.cubemapShader);
        Mesh cube = Mesh.Cube();

        cubemapShader.use();
        cubemapShader.setInt("EquirectangularMap", 0);
        cubemapShader.setMatrix4("Projection", captureProjection);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hdrTexture);
        glViewport(0, 0, 512, 512);
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);

        for (int i = 0; i < 6; i++) {
            cubemapShader.setMatrix4("View", captureViews[i]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, ID, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            cube.draw();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public void use(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    public void delete() {
        glDeleteTextures(ID);
    }
}
