package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Scene {
    public Vector3f cameraPosition = new Vector3f(0, 0, 0);
    public Vector3f cameraLookAt = new Vector3f(0, 0, -1);
    public Vector3f cameraUp = new Vector3f(0, 1, 0);

    public int fov = 60;

    public float aspectRatio;

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
}
