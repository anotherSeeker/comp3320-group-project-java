package scripts;

import java.nio.file.Path;

import dev.dominion.ecs.api.Entity;
import engine.AssetManager;
import engine.Mesh;
import engine.MonoScript;
import engine.components.Material;
import scripts.water.WaterFFT;

public class WaterScene extends MonoScript {
    Integer quadShader = Assets.LoadShader(
            Path.of("assets/shaders/water/noiseTest.vert"),
            Path.of("assets/shaders/water/noiseTest.frag")
    );

    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity A;

    int quadID;
    Material quadMat;

    @Override
    protected void Start() {
        //Generate our Gaussian noise array
        WaterFFT.noiseSetup();

        genNoiseQuad();


        Scene.SetSkyboxTexture(skyboxTexture);
        A = Scene.createObject(quadID, quadMat);
        //A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    @Override
    protected void Update(double deltaTime) 
    {

    }

    private void genNoiseQuad()
    {
        //can we do this without supplying every map?
        quadMat = Assets.createMaterial(
                quadShader,
                WaterFFT.noiseTexId,
                WaterFFT.noiseTexId,
                WaterFFT.noiseTexId,
                WaterFFT.noiseTexId
        );
        Mesh quadMesh = Mesh.Quad();
        quadID = AssetManager.get().LoadMesh(quadMesh, "noiseQuad");
    }
}

