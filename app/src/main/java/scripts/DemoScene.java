package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import engine.*;
import engine.components.Transform;

public class DemoScene extends MonoScript {
    @Override
    protected void Start() {
        Integer shaderID = assetManager.loadShader(
                Path.of("assets/shaders/default.vert"),
                Path.of("assets/shaders/default.frag"));

        Integer meshID = assetManager.loadMesh(Path.of("assets/meshes/uvsphere.obj"));

        var A = scene.createObject(meshID, shaderID);
        var B = scene.createObject(meshID, shaderID);

        A.get(Transform.class).setPosition(new Vector3f(-1f, 0, 0));
        B.get(Transform.class).setPosition(new Vector3f(1f, 0, 0));

        System.out.println("hello world");
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
