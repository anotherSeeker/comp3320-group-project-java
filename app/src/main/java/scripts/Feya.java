package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry; 
import engine.components.Transform;

public class Feya extends MonoScript {
    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag")
    );

    Integer characterMesh = Assets.LoadMesh(Path.of("assets/meshes/character_combined.obj"));

    //@formatter:off
    Material material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg")))
    );
    //@formatter:on

    Entity character;

    @Override
    protected void Start() {

        character = Scene.createObject(characterMesh, material);
        character.get(Transform.class).setPosition(new Vector3f(5f, 0f, 0f));
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
