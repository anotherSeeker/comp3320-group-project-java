package scripts;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.*;

public class Camera extends MonoScript {
    final int MAX_CAMERA_SPEED = 10;
    final int MIN_CAMERA_SPEED = 2;

    int cameraSpeed = MAX_CAMERA_SPEED;
    float mouseSens = 0.005f;

    float yaw = (float) Math.PI;
    float pitch = 0.0f;

    @Override
    protected void Start() {
        Scene.cameraTransform.setPosition(new Vector3f(0, 0, -5));
        Scene.cameraTransform.lookAt(new Vector3f(0, 0, 0));

        Input.LockMouse();
    }

    private void rotateCamera() {
        Vector2f mouseDelta = Input.GetMouseDelta();

        yaw -= mouseDelta.x * mouseSens;
        pitch += mouseDelta.y * mouseSens;

        pitch = Math.clamp(pitch, (float) Math.toRadians(-89), (float) Math.toRadians(89));

        Scene.cameraTransform.setOrientationEuler(new Vector3f(pitch, yaw, 0));
    }

    @Override
    protected void Update(double deltaTime) {
        int cameraSpeed = Input.IsKeyDown(GLFW_KEY_LEFT_SHIFT) ? MIN_CAMERA_SPEED : MAX_CAMERA_SPEED;
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

        if (Input.IsMouse2Down()) {
            Input.LockMouse();
            rotateCamera();
        } else {
            Input.UnlockMouse();
        }

        Scene.cameraTransform.setPosition(Scene.cameraTransform.position.add(worldDirection));
    }
}
