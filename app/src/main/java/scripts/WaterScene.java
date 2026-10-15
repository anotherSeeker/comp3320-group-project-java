package scripts;

import java.nio.file.Path;

import dev.dominion.ecs.api.Entity;
import engine.Mesh;
import engine.MonoScript;
import engine.components.Material;
import scripts.water.WaterFFT;

public class WaterScene extends MonoScript {
    Integer quadShader = Assets.LoadShader(
            Path.of("assets/shaders/water/noiseTest.vert"),
            Path.of("assets/shaders/water/noiseTest.frag")
    );

    Material quadMat = Assets.createMaterial(
            quadShader,
            0,
            0,
            0,
            0
    );

    Mesh quadMesh = Mesh.Quad();

    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity A;
    //Entity B;

    @Override
    protected void Start() {

        //Scene.SetSkyboxTexture(skyboxTexture);
        //A = Scene.createObject(uvSmoothID, material);
        //A.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));

        //Generate our Gaussian noise array
        WaterFFT.noiseSetup();
        

    }

    @Override
    protected void Update(double deltaTime) {
        //Render it to the screen as a testing step
        WaterFFT.noiseTestDraw();  
    }
}

