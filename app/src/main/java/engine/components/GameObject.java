package engine.components;

import engine.Mesh;
import engine.MeshAnim;

public class GameObject {
    public Mesh mesh;
    public MeshAnim meshAnim;
    public Material material;

    public GameObject(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

        public GameObject(MeshAnim meshAnim, Material material) {
        this.meshAnim = meshAnim;
        this.mesh = meshAnim.getMesh();
        this.material = material;
    }
}
