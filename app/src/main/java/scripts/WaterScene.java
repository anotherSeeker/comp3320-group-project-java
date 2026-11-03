package scripts;

import static org.lwjgl.opengl.GL11.glGetError;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;
import engine.AssetManager;
import engine.rendering.Mesh;
import engine.MonoScript;
import engine.components.Material;
import engine.components.MaterialEntry;
import engine.components.Transform;
import scripts.water.WaterFFT;

import engine.GLErrors;

public class WaterScene extends MonoScript {
    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity NoisePlane;
    Entity WaterPlane;

    int quadID;
    Material quadMat;

    @Override
    protected void Start() {
        GLErrors.printIfError("start WScene");
            
        //Generate our Gaussian noise array
        WaterFFT.noiseSetup();

        GLErrors.printIfError("noiseSetup WScene");
    
        //compute jonswap params
        WaterFFT.computeWaveSpectrum();

        GLErrors.printIfError("waveSpec WScene");

        //move to update loop later
        WaterFFT.fourierComponentCompute();

        GLErrors.printIfError("fComp WScene");

        WaterFFT.precomputeButterflyTexture();

        GLErrors.printIfError("butterflyShader WScene");

        GLErrors.printIfError("ifft WScene");

        //render our maps to screen
        //drawMapQuads(WaterFFT.noiseIndexId, WaterFFT.waveVecIndexID, WaterFFT.h0IndexID, WaterFFT.dxdzIndexID);
        //drawMapQuads(WaterFFT.noiseIndexId, WaterFFT.waveVecIndexID, WaterFFT.h0IndexID, WaterFFT.dxdzIndexID);
        drawMapQuads(WaterFFT.dxdzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dyxdyzIndexID, WaterFFT.dxxdzzIndexID);
        //makeWaterPlane();

        GLErrors.printIfError("drawMapQuads WScene");

        Scene.SetSkyboxTexture(skyboxTexture);
    }

    @Override
    protected void Update(double deltaTime) 
    {
        //compute the time variant displacement
        //todo

        //generate displacement maps via IFFT
        //todo

        //dispatch water shader
        //
    }

    private void drawScreenQuad(int texIndexID)
    {
        Integer quadShader = Assets.LoadShader(
            Path.of("assets/shaders/water/noiseTest.vert"),
            Path.of("assets/shaders/water/noiseTest.frag")
        );

        quadMat = Assets.createMaterial(new MaterialEntry()
            .add("SHADER", quadShader)
            .add("COLOUR",texIndexID)
        );

        Mesh quadMesh = Mesh.Quad();
        quadID = AssetManager.get().LoadMesh(quadMesh, "noiseQuad");

        NoisePlane = Scene.createObject(quadID, quadMat);
        NoisePlane.get(Transform.class).setPosition(new Vector3f(0f, 0, 0));
    }

    private void drawMapQuads(int ID1, int ID2, int ID3, int ID4)
    {
        Integer quadShader = Assets.LoadShader(
            Path.of("assets/shaders/water/mapDraw.vert"),
            Path.of("assets/shaders/water/mapDraw.frag")
        );

        quadMat = Assets.createMaterial(new MaterialEntry()
            .add("SHADER", quadShader)
            .add("COLOUR",ID1)
            .add("ROUGHNESS",ID2)
            .add("AMBIENT_OCCLUSION",ID3)
            .add("NORMAL", ID4)
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

        Material waterMat = Assets.createMaterial(new MaterialEntry()
            .add("SHADER", waterShader)
            .add("COLOUR",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")))
            .add("ROUGHNESS",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")))
            .add("NORMAL",Assets.LoadTexture(Path.of("assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg")))
        );

        WaterPlane = Scene.createObject(waterMeshID, waterMat);
        WaterPlane.get(Transform.class).setPosition(new Vector3f(0, 0, 0));
    }
}

