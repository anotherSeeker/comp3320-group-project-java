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
import scripts.Animation.AnimationData;

public class Feya extends MonoScript {
    private AnimationData animationData;

    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/anim.vert"),
            Path.of("assets/shaders/anim.frag")
    );

    Integer characterModel = Assets.LoadModel(Path.of("assets/meshes/feya combined.glb"));
    Integer characterMesh = Assets.LoadMesh(Path.of("assets/meshes/feya combined.glb"));

    //@formatter:off
    Material material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_NormalGL.jpg")))
    );
    //@formatter:on

    Entity character;
    
    

    @Override
    protected void Start() {
        animationData = new AnimationData(Assets.getModel(characterModel).getAnimationList().get(0));
        character = Scene.createAnimatedObject(material, characterModel);
        character.get(Transform.class).setAnimationData(animationData);
        character.get(Transform.class).setPosition(new Vector3f(5f, 0f, 0f));
    }

    @Override
    protected void Update(double deltaTime) {
        animationData.IterateNFrames(deltaTime*24);
    }
}
