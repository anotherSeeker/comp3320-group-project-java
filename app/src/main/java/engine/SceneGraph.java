package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.GameObject;
import engine.components.Material;
import engine.components.Transform;

public class SceneGraph {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    public Transform cameraTransform = new Transform(new Matrix4f());
    public Vector3f lightDirection = new Vector3f(1, 1, 0);

    public int fov = 60;
    public float aspectRatio;

    Dominion ECS = Dominion.create();

    private AssetManager assetManager = AssetManager.get();

    public SceneGraph() {
        cameraTransform.setPosition(new Vector3f(0, 0, 0));
    }

    public void updateAspectRatio(int width, int height) {
        aspectRatio = (float) width / height;
    }

    public Matrix4f computeViewportMatrix() {
        Matrix4f view = new Matrix4f()
                .rotate(cameraTransform.orientation.conjugate())
                .translate(
                        -cameraTransform.position.x,
                        -cameraTransform.position.y,
                        -cameraTransform.position.z
                );

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);

        return projection.mul(view);
    }

    public void draw() {
        var drawables = ECS.findEntitiesWith(GameObject.class, Transform.class);
        Matrix4f viewportMatrix = computeViewportMatrix();

        for (var iter = drawables.iterator(); iter.hasNext() != false;) {
            var object = iter.next();
            GameObject gameObject = object.comp1();
            Transform transform = object.comp2();

            Material material = gameObject.material;
            material.shader.use();

            material.shader.setMatrix4("Model", transform.model);
            material.shader.setMatrix4("CameraMatrix", viewportMatrix);
            material.shader.setTexture("Albedo", material.albedo, 0);
            material.shader.setVector3("LightDirection", lightDirection);
            material.shader.setVector3("ViewPosition", cameraTransform.position);

            gameObject.mesh.bind();
            gameObject.mesh.draw();
        }
    }

    public Entity createObject(Integer meshID, Material material) {

        Mesh mesh = assetManager.getMesh(meshID);

        GameObject gameObject = new GameObject(mesh, material);
        Transform transform = new Transform(new Matrix4f());

        return ECS.createEntity(gameObject, transform);
    }
}
