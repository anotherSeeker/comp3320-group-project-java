package engine;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.GameObject;
import engine.components.Transform;

public class Scene {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    public Transform cameraTransform = new Transform(new Matrix4f());

    public int fov = 60;
    public float aspectRatio;

    Dominion ECS = Dominion.create();

    private AssetManager assetManager = AssetManager.get();
    private long window;

    public Scene(long window, Vector3f initPosition) {
        this.window = window;
        cameraTransform.setPosition(initPosition);
    }

    public void updateAspectRatio(int width, int height) {
        aspectRatio = (float) width / height;
    }

    public Matrix4f computeViewportMatrix() {
        Matrix4f view = cameraTransform.model;
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

    public Entity createObject(Integer meshID, Integer shaderID) {

        Mesh mesh = assetManager.getMesh(meshID);
        Shader shader = assetManager.getShader(shaderID);

        GameObject gameObject = new GameObject(mesh, shader);
        Transform transform = new Transform(new Matrix4f());

        return ECS.createEntity(gameObject, transform);
    }

    public boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }
}
