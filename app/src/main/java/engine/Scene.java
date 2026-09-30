package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.GameObject;
import engine.components.Transform;

public class Scene {
    public Vector3f cameraPosition = new Vector3f(0, 0, 0);
    public Vector3f cameraLookAt = new Vector3f(0, 0, -1);
    public Vector3f cameraUp = new Vector3f(0, 1, 0);

    public int fov = 60;

    public float aspectRatio;

    Dominion ECS = Dominion.create();

    public Scene(Vector3f initPosition) {
        cameraPosition = initPosition;
    }

    public void updateAspectRatio(int width, int height) {
        aspectRatio = width / height;
    }

    public Matrix4f computeViewportMatrix() {
        Matrix4f view = new Matrix4f().lookAt(cameraPosition, cameraLookAt, cameraUp);
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

            gameObject.shader.setMatrix4("Model", transform.model);
            gameObject.shader.setMatrix4("CameraMatrix", viewportMatrix);

            gameObject.shader.use();
            gameObject.mesh.bind();
            gameObject.mesh.draw();
        }
    }

    public Entity createObject(Mesh mesh, Shader shader, Matrix4f model) {
        GameObject gameObject = new GameObject(mesh, shader);
        Transform transform = new Transform(model);

        return ECS.createEntity(gameObject, transform);
    }

}
