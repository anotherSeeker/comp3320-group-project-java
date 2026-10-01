package engine;

import org.joml.Vector3f;
import org.joml.Vector2f;

public class Vertex {
    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f normal = new Vector3f(0, 0, 0);
    public Vector2f uv = new Vector2f(0, 0);

    Vertex(Vector3f position, Vector3f normal, Vector2f uv) {
        this.position = position;
        this.normal = normal;
        this.uv = uv;
    }
}