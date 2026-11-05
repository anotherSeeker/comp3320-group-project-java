package scripts;

import java.nio.file.Path;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import engine.rendering.Mesh;

public class DemoScene extends MonoScript {

    Integer uvSmoothID = Assets.LoadMesh(Path.of("assets/meshes/uvsmooth.obj"));

    //@formatter:off
    Material tile_material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg")))
        .add("DISPLACEMENT", Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Displacement.jpg")))
    );

    Material brick_Material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_NormalGL.jpg")))
        .add("DISPLACEMENT", Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Displacement.jpg")))
    );

    Material concrete_Material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_NormalGL.jpg")))
        .add("DISPLACEMENT", Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Displacement.jpg")))
    );

    Material ornament_Material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Roughness.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_NormalGL.jpg")))
        .add("METAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Metalness.jpg")))
        .add("DISPLACEMENT", Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Displacement.jpg")))
    );
    //@formatter:on

    Integer plate = Assets.LoadMesh(Mesh.Quad(), "PLATE");

    Entity A;
    Entity B;
    Entity C;
    Entity D;
    Entity floor;

    @Override
    protected void Start() {

        Vector3f offset = new Vector3f(0, 0, -5);

        A = Scene.createObject(uvSmoothID, tile_material);
        A.get(Transform.class).setPosition(new Vector3f(0.0f, 0, 0).add(offset));

        B = Scene.createObject(uvSmoothID, brick_Material);
        B.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, 0).add(offset));

        C = Scene.createObject(uvSmoothID, concrete_Material);
        C.get(Transform.class).setPosition(new Vector3f(0.0f, 0, -5.0f).add(offset));

        D = Scene.createObject(uvSmoothID, ornament_Material);
        D.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, -5.0f).add(offset));

        floor = Scene.createObject(plate, concrete_Material);
        floor.get(Transform.class).setPosition(new Vector3f(0.0f, -5.0f, 0.0f));
        floor.get(Transform.class).setOrientationEuler(new Vector3f((float) -Math.PI / 2.0f, 0.0f, 0.0f));

        Assets.LoadIntoScene(Path.of("assets/DamagedHelmet.glb"), Scene, new Vector3f(0, 0, 0));

        ornament_Material.metallic = 1;
        ornament_Material.roughness = 0.3f;
        brick_Material.roughness = 0.8f;
        concrete_Material.roughness = 0.8f;
        tile_material.roughness = 0.4f;

        ornament_Material.specularStrength = 0.5f;
        brick_Material.specularStrength = 0.5f;
        concrete_Material.specularStrength = 0.5f;
        tile_material.specularStrength = 0.5f;

        ornament_Material.annisotropy = 1;
        ornament_Material.displacement = 0.2f;

        // brick_Material.displacement = 0;
    }

    @Override
    protected void Update(double deltaTime) {

    }
}
