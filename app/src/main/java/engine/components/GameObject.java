package engine.components;

import engine.Mesh;
import engine.Shader;

public class GameObject {
    public Mesh mesh;
    public Shader shader;

    public GameObject(Mesh mesh, Shader shader) {
        this.mesh = mesh;
        this.shader = shader;
    }
}
