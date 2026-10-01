package scripts;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Transform;

public class DemoScene extends MonoScript {
    int cameraSpeed = 10;

    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag"));

    Integer meshID = Assets.LoadMesh(Path.of("assets/meshes/uvsphere.obj"));

    Entity A;

    @Override
    protected void Start() {
        A = Scene.createObject(meshID, shaderID);

        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    @Override
    protected void Update(double deltaTime) {

        if (Input.IsKeyDown(GLFW_KEY_W) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(0.0f, 0.0f, cameraSpeed * (float) deltaTime)));
        }

        if (Input.IsKeyDown(GLFW_KEY_S) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(0.0f, 0.0f, cameraSpeed * (float) -deltaTime)));
        }

        if (Input.IsKeyDown(GLFW_KEY_A) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(cameraSpeed * (float) deltaTime, 0.0f, 0.0f)));
        }

        if (Input.IsKeyDown(GLFW_KEY_D) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(cameraSpeed * (float) -deltaTime, 0.0f, 0.0f)));
        }

        if (Input.IsKeyDown(GLFW_KEY_Q) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(0.0f, cameraSpeed * (float) deltaTime, 0.0f)));
        }

        if (Input.IsKeyDown(GLFW_KEY_E) == true) {
            Scene.cameraTransform.setPosition(
                    Scene.cameraTransform.position.add(new Vector3f(0.0f, cameraSpeed * (float) -deltaTime, 0.0f)));
        }

    }
}
