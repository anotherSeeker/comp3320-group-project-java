package scripts;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.Transform;

import imgui.ImGui;

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
            Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg"))
    );

    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));
    Integer barSkybox = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/warm_restaurant_night_1k.hdr"));

    boolean swapped = false;
    boolean pressed = false;

    Entity A;
    Entity B;

    @Override
    protected void Start() {
        Scene.SetSkyboxTexture(skyboxTexture);

        A = Scene.createObject(uvSmoothID, material);
        A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
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

    float[] subsurface = new float[] { 0.0f };
    float[] sheen = new float[] { 0.0f };
    float[] annisotropy = new float[] { 0.0f };
    float[] specularStrength = new float[] { 0.5f };

    float[] sheenColour = new float[] { 1, 1, 1 };
    float[] lightColour = new float[] { 1, 1, 1 };

    float[] lightDirection = new float[] { 1, 1, 1 };

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
        ImGui.sliderFloat("sheen", sheen, 0, 1);
        ImGui.sliderFloat("annisotropy", annisotropy, 0, 1);
        ImGui.sliderFloat("specularStrength", specularStrength, 0, 1);

        ImGui.colorEdit3("sheenColour", sheenColour);
        ImGui.colorEdit3("lightColour", lightColour);

        ImGui.sliderFloat3("lightDirection", lightDirection, -1, 1);

        material.roughness = roughness[0];
        material.ambientOcclusion = ambientOcclusion[0];
        material.subsurface = subsurface[0];
        material.sheen = sheen[0];
        material.annisotropy = annisotropy[0];
        material.specularStrength = specularStrength[0];

        material.sheenColour.x = sheenColour[0];
        material.sheenColour.y = sheenColour[1];
        material.sheenColour.z = sheenColour[2];

        Scene.lightColour.x = lightColour[0];
        Scene.lightColour.y = lightColour[1];
        Scene.lightColour.z = lightColour[2];

        Scene.lightDirection.x = lightDirection[0];
        Scene.lightDirection.y = lightDirection[1];
        Scene.lightDirection.z = lightDirection[2];

        ImGui.end();

    }
}
