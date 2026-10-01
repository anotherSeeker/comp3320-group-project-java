package scripts;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Transform;

public class DemoScene extends MonoScript {
    int cameraSpeed = 10;

    Integer shaderID = assetManager.loadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag"));

    Integer meshID = assetManager.loadMesh(Path.of("assets/meshes/uvsphere.obj"));

    Entity A;

    @Override
    protected void Start() {
        A = scene.createObject(meshID, shaderID);

        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    @Override
    protected void Update(double deltaTime) {

        if (input.isKeyDown(GLFW_KEY_W) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(0.0f, 0.0f, cameraSpeed * (float) deltaTime)));
        }

        if (input.isKeyDown(GLFW_KEY_S) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(0.0f, 0.0f, cameraSpeed * (float) -deltaTime)));
        }

        if (input.isKeyDown(GLFW_KEY_A) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(cameraSpeed * (float) deltaTime, 0.0f, 0.0f)));
        }

        if (input.isKeyDown(GLFW_KEY_D) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(cameraSpeed * (float) -deltaTime, 0.0f, 0.0f)));
        }

        if (input.isKeyDown(GLFW_KEY_Q) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(0.0f, cameraSpeed * (float) deltaTime, 0.0f)));
        }

        if (input.isKeyDown(GLFW_KEY_E) == true) {
            scene.cameraTransform.setPosition(
                    scene.cameraTransform.position.add(new Vector3f(0.0f, cameraSpeed * (float) -deltaTime, 0.0f)));
        }

    }
}
