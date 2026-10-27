package scripts;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;

import imgui.ImGui;

public class DemoScene extends MonoScript {

    Integer shaderID = Assets.LoadShader(
            Path.of("assets/shaders/default.vert"),
            Path.of("assets/shaders/default.frag")
    );

    Integer uvSmoothID = Assets.LoadMesh(Path.of("assets/meshes/uvsmooth.obj"));

    //@formatter:off
    Material tile_material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg")))
    );

    Material brick_Material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_NormalGL.jpg")))
    );

    Material concrete_Material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_NormalGL.jpg")))
    );

    Material ornament_Material = Assets.createMaterial(new MaterialEntry()
        .add("SHADER", shaderID)
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Roughness.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_NormalGL.jpg")))
        .add("METAL",Assets.LoadTexture(Path.of("assets/textures/Ornament/ChristmasTreeOrnament021_1K-JPG_Metalness.jpg")))
    );
    //@formatter:on

    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));
    Integer barSkybox = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/warm_restaurant_night_1k.hdr"));

    boolean swapped = false;
    boolean pressed = false;

    Entity A;
    Entity B;
    Entity C;
    Entity D;

    @Override
    protected void Start() {
        Scene.SetSkyboxTexture(skyboxTexture);

        A = Scene.createObject(uvSmoothID, tile_material);
        A.get(Transform.class).setPosition(new Vector3f(0.0f, 0, 0));

        B = Scene.createObject(uvSmoothID, brick_Material);
        B.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, 0));

        C = Scene.createObject(uvSmoothID, concrete_Material);
        C.get(Transform.class).setPosition(new Vector3f(0.0f, 0, -5.0f));

        D = Scene.createObject(uvSmoothID, ornament_Material);
        D.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, -5.0f));

        ImGui.setNextWindowPos(0.5f, 0.5f);
    }

    private void swapSkybox() {
        if (swapped) {
            Scene.SetSkyboxTexture(skyboxTexture);
        } else {
            Scene.SetSkyboxTexture(barSkybox);
        }
        swapped = !swapped;
    }

    float[] roughness = new float[] { 0.5f };
    float[] ambientOcclusion = new float[] { 1.0f };
    float[] metallic = new float[] { 0.0f };

    float[] subsurface = new float[] { 0.0f };
    float[] sheen = new float[] { 0.0f };
    float[] annisotropy = new float[] { 0.0f };
    float[] specularStrength = new float[] { 0.5f };

    float[] sheenColour = new float[] { 1, 1, 1 };
    float[] lightColour = new float[] { 1, 1, 1 };

    float[] lightDirection = new float[] { 1, 1, 1 };

    void updateMaterial(Material material) {
        material.roughness = roughness[0];
        material.ambientOcclusion = ambientOcclusion[0];
        material.subsurface = subsurface[0];
        material.sheen = sheen[0];
        material.annisotropy = annisotropy[0];
        material.specularStrength = specularStrength[0];
        material.metallic = metallic[0];

        material.sheenColour.x = sheenColour[0];
        material.sheenColour.y = sheenColour[1];
        material.sheenColour.z = sheenColour[2];
    }

    @Override
    protected void Update(double deltaTime) {
        if (pressed == false && Input.IsKeyDown(GLFW_KEY_F)) {
            pressed = true;
            swapSkybox();
        } else if (!Input.IsKeyDown(GLFW_KEY_F)) {
            pressed = false;
        }

        ImGui.begin("debug window");

        ImGui.sliderFloat("roughness", roughness, 0, 1);
        ImGui.sliderFloat("ambientOcclusion", ambientOcclusion, 0, 1);
        ImGui.sliderFloat("subsurface", subsurface, 0, 1);
        ImGui.sliderFloat("sheen", sheen, 0, 200);
        ImGui.sliderFloat("annisotropy", annisotropy, 0, 1);
        ImGui.sliderFloat("specularStrength", specularStrength, 0, 1);
        ImGui.sliderFloat("metallic", metallic, 0, 1);

        ImGui.colorEdit3("sheenColour", sheenColour);
        ImGui.colorEdit3("lightColour", lightColour);

        ImGui.sliderFloat3("normalised light position", lightDirection, -1, 1);

        updateMaterial(tile_material);
        updateMaterial(brick_Material);
        updateMaterial(concrete_Material);
        updateMaterial(ornament_Material);

        Scene.lightColour.x = lightColour[0];
        Scene.lightColour.y = lightColour[1];
        Scene.lightColour.z = lightColour[2];

        Scene.lightDirection.x = lightDirection[0];
        Scene.lightDirection.y = lightDirection[1];
        Scene.lightDirection.z = lightDirection[2];

        ImGui.end();

    }
}
