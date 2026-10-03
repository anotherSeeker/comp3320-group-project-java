package engine;

import org.joml.Vector3f;
import org.joml.Vector2f;

public class Vertex {
    public Vector3f position = new Vector3f(0, 0, 0);
    public Vector3f normal = new Vector3f(0, 0, 0);
    public Vector2f uv = new Vector2f(0, 0);

    public Vector3f tangent = new Vector3f(0, 0, 0);
    public Vector3f bitangent = new Vector3f(0, 0, 0);

    Vertex(Vector3f position, Vector3f normal, Vector2f uv, Vector3f tangent, Vector3f bitangent) {
        this.position = position;
        this.normal = normal;
        this.uv = uv;
        this.tangent = tangent;
        this.bitangent = bitangent;
    }

    Vertex(Vector3f position) {
        this.position = position;
    }

    Vertex(Vector3f position, Vector2f uv) {
        this.position = position;
        this.uv = uv;
    }
}