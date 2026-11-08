package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import engine.*;
import engine.rendering.Shader;
import engine.rendering.ShaderPipeline;
import imgui.ImGui;
import imgui.type.ImBoolean;

/**
 * Underwater lighting (Tanz).
 *
 * Sets up the engine's existing lighting and post processing so the scene looks
 * like it is under water:
 * - white sunlight; the castle shader (underwater.frag) works out how the water
 *   absorbs and bends it below the water line, and leaves it as it is above
 * - the fog is blue-green, so far things fade into the water colour
 * - bloom makes the bright water surface glow
 * - a little darker exposure, like being deeper down
 *
 * It only uses settings the engine already has (Scene and PostProcess), so no
 * engine file is changed. It has its own "underwater lighting" window to tweak
 * the values while the game runs, and a checkbox to turn it off.
 *
 * Register it AFTER scripts.PostProcess in Game.java, so its values are used:
 * app.registerScript(new scripts.UnderwaterLighting());
 */
public class UnderwaterLighting extends MonoScript {
    // starting values (all can be changed in the window)
    final float[] SUN_DIRECTION = { 0.6f, 0.8f, 0.4f };     // points towards the sun (lower = longer shadows)
    final float[] SUN_COLOUR = { 1.0f, 0.97f, 0.92f };      // real sunlight (white); underwater.frag lets the
                                                             // water colour it (Beer-Lambert absorption)
    final float SUN_STRENGTH = 3.5f;                         // the engine's sun is dim compared to the sky light
    final float[] FOG_COLOUR = { 0.12f, 0.40f, 0.46f };     // blue-green water
    final float FOG_DENSITY = 0.004f;                        // thin: the engine fog treats distances as about 3x longer (its near plane is 0.1, the camera uses 0.01)
    final float FOG_MOVEMENT = 0.3f;                         // how fast the fog drifts (curl noise)
    final float EXPOSURE = -0.3f;
    final float BLOOM_THRESHOLD = 1.5f;
    final float CAUSTICS = 1.5f;                             // brightness of the caustic light lines
    final float LIGHT_RAYS = 1.0f;                           // light rays (god rays) in the water

    // the castle's shader (used by SunkenCastleScene): gets the time and caustics strength
    Shader underwaterShader = Assets.getShader(Assets.LoadShader(
            Path.of("assets/shaders/underwater.vert"),
            Path.of("assets/shaders/underwater.frag")
    ));
    float time = 0.0f;

    // the water shaders (used by WaterSurface) also draw the light rays
    Shader surfaceShader = Assets.getShader(Assets.LoadShader(
            Path.of("assets/shaders/water_surface.vert"),
            Path.of("assets/shaders/water_surface.frag")
    ));
    Shader backgroundShader = Assets.getShader(Assets.LoadShader(
            Path.of("assets/shaders/water_background.vert"),
            Path.of("assets/shaders/water_background.frag")
    ));

    ImBoolean enabled = new ImBoolean(true);

    float[] sunDirection = SUN_DIRECTION.clone();
    float[] sunColour = SUN_COLOUR.clone();
    float[] sunStrength = { SUN_STRENGTH };
    float[] fogColour = FOG_COLOUR.clone();
    float[] fogDensity = { FOG_DENSITY };
    float[] fogMovement = { FOG_MOVEMENT };
    float[] exposure = { EXPOSURE };
    float[] bloomThreshold = { BLOOM_THRESHOLD };
    float[] caustics = { CAUSTICS };
    float[] lightRays = { LIGHT_RAYS };

    @Override
    protected void Start() {
        PostProcess.Enable(ShaderPipeline.FOG);
        PostProcess.Enable(ShaderPipeline.BLOOM);
    }

    @Override
    protected void Update(double deltaTime) {
        ImGui.begin("underwater lighting");

        if (ImGui.checkbox("underwater lighting on", enabled)) {
            if (enabled.get()) {
                PostProcess.Enable(ShaderPipeline.FOG);
                PostProcess.Enable(ShaderPipeline.BLOOM);
            }
        }

        ImGui.sliderFloat3("sun direction", sunDirection, -1, 1);
        ImGui.colorEdit3("sun colour", sunColour);
        ImGui.sliderFloat("sun strength (shadows)", sunStrength, 0.0f, 10.0f);
        ImGui.colorEdit3("water colour", fogColour);
        ImGui.sliderFloat("water density", fogDensity, 0.0f, 0.03f);
        ImGui.sliderFloat("water movement", fogMovement, 0.0f, 1.0f);
        ImGui.sliderFloat("exposure", exposure, -5, 5);
        ImGui.sliderFloat("glow threshold", bloomThreshold, 0, 5);
        ImGui.sliderFloat("caustics (light lines)", caustics, 0, 4);
        ImGui.sliderFloat("light rays", lightRays, 0, 4);

        if (ImGui.button("reset")) {
            sunDirection = SUN_DIRECTION.clone();
            sunColour = SUN_COLOUR.clone();
            sunStrength[0] = SUN_STRENGTH;
            fogColour = FOG_COLOUR.clone();
            fogDensity[0] = FOG_DENSITY;
            fogMovement[0] = FOG_MOVEMENT;
            exposure[0] = EXPOSURE;
            bloomThreshold[0] = BLOOM_THRESHOLD;
            caustics[0] = CAUSTICS;
            lightRays[0] = LIGHT_RAYS;
        }

        ImGui.end();

        // the caustics move with time (the engine does not send time to material shaders)
        time += (float) deltaTime;
        underwaterShader.use();
        underwaterShader.setFloat("Time", time);
        underwaterShader.setFloat("CausticsStrength", enabled.get() ? caustics[0] : 0.0f);

        // light rays: the same strength in all three shaders, so they match up
        float rays = enabled.get() ? lightRays[0] : 0.0f;
        for (Shader shader : new Shader[] { underwaterShader, surfaceShader, backgroundShader }) {
            shader.use();
            shader.setFloat("RayStrength", rays);
        }

        // when it is off, the PostProcess debug window's values are used instead
        if (!enabled.get()) {
            return;
        }

        // the PostProcess script sets these every frame too, so they are set
        // again here (this script runs after it)
        Scene.lightDirection.set(sunDirection[0], sunDirection[1], sunDirection[2]);
        // brighter sun = stronger contrast between lit areas and the shadows
        Scene.lightColour.set(sunColour[0], sunColour[1], sunColour[2]).mul(sunStrength[0]);

        PostProcess.FogColour = new Vector3f(fogColour[0], fogColour[1], fogColour[2]);
        PostProcess.FogDensity = fogDensity[0];
        PostProcess.CurlStrength = fogMovement[0];
        PostProcess.Exposure = exposure[0];
        PostProcess.BloomThreshold = bloomThreshold[0];
    }
}
