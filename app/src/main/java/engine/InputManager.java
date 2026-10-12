package engine;

import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector2f;

public class InputManager {
    private float mouseDeltaX = 0.0f;
    private float mouseDeltaY = 0.0f;

    private long window;

    private double previousMouseX = 0.0f;
    private double previousMouseY = 0.0f;
    private boolean firstMouse = true;

    public InputManager(long window) {
        this.window = window;

        glfwSetCursorPosCallback(window, (_, x, y) -> {
            if (firstMouse) {
                previousMouseX = x;
                previousMouseY = y;
                firstMouse = false;
            }

            mouseDeltaX = (float) (x - previousMouseX);
            mouseDeltaY = (float) (previousMouseY - y);

            previousMouseX = x;
            previousMouseY = y;
        });
    }

    public boolean IsKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    public boolean IsMouse1Down() {
        return glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    public boolean IsMouse2Down() {
        return glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;
    }

    public Vector2f GetMouseDelta() {
        Vector2f delta = new Vector2f(mouseDeltaX, mouseDeltaY);

        mouseDeltaX = 0.0f;
        mouseDeltaY = 0.0f;

        return delta;
    }

    public void LockMouse() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    public void UnlockMouse() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }
}
