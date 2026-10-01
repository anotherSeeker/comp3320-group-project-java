package engine;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {
    public float mouseDeltaX = 0.0f;
    public float mouseDeltaY = 0.0f;

    private long window;

    private double previousMouseX = 0.0f;
    private double previousMouseY = 0.0f;
    private boolean firstMouse = false;

    public InputManager(long window) {
        this.window = window;

        glfwSetCursorPosCallback(window, (_, x, y) -> {
            if (firstMouse) {
                previousMouseX = x;
                previousMouseY = y;
            }

            mouseDeltaX = (float) (x - previousMouseX);
            mouseDeltaY = (float) (previousMouseY - y);
        });
    }

    public boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

}
