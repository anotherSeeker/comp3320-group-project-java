package scripts;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import engine.rendering.ShaderPipeline;
import imgui.ImGui;
import imgui.type.ImBoolean;

public class DemoScene extends MonoScript {

    Integer uvSmoothID = Assets.LoadMesh(Path.of("assets/meshes/uvsmooth.obj"));

    //@formatter:off
    Material tile_material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg")))
    );

    Material brick_Material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Bricks/Bricks066_1K-JPG_NormalGL.jpg")))
    );

    Material concrete_Material = Assets.createMaterial(new MaterialEntry()
        .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Color.jpg")))
        .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_Roughness.jpg")))
        .add("AMBIENT_OCCLUSION",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_AmbientOcclusion.jpg")))
        .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Concrete/Concrete042A_1K-JPG_NormalGL.jpg")))
    );

    Material ornament_Material = Assets.createMaterial(new MaterialEntry()
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
        PostProcess.Enable(ShaderPipeline.BLOOM);

        Scene.SetSkyboxTexture(skyboxTexture);

        Vector3f offset = new Vector3f(0, 0, -5);

        A = Scene.createObject(uvSmoothID, tile_material);
        A.get(Transform.class).setPosition(new Vector3f(0.0f, 0, 0).add(offset));

        B = Scene.createObject(uvSmoothID, brick_Material);
        B.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, 0).add(offset));

        C = Scene.createObject(uvSmoothID, concrete_Material);
        C.get(Transform.class).setPosition(new Vector3f(0.0f, 0, -5.0f).add(offset));

        D = Scene.createObject(uvSmoothID, ornament_Material);
        D.get(Transform.class).setPosition(new Vector3f(-5.0f, 0, -5.0f).add(offset));

        ImGui.setNextWindowPos(0.5f, 0.5f);

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
    }

    private void swapSkybox() {
        if (swapped) {
            Scene.SetSkyboxTexture(skyboxTexture);
        } else {
            Scene.SetSkyboxTexture(barSkybox);
        }
        swapped = !swapped;
    }

    float[] lightColour = new float[] { 1, 1, 1 };
    float[] lightDirection = new float[] { 1, 1, 1 };

    float[] bloomKnee = new float[] { 0.5f };
    float[] bloomThreshold = new float[] { 1.0f };

    float[] vignetteRadius = new float[] { 0.75f };
    float[] vignetteSoftness = new float[] { 0.2f };

    ImBoolean bloomEnabled = new ImBoolean(true);
    ImBoolean vignetteEnabled = new ImBoolean(false);

    @Override
    protected void Update(double deltaTime) {
        if (pressed == false && Input.IsKeyDown(GLFW_KEY_F)) {
            pressed = true;
            swapSkybox();
        } else if (!Input.IsKeyDown(GLFW_KEY_F)) {
            pressed = false;
        }

        ImGui.begin("debug window");

        ImGui.text("Scene properties");
        ImGui.separator();
        ImGui.colorEdit3("lightColour", lightColour);
        ImGui.sliderFloat3("normalised light position", lightDirection, -1, 1);

        ImGui.text("bloom");
        ImGui.separator();
        if (ImGui.checkbox("enable bloom", bloomEnabled)) {
            if (bloomEnabled.get()) {
                PostProcess.Enable(ShaderPipeline.BLOOM);
            } else {
                PostProcess.Disable(ShaderPipeline.BLOOM);
            }
        }

        ImGui.sliderFloat("knee", bloomKnee, 0, 2);
        ImGui.sliderFloat("threshold", bloomThreshold, 0, 5);

        ImGui.text("vignette");
        ImGui.separator();

        if (ImGui.checkbox("enable vignette", vignetteEnabled)) {
            if (vignetteEnabled.get()) {
                PostProcess.Enable(ShaderPipeline.VIGNETTE);
            } else {
                PostProcess.Disable(ShaderPipeline.VIGNETTE);
            }
        }

        ImGui.sliderFloat("radius", vignetteRadius, 0, 1);
        ImGui.sliderFloat("softness", vignetteSoftness, 0.01f, 1);

        Scene.lightColour.x = lightColour[0];
        Scene.lightColour.y = lightColour[1];
        Scene.lightColour.z = lightColour[2];

        Scene.lightDirection.x = lightDirection[0];
        Scene.lightDirection.y = lightDirection[1];
        Scene.lightDirection.z = lightDirection[2];

        PostProcess.BloomKnee = bloomKnee[0];
        PostProcess.BloomThreshold = bloomThreshold[0];

        PostProcess.VignetteRadius = vignetteRadius[0];
        PostProcess.VignetteSoftness = vignetteSoftness[0];
        ImGui.end();

    }
}
