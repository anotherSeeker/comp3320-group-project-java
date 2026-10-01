package engine.components;

import engine.Mesh;

public class GameObject {
    public Mesh mesh;
    public Material material;

    public GameObject(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }
}
