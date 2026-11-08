package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import engine.*;
import engine.components.Material;
import engine.components.MaterialEntry;

/**
 * Davies' sunken castle scene in the Java engine (Tanz).
 *
 * Loads the castle, seabed, rocks/coral/seaweed and trees that were exported
 * from Davies' Blender scene (app/assets/meshes/sunken/), and gives them the
 * engine's PBR materials (temporary textures from assets/textures), so the
 * engine's lighting and shadows work on them. The materials use
 * underwater.vert/.frag: the team's PBR shader plus caustics, blue-green
 * ambient light and softer shadows. Use it with UnderwaterLighting (which
 * moves the caustics) and WaterSurface.
 *
 * The models are already in the right place (Y-up, seabed at height 0), so
 * nothing needs moving. In Game.java, use it instead of scripts.DemoScene, and
 * register it AFTER scripts.Camera (so the camera starts in front of the castle):
 * app.registerScript(new scripts.SunkenCastleScene());
 */
public class SunkenCastleScene extends MonoScript {
    final String MESHES = "assets/meshes/sunken/";
    // the Camera script starts facing +Z, so start on the -Z side looking at the castle
    final Vector3f CAMERA_START = new Vector3f(0, 6, -30);

    // the team's PBR shader with underwater lighting added (caustics, blue-green
    // ambient light, softer shadows) - see assets/shaders/underwater.frag
    Integer underwaterShader = Assets.LoadShader(
            Path.of("assets/shaders/underwater.vert"),
            Path.of("assets/shaders/underwater.frag")
    );

    Integer seabedMesh = Assets.LoadMesh(Path.of(MESHES + "sunken_seabed.obj"));
    Integer castleMesh = Assets.LoadMesh(Path.of(MESHES + "sunken_castle.obj"));
    Integer landscapeMesh = Assets.LoadMesh(Path.of(MESHES + "sunken_landscape.obj"));   // rocks, coral, seaweed
    Integer extrasMesh = Assets.LoadMesh(Path.of(MESHES + "sunken_extras.obj"));         // trees, stones

    Material stoneMaterial = textureSet("Bricks/Bricks066_1K-JPG", false);
    Material groundMaterial = textureSet("Concrete/Concrete042A_1K-JPG", true);
    Material rockMaterial = textureSet("Concrete/Concrete042A_1K-JPG", true);

    /** A PBR material from one ambientCG texture set (colour, roughness, AO, normal). */
    private Material textureSet(String name, boolean hasMetal) {
        String folder = "assets/textures/" + name;
        MaterialEntry entry = new MaterialEntry()
                .add("SHADER", underwaterShader)
                .add("COLOUR", Assets.LoadTexture(Path.of(folder + "_Color.jpg")))
                .add("ROUGHNESS", Assets.LoadTexture(Path.of(folder + "_Roughness.jpg")))
                .add("AMBIENT_OCCLUSION", Assets.LoadTexture(Path.of(folder + "_AmbientOcclusion.jpg")))
                .add("NORMAL", Assets.LoadTexture(Path.of(folder + "_NormalGL.jpg")));
        if (hasMetal) {
            entry.add("METAL", Assets.LoadTexture(Path.of(folder + "_Metalness.jpg")));
        }
        return Assets.createMaterial(entry);
    }

    @Override
    protected void Start() {
        // rough, non-shiny surfaces under water; no tessellation displacement
        // (the models' UVs are stretched, so displacement would look bumpy)
        for (Material material : new Material[] { stoneMaterial, groundMaterial, rockMaterial }) {
            material.roughness = 0.9f;
            material.specularStrength = 0.3f;
            material.displacement = 0.0f;
        }

        Scene.createObject(seabedMesh, groundMaterial);
        Scene.createObject(castleMesh, stoneMaterial);
        Scene.createObject(landscapeMesh, rockMaterial);
        Scene.createObject(extrasMesh, rockMaterial);

        Scene.cameraTransform.setPosition(new Vector3f(CAMERA_START));
    }
}
