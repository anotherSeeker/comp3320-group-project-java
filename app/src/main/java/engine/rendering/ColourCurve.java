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

    private Curve red;
    private Curve green;
    private Curve blue;

    public record Curve(
            float p0,
            Vector2f p1,
            Vector2f p2,
            float p3) {

        public Curve(float p0, Vector2f p1, Vector2f p2, float p3) {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        public static Curve Default() {
            return new Curve(0.0f, new Vector2f(0.25f, 0.25f), new Vector2f(0.75f, 0.75f), 1.0f);
        }
    }

    int ID;

    private float bezier(Curve curve, float x) {
        float low = 0.0f;
        float high = 1.0f;

        // binary search to derive t from p1 & p2 x values.
        for (int i = 0; i < 20; i++) {
            float t = (high + low) * 0.5f;
            float u = 1.0f - t;

            float bx = 3 * u * u * t * curve.p1.x()
                    + 3 * u * t * t * curve.p2.x()
                    + t * t * t;

            if (bx < x) {
                low = t;
            } else {
                high = t;
            }
        }

        float t = (high + low) * 0.5f;
        float u = 1.0f - t;

        float a = u * u * u;
        float b = 3 * u * u * t;
        float c = 3 * u * t * t;
        float d = t * t * t;

        return a * curve.p0 + b * curve.p1.y() + c * curve.p2.y() + d * curve.p3;
    }

    ColourCurve(Curve red, Curve green, Curve blue) {

        this.red = red;
        this.green = green;
        this.blue = blue;

        ID = glGenTextures();
        update();

        glBindTexture(GL_TEXTURE_1D, ID);
        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterIi(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindTexture(GL_TEXTURE_1D, 0);
    }

    public void update() {
        FloatBuffer data = BufferUtils.createFloatBuffer(RESOLUTION * 3);

        for (int i = 0; i < RESOLUTION; i++) {
            float t = (float) i / (RESOLUTION - 1);

            data.put(bezier(red, t));
            data.put(bezier(green, t));
            data.put(bezier(blue, t));
        }

        data.flip();
        glBindTexture(GL_TEXTURE_1D, ID);

        glTexImage1D(GL_TEXTURE_1D, 0, GL_RGB, RESOLUTION, 0, GL_RGB, GL_FLOAT, data);

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
