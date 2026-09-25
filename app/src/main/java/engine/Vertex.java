package engine;

import org.joml.Vector3f;

public class Vertex {
    public Vector3f position = new Vector3f(0, 0, 0);

    Vertex(Vector3f position) {
        this.position = position;
    }
}
