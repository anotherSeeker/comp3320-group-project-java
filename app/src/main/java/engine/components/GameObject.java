package engine.components;

import engine.rendering.Mesh;
import scripts.Animation.Model;

public class GameObject {
    public Mesh mesh;
    public Material material;
    public Model model;

    public GameObject(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

        public GameObject(Mesh mesh, Material material, Model model) {
        this.mesh = mesh;
        this.material = material;
        this.model = model;
    }

}
