package scripts;

import java.nio.file.Path;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import engine.*;

public class DemoScene extends MonoScript {
    Integer shaderID = assetManager.loadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag"));

    Integer meshID = assetManager.loadMesh(Path.of("assets/meshes/uvsphere.obj"));

    Matrix4f transformA = new Matrix4f().translate(new Vector3f(-0.75f, 0, 0));
    Matrix4f transformB = new Matrix4f().translate(new Vector3f(0.75f, 0, 0));

    @Override
    protected void Start() {
        scene.createObject(meshID, shaderID, transformA);
        scene.createObject(meshID, shaderID, transformB);

        System.out.println("hello world");
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
