package scripts;

import java.nio.file.Path;
import java.util.Map;

import org.joml.Vector2f;
import org.joml.Vector3f;

import engine.*;
import engine.rendering.ColourCurve.Curve;
import engine.rendering.ShaderPipeline;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;

public class PostProcess extends MonoScript {
    private boolean showControls = true;
    public void showUI(boolean enabled) { showControls = enabled; }
    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));
    Integer barSkybox = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/warm_restaurant_night_1k.hdr"));
    Integer nightSkybox = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/kloppenheim_02_puresky_1k.hdr"));

    Map<String, Integer> skyboxes = Map.of("dusk", skyboxTexture, "night", nightSkybox, "bar", barSkybox);
    String[] skyboxArray = { "dusk", "night", "bar" };
    ImInt skyboxIndex = new ImInt(0);

    @Override
    protected void Start() {
        Scene.SetSkyboxTexture(skyboxes.get(skyboxArray[skyboxIndex.get()]));

    }

    float[] lightColour = new float[] { 1, 1, 1 };
    float[] lightDirection = new float[] { 1, 1, 1 };

    float[] bloomKnee = new float[] { 0.5f };
    float[] bloomThreshold = new float[] { 1.0f };

    float[] vignetteRadius = new float[] { 0.75f };
    float[] vignetteSoftness = new float[] { 0.2f };

    float[] red_p0 = new float[] { 0.0f };
    float[] red_p1 = new float[] { 0.25f, 0.25f };
    float[] red_p2 = new float[] { 0.75f, 0.75f };
    float[] red_p3 = new float[] { 1.0f, 1.0f, 1.0f };

    float[] green_p0 = new float[] { 0.0f };
    float[] green_p1 = new float[] { 0.25f, 0.25f };
    float[] green_p2 = new float[] { 0.75f, 0.75f };
    float[] green_p3 = new float[] { 1.0f, 1.0f, 1.0f };

    float[] blue_p0 = new float[] { 0.0f };
    float[] blue_p1 = new float[] { 0.25f, 0.25f };
    float[] blue_p2 = new float[] { 0.75f, 0.75f };
    float[] blue_p3 = new float[] { 1.0f, 1.0f, 1.0f };

    float[] maxBrightness = new float[] { 1.0f };
    float[] contrast = new float[] { 1.0f };
    float[] linSectStart = new float[] { 0.22f };
    float[] linSectLength = new float[] { 0.4f };
    float[] black = new float[] { 1.33f };
    float[] pedestal = new float[] { 0.0f };

    float[] whitepoint = new float[] { 11.2f };
    float[] exposure = new float[] { -1.5f };

    float[] fogDensity = new float[] { 0.05f };
    float[] curlStrength = new float[] { 0.04f };
    float[] fogColour = new float[] { 1.0f, 1.0f, 1.0f };

    ImBoolean bloomEnabled = new ImBoolean(false);
    ImBoolean vignetteEnabled = new ImBoolean(false);
    ImBoolean gradingEnabled = new ImBoolean(false);
    ImBoolean fogEnabled = new ImBoolean(false);

    private void colourGradingUi() {

        if (ImGui.checkbox("enable colour grading", gradingEnabled)) {
            if (gradingEnabled.get()) {
                PostProcess.Enable(ShaderPipeline.COLOUR_GRADE);
            } else {
                PostProcess.Disable(ShaderPipeline.COLOUR_GRADE);
            }
        }

        ImGui.text("red");
        ImGui.separator();

        boolean red_updated = false;

        red_updated |= ImGui.sliderFloat("red p0", red_p0, 0, 1);
        red_updated |= ImGui.sliderFloat2("red p1", red_p1, 0, 1);
        red_updated |= ImGui.sliderFloat2("red p2", red_p2, 0, 1);
        red_updated |= ImGui.sliderFloat("red p3", red_p3, 0, 1);

        if (red_updated) {
            PostProcess.ColourGradeCurve.red = new Curve(
                    red_p0[0],
                    new Vector2f(red_p1[0], red_p1[1]),
                    new Vector2f(red_p2[0], red_p2[1]),
                    red_p3[0]
            );

            PostProcess.ColourGradeCurve.update();
        }

        ImGui.text("green");
        ImGui.separator();

        boolean green_updated = false;

        green_updated |= ImGui.sliderFloat("green p0", green_p0, 0, 1);
        green_updated |= ImGui.sliderFloat2("green p1", green_p1, 0, 1);
        green_updated |= ImGui.sliderFloat2("green p2", green_p2, 0, 1);
        green_updated |= ImGui.sliderFloat("green p3", green_p3, 0, 1);

        if (green_updated) {
            PostProcess.ColourGradeCurve.green = new Curve(
                    green_p0[0],
                    new Vector2f(green_p1[0], green_p1[1]),
                    new Vector2f(green_p2[0], green_p2[1]),
                    green_p3[0]
            );

            PostProcess.ColourGradeCurve.update();
        }

        ImGui.text("blue");
        ImGui.separator();

        boolean blue_updated = false;

        blue_updated |= ImGui.sliderFloat("blue p0", blue_p0, 0, 1);
        blue_updated |= ImGui.sliderFloat2("blue p1", blue_p1, 0, 1);
        blue_updated |= ImGui.sliderFloat2("blue p2", blue_p2, 0, 1);
        blue_updated |= ImGui.sliderFloat("blue p3", blue_p3, 0, 1);

        if (blue_updated) {
            PostProcess.ColourGradeCurve.blue = new Curve(
                    blue_p0[0],
                    new Vector2f(blue_p1[0], blue_p1[1]),
                    new Vector2f(blue_p2[0], blue_p2[1]),
                    blue_p3[0]
            );

            PostProcess.ColourGradeCurve.update();
        }

    }

    @Override
    protected void Update(double deltaTime) {
        if (!showControls) return;
        ImGui.setNextWindowPos(750, 12, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.setNextWindowSize(335, 460, imgui.flag.ImGuiCond.FirstUseEver);
        ImGui.begin("Scene and post processing");

        ImGui.text("Scene properties");
        ImGui.separator();
        ImGui.colorEdit3("lightColour", lightColour);
        ImGui.sliderFloat3("normalised light position", lightDirection, -1, 1);

        if (ImGui.combo("skybox", skyboxIndex, skyboxArray)) {
            Scene.SetSkyboxTexture(skyboxes.get(skyboxArray[skyboxIndex.get()]));
        }
        ;

        boolean computeSupported = PostProcess.SupportsCompute();
        if (!computeSupported) ImGui.textWrapped("Compute effects require OpenGL 4.3. HDR and lighting remain available.");
        if (ImGui.beginTabBar("Post processing")) {
        ImGui.beginDisabled(!computeSupported);

        if (ImGui.beginTabItem("Bloom")) {
            if (ImGui.checkbox("enable bloom", bloomEnabled)) {
                if (bloomEnabled.get()) {
                    PostProcess.Enable(ShaderPipeline.BLOOM);
                } else {
                    PostProcess.Disable(ShaderPipeline.BLOOM);
                }
            }

            ImGui.sliderFloat("knee", bloomKnee, 0, 2);
            ImGui.sliderFloat("threshold", bloomThreshold, 0, 5);

            ImGui.endTabItem();
        }

        if (ImGui.beginTabItem("vignette")) {
            if (ImGui.checkbox("enable vignette", vignetteEnabled)) {
                if (vignetteEnabled.get()) {
                    PostProcess.Enable(ShaderPipeline.VIGNETTE);
                } else {
                    PostProcess.Disable(ShaderPipeline.VIGNETTE);
                }
            }

            ImGui.sliderFloat("radius", vignetteRadius, 0, 1);
            ImGui.sliderFloat("softness", vignetteSoftness, 0.01f, 1);

            ImGui.endTabItem();
        }

        if (ImGui.beginTabItem("colour grading")) {
            colourGradingUi();
            ImGui.endTabItem();
        }

        if (ImGui.beginTabItem("Fog")) {
            if (ImGui.checkbox("enable fog", fogEnabled)) {
                if (fogEnabled.get()) {
                    PostProcess.Enable(ShaderPipeline.FOG);
                } else {
                    PostProcess.Disable(ShaderPipeline.FOG);
                }
            }

            ImGui.sliderFloat("curl strength", curlStrength, 0.0f, 1.0f);
            ImGui.sliderFloat("fog density", fogDensity, 0.0f, 0.25f);
            ImGui.colorEdit3("fog colour", fogColour);
            ImGui.endTabItem();
        }

        ImGui.endDisabled();
        if (ImGui.beginTabItem("HDR")) {

            ImGui.sliderFloat("max brightness", maxBrightness, 0.3f, 20.0f);
            ImGui.sliderFloat("contrast", contrast, 0.3f, 2);
            ImGui.sliderFloat("linear section Start", linSectStart, 0.05f, 1);
            ImGui.sliderFloat("linear section Length", linSectLength, 0, 0.95f);
            ImGui.sliderFloat("black", black, 0, 3);
            ImGui.sliderFloat("pedestal", pedestal, 0, 1);

            ImGui.sliderFloat("exposure", exposure, -5, 5);
            ImGui.sliderFloat("whitepoint", whitepoint, 0, 15);
            ImGui.endTabItem();
        }

        ImGui.endTabBar();
        }

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

        PostProcess.MaxBrightness = maxBrightness[0];
        PostProcess.Contrast = contrast[0];
        PostProcess.LinSectStart = linSectStart[0];
        PostProcess.LinSectLength = linSectLength[0];
        PostProcess.Black = black[0];
        PostProcess.Pedestal = pedestal[0];

        PostProcess.Exposure = exposure[0];
        PostProcess.Whitepoint = whitepoint[0];

        PostProcess.FogDensity = fogDensity[0];
        PostProcess.CurlStrength = curlStrength[0];
        PostProcess.FogColour = new Vector3f(fogColour[0], fogColour[1], fogColour[2]);

        ImGui.end();
    }
}
