package scripts;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.*;

/**
 * Free 360 degree camera for exploring the sunken castle (Tanz).
 *
 * Use it instead of scripts.Camera in Game.java:
 * app.registerScript(new scripts.FreeCamera());
 *
 * Controls:
 * - arrow keys            turn left/right (all the way round) and look up/down
 * - right mouse + drag    look around with the mouse
 * - W / S                 forward / back (in the direction you are looking)
 * - A / D                 left / right
 * - E / Q                 up / down
 * - hold Left Shift       move slowly
 */
public class FreeCamera extends MonoScript {
    final float MOVE_SPEED = 8.0f;          // metres per second
    final float SLOW_SPEED = 2.0f;          // with Left Shift
    final float TURN_SPEED = 1.8f;          // radians per second (arrow keys)
    final float MOUSE_SENSITIVITY = 0.005f;
    final float MAX_PITCH = (float) Math.toRadians(89);

    float yaw = (float) Math.PI;   // starts facing +Z (the castle)
    float pitch = 0.0f;

    @Override
    protected void Start() {
        Scene.cameraTransform.setOrientationEuler(new Vector3f(pitch, yaw, 0));
    }

    @Override
    protected void Update(double deltaTime) {
        float dt = (float) deltaTime;

        // turning with the arrow keys (no limit left/right, so a full 360)
        if (Input.IsKeyDown(GLFW_KEY_LEFT)) yaw += TURN_SPEED * dt;
        if (Input.IsKeyDown(GLFW_KEY_RIGHT)) yaw -= TURN_SPEED * dt;
        if (Input.IsKeyDown(GLFW_KEY_UP)) pitch += TURN_SPEED * dt;
        if (Input.IsKeyDown(GLFW_KEY_DOWN)) pitch -= TURN_SPEED * dt;

        // mouse look while the right button is held; the movement is read every
        // frame so it does not build up and make the view jump when you click
        Vector2f mouse = Input.GetMouseDelta();
        if (Input.IsMouse2Down()) {
            Input.LockMouse();
            yaw -= mouse.x * MOUSE_SENSITIVITY;
            pitch += mouse.y * MOUSE_SENSITIVITY;
        } else {
            Input.UnlockMouse();
        }

        // keep yaw between 0 and 2*PI, and don't let the view flip over the top
        yaw = (float) (yaw % (2 * Math.PI));
        pitch = Math.clamp(pitch, -MAX_PITCH, MAX_PITCH);
        Scene.cameraTransform.setOrientationEuler(new Vector3f(pitch, yaw, 0));

        // moving, relative to where the camera is looking
        float speed = Input.IsKeyDown(GLFW_KEY_LEFT_SHIFT) ? SLOW_SPEED : MOVE_SPEED;
        Vector3f move = new Vector3f(0, 0, 0);
        if (Input.IsKeyDown(GLFW_KEY_W)) move.add(0, 0, -speed);
        if (Input.IsKeyDown(GLFW_KEY_S)) move.add(0, 0, speed);
        if (Input.IsKeyDown(GLFW_KEY_A)) move.add(-speed, 0, 0);
        if (Input.IsKeyDown(GLFW_KEY_D)) move.add(speed, 0, 0);
        if (Input.IsKeyDown(GLFW_KEY_E)) move.add(0, speed, 0);
        if (Input.IsKeyDown(GLFW_KEY_Q)) move.add(0, -speed, 0);

        Vector3f worldMove = new Vector3f();
        move.mulDirection(Scene.cameraTransform.model, worldMove);
        worldMove.mul(dt);

        Scene.cameraTransform.setPosition(Scene.cameraTransform.position.add(worldMove));
    }
}
