package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.Transform;

public class DemoScene extends MonoScript {
    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag")
    );

    Integer meshID = Assets.LoadMesh(Path.of("assets/meshes/uvsphere.obj"));

    Material material = Assets.createMaterial(
            shaderID,
            Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg"))
    );

    Entity A;
    Entity B;

    @Override
    protected void Start() {

        A = Scene.createObject(meshID, material);
        B = Scene.createObject(meshID, material);

        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
        B.get(Transform.class).setPosition(new Vector3f(-5f, 0, 0));

    }

    @Override
    protected void Update(double deltaTime) {

    }
}
