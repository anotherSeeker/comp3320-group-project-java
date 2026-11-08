package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import engine.rendering.Mesh;
import engine.rendering.Shader;

/**
 * The water surface seen from below, and the deep water all around (Tanz).
 *
 * 1. Water surface (assets/shaders/water_surface.vert / .frag):
 *    - moving waves and ripples
 *    - Snell's window: looking steeply up you see the real sky through the
 *      water, bent by the waves; at low angles the surface reflects the water
 *    - the sun glinting through the waves
 * 2. Deep water background (assets/shaders/water_background.vert / .frag):
 *    a big ball around the camera in the water colour, so the sky picture does
 *    not show past the edges of the scene.
 *
 * Register it in Game.java:
 * app.registerScript(new scripts.WaterSurface());
 */
public class WaterSurface extends MonoScript {
    final float WATER_HEIGHT = 14.0f;  // the water surface height (same as in Davies' Blender scene)

    Integer surfaceShaderID = Assets.LoadShader(
            Path.of("assets/shaders/water_surface.vert"),
            Path.of("assets/shaders/water_surface.frag")
    );
    Integer backgroundShaderID = Assets.LoadShader(
            Path.of("assets/shaders/water_background.vert"),
            Path.of("assets/shaders/water_background.frag")
    );

    Integer quadID = Assets.LoadMesh(Mesh.Quad(), "WATER_SURFACE");
    Integer sphereID = Assets.LoadMesh(Path.of("assets/meshes/uvsphere.obj"));

    Material surfaceMaterial = Assets.createMaterial(new MaterialEntry().add("SHADER", surfaceShaderID));
    Material backgroundMaterial = Assets.createMaterial(new MaterialEntry().add("SHADER", backgroundShaderID));

    Shader surfaceShader = Assets.getShader(surfaceShaderID);
    Shader backgroundShader = Assets.getShader(backgroundShaderID);

    float time = 0.0f;

    @Override
    protected void Start() {
        Entity water = Scene.createObject(quadID, surfaceMaterial);
        Entity background = Scene.createObject(sphereID, backgroundMaterial);

        water.get(Transform.class).setPosition(new Vector3f(0.0f, WATER_HEIGHT, 0.0f));

        // The engine draws every object into the shadow map, so the water plane
        // (and the big ball) would block all the sunlight. Scaling them to 0
        // squashes them to a point in the shadow pass (no shadow); their vertex
        // shaders ignore the scale and build the full size themselves.
        water.get(Transform.class).setScale(0.0f);
        background.get(Transform.class).setScale(0.0f);
    }

    @Override
    protected void Update(double deltaTime) {
        time += (float) deltaTime;

        // the engine does not send the time to material shaders, so it is set
        // here (uniform values stay set until they are changed)
        surfaceShader.use();
        surfaceShader.setFloat("Time", time);

        backgroundShader.use();
        backgroundShader.setFloat("Time", time);
    }
}
