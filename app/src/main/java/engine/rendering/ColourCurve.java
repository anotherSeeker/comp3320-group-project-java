package engine.rendering;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.joml.Vector2f;
import org.lwjgl.BufferUtils;

public class ColourCurve {

    private final int RESOLUTION = 256;

    public record Curve(
            float p0,
            Vector2f p1,
            Vector2f p2,
            float p3) {
    }

    int ID;

    private float bezier(Curve curve, float t) {

        Vector2f p0 = new Vector2f(0.0f, curve.p0);
        Vector2f p1 = curve.p1;
        Vector2f p2 = curve.p2;
        Vector2f p3 = new Vector2f(1.0f, curve.p3);

        float a = (float) Math.pow((1.0 - t), 3.0);
        float b = (float) (3.0 * Math.pow((1.0 - t), 2.0) * t);
        float c = (float) (3.0 * Math.pow((1.0 - t), 2.0) * t * t);
        float d = t * t * t;

        return (p0.mul(a).add(p1.mul(b)).add(p2.mul(c)).add(p3.mul(d))).y();
    }

    ColourCurve(Curve red, Curve green, Curve blue) {

        FloatBuffer data = BufferUtils.createFloatBuffer(RESOLUTION * 3);

        for (int i = 0; i < RESOLUTION; i++) {
            data.put(bezier(red, i));
            data.put(bezier(green, i));
            data.put(bezier(blue, i));
        }

        data.flip();

        ID = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, ID);

        glTexImage1D(GL_TEXTURE_1D, 0, GL_RGB16F, RESOLUTION, 0, GL_RGB16F, GL_FLOAT, data);

        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_1D, 0);
    }

    public void use(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_1D, ID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_1D, 0);
    }

    public void delete() {
        glDeleteTextures(ID);
    }

}
