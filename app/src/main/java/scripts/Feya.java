package scripts;
import java.io.FileWriter;
import java.io.IOException;
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

    Integer characterModel = Assets.LoadModel(Path.of("assets/meshes/feya combined.glb"));
    Integer characterMesh = Assets.LoadMesh(Path.of("assets/meshes/feya combined.glb"));

    //@formatter:off
    Material material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Roughness.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_NormalGL.jpg")))
        .add("METAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Metalness.jpg")))
    );
    //@formatter:on

    Entity character;
    
    

    @Override
    protected void Start() {

        //character = Scene.createObject(characterMesh, material);
        //character.get(Transform.class).setPosition(new Vector3f(5f, 0f, 0f));

        Integer meshid = Assets.getModel(characterModel).getMeshId();
        character = Scene.createObject(meshid, material);
        character.get(Transform.class).setPosition(new Vector3f(5f, 0f, 0f));
        //Assets.LoadIntoScene(Path.of("assets/meshes/feya combined.glb"), Scene, new Vector3f(0, 0, 5));
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
