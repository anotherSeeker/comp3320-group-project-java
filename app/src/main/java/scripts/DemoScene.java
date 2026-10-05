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

    Integer uvSmoothID = Assets.LoadMesh(Path.of("assets/meshes/uvsmooth.obj"));

    Material material = Assets.createMaterial(
            shaderID,
            Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")),
            Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")),
            Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")),
            Assets.LoadTexture(Path.of("app/assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg"))
    );

    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity A;
    Entity B;

    @Override
    protected void Start() {

        Scene.SetSkyboxTexture(skyboxTexture);

        A = Scene.createObject(uvSmoothID, material);
        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
