package engine.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.AssetManager;

public class CubeTexture {
    public int ID;
    public int irradianceID;
    public int prefilterID;
    public int brdfIntID;

    public CubeTexture(FloatBuffer data, int width, int height) {
        glDisable(GL_DEPTH_TEST);
        // creating texture object for the input texture
        int hdrTexture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, hdrTexture);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, data);

        // generating cube map
        int FBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

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
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

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

        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        // generating irradiance map
        irradianceID = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, irradianceID);
        for (int i = 0; i < 6; i++) {
            glTexImage2D(
                    GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGB16F, 64, 64, 0, GL_RGB, GL_FLOAT, (ByteBuffer) null
            );
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        Shader irradianceShader = assets.getShader(assets.irradianceShader);

        irradianceShader.use();
        irradianceShader.setInt("EnvironmentMap", 0);
        irradianceShader.setMatrix4("Projection", captureProjection);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ID);

        glViewport(0, 0, 64, 64);

        for (int i = 0; i < 6; i++) {
            irradianceShader.setMatrix4("View", captureViews[i]);
            glFramebufferTexture2D(
                    GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, irradianceID, 0
            );
            glClear(GL_COLOR_BUFFER_BIT);
            cube.draw();
        }

        // generating prefilter map
        int maxMipLevels = 8;

        prefilterID = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, prefilterID);

        for (int mip = 0; mip < maxMipLevels; mip++) {
            int mipSize = 128 >> mip;
            for (int i = 0; i < 6; i++) {
                glTexImage2D(
                        GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, mip, GL_RGB16F, mipSize, mipSize, 0, GL_RGB, GL_FLOAT,
                        (ByteBuffer) null
                );
            }
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

        Shader prefilterShader = assets.getShader(assets.prefilterShader);

        prefilterShader.use();
        prefilterShader.setInt("EnvironmentMap", 0);
        prefilterShader.setMatrix4("Projection", captureProjection);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ID);

        for (int mip = 0; mip < maxMipLevels; mip++) {
            int mipWidth = 128 >> mip;
            int mipHeight = 128 >> mip;

            glViewport(0, 0, mipWidth, mipHeight);

            float roughness = (float) mip / (float) (maxMipLevels - 1);

            prefilterShader.setFloat("Roughness", roughness);
            for (int i = 0; i < 6; i++) {
                prefilterShader.setMatrix4("View", captureViews[i]);
                glFramebufferTexture2D(
                        GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, prefilterID, mip
                );
                glClear(GL_COLOR_BUFFER_BIT);
                cube.draw();
            }
        }

        // brdf integration map creation
        brdfIntID = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, brdfIntID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16F, 512, 512, 0, GL_RG, GL_FLOAT, (FloatBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        Shader brdfIntShader = assets.getShader(assets.brdfIntShader);
        Mesh quad = Mesh.Quad();

        glViewport(0, 0, 512, 512);

        brdfIntShader.use();
        glFramebufferTexture2D(
                GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, brdfIntID, 0
        );
        glClear(GL_COLOR_BUFFER_BIT);
        quad.draw();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDeleteFramebuffers(FBO);
        glDeleteTextures(hdrTexture);

        glEnable(GL_DEPTH_TEST);
    }

    public void use(int slot, int irradianceSlot, int prefilterSlot, int brdfIntSlot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, ID);

        glActiveTexture(GL_TEXTURE0 + irradianceSlot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, irradianceID);

        glActiveTexture(GL_TEXTURE0 + prefilterSlot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, prefilterID);

        glActiveTexture(GL_TEXTURE0 + brdfIntSlot);
        glBindTexture(GL_TEXTURE_2D, brdfIntID);
    }

    public void unbind(int slot, int irradianceSlot, int prefilterSlot, int brdfIntSlot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        glActiveTexture(GL_TEXTURE0 + irradianceSlot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        glActiveTexture(GL_TEXTURE0 + prefilterSlot);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        glActiveTexture(GL_TEXTURE0 + brdfIntSlot);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void delete() {
        glDeleteTextures(ID);
        glDeleteTextures(irradianceID);
        glDeleteTextures(prefilterID);
        glDeleteTextures(brdfIntID);
    }
}
