package scripts;

import java.nio.file.Path;

import engine.*;
import engine.rendering.Shader;
import imgui.ImGui;
import imgui.type.ImBoolean;

/**
 * Controls for my underwater shader effects (Tanz).
 *
 * The sun direction, sun colour, fog, bloom and exposure all come from the
 * engine's PostProcess debug window - this script does NOT change them. My
 * shaders read the engine's light and fog values every frame.
 *
 * This script only sends my shaders the values the engine does not:
 * - the time, so the caustics and light rays move
 * - caustics strength, light ray strength and sun brightness (inside my shaders only)
 * It has an "underwater effects" window with sliders and an on/off checkbox.
 *
 * Register it in Game.java (any position after the camera):
 * app.registerScript(new scripts.UnderwaterLighting());
 */
public class UnderwaterLighting extends MonoScript {
    // starting values (all can be changed in the window)
    final float CAUSTICS = 1.5f;        // brightness of the caustic light lines
    final float LIGHT_RAYS = 1.0f;      // light rays (god rays) in the water
    final float SUN_BRIGHTNESS = 3.5f;  // how strongly my shaders use the engine's sun
                                        // (the engine's sun colour can only go up to 1)

    Shader underwaterShader = loadShader("underwater");          // the castle (SunkenCastleScene)
    Shader surfaceShader = loadShader("water_surface");          // WaterSurface
    Shader backgroundShader = loadShader("water_background");    // WaterSurface

    ImBoolean enabled = new ImBoolean(true);
    float[] caustics = { CAUSTICS };
    float[] lightRays = { LIGHT_RAYS };
    float[] sunBrightness = { SUN_BRIGHTNESS };
    float time = 0.0f;

    private Shader loadShader(String name) {
        return Assets.getShader(Assets.LoadShader(
                Path.of("assets/shaders/" + name + ".vert"),
                Path.of("assets/shaders/" + name + ".frag")
        ));
    }

    @Override
    protected void Update(double deltaTime) {
        ImGui.begin("underwater effects");
        ImGui.checkbox("effects on", enabled);
        ImGui.sliderFloat("caustics (light lines)", caustics, 0, 4);
        ImGui.sliderFloat("light rays", lightRays, 0, 4);
        ImGui.sliderFloat("sun brightness", sunBrightness, 0, 10);
        if (ImGui.button("reset")) {
            caustics[0] = CAUSTICS;
            lightRays[0] = LIGHT_RAYS;
            sunBrightness[0] = SUN_BRIGHTNESS;
        }
        ImGui.end();

        // the engine does not send time or these values to material shaders, so
        // they are set here (uniform values stay set until they are changed)
        time += (float) deltaTime;
        boolean on = enabled.get();

        for (Shader shader : new Shader[] { underwaterShader, surfaceShader, backgroundShader }) {
            shader.use();
            shader.setFloat("Time", time);
            shader.setFloat("RayStrength", on ? lightRays[0] : 0.0f);
            shader.setFloat("SunBoost", on ? sunBrightness[0] : 1.0f);
        }
        underwaterShader.use();
        underwaterShader.setFloat("CausticsStrength", on ? caustics[0] : 0.0f);
    }
}
