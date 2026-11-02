package engine.rendering;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL42.*;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL15.GL_READ_ONLY;
import static org.lwjgl.opengl.GL15.GL_WRITE_ONLY;

import java.nio.ByteBuffer;
import engine.AssetManager;
import engine.rendering.ColourCurve.Curve;

public class ShaderPipeline {
    private AssetManager assets = AssetManager.get();

    private int FBO;
    private int colourBuffer;
    private int pingBuffer;
    private int pongBuffer;
    private int depthBuffer;
    private Mesh screenQuad;
    private Shader framebufferShader;

    private Shader bloomA = assets.getShader(assets.bloomPostProcessA);
    private Shader bloomB = assets.getShader(assets.bloomPostProcessB);
    private Shader blur = assets.getShader(assets.blurPostProcess);
    private Shader vignette = assets.getShader(assets.vignettePostProcess);
    private Shader colourGrade = assets.getShader(assets.colourGradeProcess);
    private Shader fog = assets.getShader(assets.fogProcess);

    private int featureMask = 0;
    public static int BLOOM = 1 << 0;
    public static int VIGNETTE = 1 << 1;
    public static int COLOUR_GRADE = 1 << 2;
    public static int FOG = 1 << 3;

    public float BloomKnee = 0.5f;
    public float BloomThreshold = 1.0f;

    public float VignetteRadius = 0.75f;
    public float VignetteSoftness = 0.2f;

    public float MaxBrightness = 1.0f;
    public float Contrast = 1.0f;
    public float LinSectStart = 0.22f;
    public float LinSectLength = 0.4f;
    public float Black = 1.33f;
    public float Pedestal = 0.0f;

    public float Whitepoint = 11.2f;
    public float Exposure = -1.5f;

    public ColourCurve ColourGradeCurve = new ColourCurve(Curve.Default(), Curve.Default(), Curve.Default());

    private int screenWidth;
    private int screenHeight;

    private void initTexture(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA16F, screenWidth, screenHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null
        );
    }

    private void initDepthTexture() {
        glBindTexture(GL_TEXTURE_2D, depthBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, screenWidth, screenHeight, 0, GL_DEPTH_COMPONENT,
                GL_UNSIGNED_INT, (ByteBuffer) null
        );
    }

    public ShaderPipeline(Shader framebufferShader, int width, int height) {
        this.FBO = glGenFramebuffers();
        this.colourBuffer = glGenTextures();
        this.pingBuffer = glGenTextures();
        this.pongBuffer = glGenTextures();
        this.depthBuffer = glGenTextures();

        this.screenQuad = Mesh.Quad();

        this.framebufferShader = framebufferShader;

        this.screenWidth = width;
        this.screenHeight = height;

        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
        initTexture(colourBuffer);
        initTexture(pingBuffer);
        initTexture(pongBuffer);
        initDepthTexture();

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colourBuffer, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthBuffer, 0);

        var FBOStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        if (FBOStatus != GL_FRAMEBUFFER_COMPLETE)
            System.out.println(FBOStatus);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void onResize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        glBindTexture(GL_TEXTURE_2D, colourBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, pingBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, pongBuffer);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (ByteBuffer) null);

        glBindTexture(GL_TEXTURE_2D, depthBuffer);
        glTexImage2D(
                GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, screenWidth, screenHeight, 0, GL_DEPTH_COMPONENT,
                GL_UNSIGNED_INT,
                (ByteBuffer) null
        );
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, FBO);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void Enable(int feature) {
        featureMask |= feature;
    }

    public void Disable(int feature) {
        featureMask = featureMask & ~feature;
    }

    int outputTexture;

    private void runShader(Shader shader, int inputTexture, int outputTexture) {
        glBindImageTexture(0, inputTexture, 0, false, 0, GL_READ_ONLY, GL_RGBA16F);
        glBindImageTexture(1, outputTexture, 0, false, 0, GL_WRITE_ONLY, GL_RGBA16F);
        glBindImageTexture(2, colourBuffer, 0, false, 0, GL_READ_ONLY, GL_RGBA16F);

        shader.use();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, depthBuffer);
        shader.setInt("DepthBuffer", 0);
        shader.dispatchCompute((this.screenWidth + 7) / 8, (this.screenHeight + 7) / 8, 1);
    }

    public void process() {
        if (featureMask == 0) {
            outputTexture = colourBuffer;
            return;
        }

        int currentTexture = colourBuffer;

        if ((featureMask & BLOOM) != 0) {
            bloomA.use();
            bloomA.setFloat("Knee", BloomKnee);
            bloomA.setFloat("Threshold", BloomThreshold);

            runShader(bloomA, currentTexture, pingBuffer);
            runShader(blur, pingBuffer, pongBuffer);
            runShader(blur, pongBuffer, pingBuffer);
            runShader(bloomB, pingBuffer, pongBuffer);

            currentTexture = pongBuffer;
        }

        if ((featureMask & COLOUR_GRADE) != 0) {
            int target = currentTexture == pingBuffer ? pongBuffer : pingBuffer;

            colourGrade.use();

            ColourGradeCurve.use(1);
            colourGrade.setInt("ColourRamp", 1);

            runShader(colourGrade, currentTexture, target);

            currentTexture = target;
        }

        if ((featureMask & VIGNETTE) != 0) {
            int target = currentTexture == pingBuffer ? pongBuffer : pingBuffer;

            vignette.use();
            vignette.setFloat("Radius", VignetteRadius);
            vignette.setFloat("Softness", VignetteSoftness);
            runShader(vignette, currentTexture, target);

            currentTexture = target;
        }

        if ((featureMask & FOG) != 0) {
            int target = currentTexture == pingBuffer ? pongBuffer : pingBuffer;

            fog.use();
            runShader(fog, currentTexture, target);

            currentTexture = target;
        }

        outputTexture = currentTexture;
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

        framebufferShader.setFloat("MaxBrightness", MaxBrightness);
        framebufferShader.setFloat("Contrast", Contrast);
        framebufferShader.setFloat("LinSectStart", LinSectStart);
        framebufferShader.setFloat("LinSectLength", LinSectLength);
        framebufferShader.setFloat("Black", Black);
        framebufferShader.setFloat("Pedestal", Pedestal);

        framebufferShader.setFloat("Whitepoint", Whitepoint);
        framebufferShader.setFloat("Exposure", Exposure);
        screenQuad.draw();
    }
}
