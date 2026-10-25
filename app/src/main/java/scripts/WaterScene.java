package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;
import engine.AssetManager;
import engine.Mesh;
import engine.MonoScript;
import engine.components.Material;
import engine.components.Transform;
import scripts.water.WaterFFT;

public class WaterScene extends MonoScript {
    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity NoisePlane;
    Entity WaterPlane;

    int quadID;
    Material quadMat;

    @Override
    protected void Start() {
        //Generate our Gaussian noise array
        WaterFFT.noiseSetup();
        //compute jonswap params
        WaterFFT.computeWaveSpectrum();

        //render our map to screen
        //drawScreenQuad( WaterFFT.noiseIndexId );
        drawScreenQuad( WaterFFT.waveVecIndexID );
        drawScreenQuad( WaterFFT.h0IndexID );
        //makeWaterPlane();

        Scene.SetSkyboxTexture(skyboxTexture);
    }

    @Override
    protected void Update(double deltaTime) 
    {

    }

    private void drawScreenQuad(int texIndexID)
    {
        Integer quadShader = Assets.LoadShader(
            Path.of("assets/shaders/water/noiseTest.vert"),
            Path.of("assets/shaders/water/noiseTest.frag")
        );

        //can we do this without supplying every map?
        quadMat = Assets.createMaterial(
                quadShader,
                texIndexID,
                texIndexID,
                texIndexID,
                texIndexID
        );

        Mesh quadMesh = Mesh.Quad();
        quadID = AssetManager.get().LoadMesh(quadMesh, "noiseQuad");

        NoisePlane = Scene.createObject(quadID, quadMat);
        NoisePlane.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    private void makeWaterPlane()
    {
        Integer waterShader = Assets.LoadShader(
            Path.of("assets/shaders/water/water.vert"),
            Path.of("assets/shaders/water/water.frag")
        );

        Integer waterMeshID = Assets.LoadMesh(Path.of("assets/meshes/WaterTestMesh_Bigger.obj"));

        Material waterMat = Assets.createMaterial(
                waterShader,
                Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")),
                Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")),
                Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")),
                Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg"))
        );

        WaterPlane = Scene.createObject(waterMeshID, waterMat);
        WaterPlane.get(Transform.class).setPosition(new Vector3f(0, 0, 0));
    }
}

