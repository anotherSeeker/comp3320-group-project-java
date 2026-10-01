package scripts;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.file.Path;

import org.joml.Vector2f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Transform;

public class DemoScene extends MonoScript {
    int cameraSpeed = 10;
    float mouseSens = 0.005f;

    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag")
    );

    Integer meshID = Assets.LoadMesh(Path.of("assets/meshes/uvsphere.obj"));

    float yaw = 0.0f;
    float pitch = 0.0f;

    Entity A;
    Entity B;

    @Override
    protected void Start() {
        A = Scene.createObject(meshID, shaderID);
        B = Scene.createObject(meshID, shaderID);

        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
        B.get(Transform.class).setPosition(new Vector3f(-5f, 0, 0));

        Input.LockMouse();
    }

    @Override
    protected void Update(double deltaTime) {

        Vector3f moveDirection = new Vector3f(0, 0, 0);

        if (Input.IsKeyDown(GLFW_KEY_W) == true) {
            moveDirection.add(new Vector3f(0.0f, 0.0f, -cameraSpeed));
        }

        if (Input.IsKeyDown(GLFW_KEY_S) == true) {
            moveDirection.add(new Vector3f(0.0f, 0.0f, cameraSpeed));
        }

        if (Input.IsKeyDown(GLFW_KEY_A) == true) {
            moveDirection.add(new Vector3f(-cameraSpeed, 0.0f, 0.0f));
        }

        if (Input.IsKeyDown(GLFW_KEY_D) == true) {
            moveDirection.add(new Vector3f(cameraSpeed, 0.0f, 0.0f));
        }

        if (Input.IsKeyDown(GLFW_KEY_Q) == true) {
            moveDirection.add(new Vector3f(0.0f, -cameraSpeed, 0.0f));
        }

        if (Input.IsKeyDown(GLFW_KEY_E) == true) {
            moveDirection.add(new Vector3f(0.0f, cameraSpeed, 0.0f));
        }

        Vector3f worldDirection = new Vector3f(0, 0, 0);

        moveDirection.mulDirection(Scene.cameraTransform.model, worldDirection);

        worldDirection = worldDirection.mul((float) deltaTime);

        Vector2f mouseDelta = Input.GetMouseDelta();

        yaw -= mouseDelta.x * mouseSens;
        pitch += mouseDelta.y * mouseSens;

        pitch = Math.clamp(pitch, (float) Math.toRadians(-89), (float) Math.toRadians(89));

        Scene.cameraTransform.setOrientationEuler(new Vector3f(pitch, yaw, 0));
        Scene.cameraTransform.setPosition(Scene.cameraTransform.position.add(worldDirection));
    }
}
