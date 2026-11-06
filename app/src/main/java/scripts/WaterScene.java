package scripts;

//import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.*;

import static org.lwjgl.glfw.GLFW.*;

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
import engine.components.GameObject;

public class WaterScene extends MonoScript {
    Integer skyboxTexture = Assets.LoadCubeMap(Path.of("assets/textures/skyboxes/qwantani_dusk_2_puresky_1k.hdr"));

    Entity NoisePlane;
    Entity WaterPlane;

    int quadID;
    Material quadMat;
    int quadShader;

    int previewState = 0;
    int lastState = 0;

    static int waterUpdateErr = 0;

    @Override
    protected void Start() {
        GLErrors.printIfError("start WScene");
            
        //Generate our Gaussian noise array
        WaterFFT.noiseSetup();

        GLErrors.printIfError("noiseSetup WScene");
    
        //compute jonswap params
        WaterFFT.computeWaveSpectrum();
        //drawMapQuads(WaterFFT.h0IndexID, WaterFFT.waveVecIndexID, WaterFFT.h0IndexID, WaterFFT.h0IndexID);
        //aaaaaaaaa
        WaterFFT.conjSpectra();

        GLErrors.printIfError("waveSpec WScene");

        //precompute butterfly shader for fft
        WaterFFT.precomputeButterflyTexture();
        GLErrors.printIfError("butterflyShader WScene");

        //prep textures and shaders for FComponents
        WaterFFT.setupFourierComponentShader();
        GLErrors.printIfError("setupFComp WScene");

        //prep textures and shaders for the final stages
        WaterFFT.setupIFFT();
        GLErrors.printIfError("setupIFFT WScene");



        GLFWKeyCallback callback = GLFWKeyCallback.create(this::_previewCallback);
        callback.set(Input.getWindow());

        //render our maps to screen
        drawMapQuads(WaterFFT.noiseIndexId, WaterFFT.waveVecIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);

        GLErrors.printIfError("drawMapQuads WScene");

        Scene.SetSkyboxTexture(skyboxTexture);
    }

    @Override
    protected void Update(double deltaTime) 
    {
        waterUpdate();

        //updateMapQuad(WaterFFT.waveVecIndexID, WaterFFT.dydxzIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);
        doWaterMapsPreview();
    }

    private void doWaterMapsPreview()
    {
        //if (lastState != previewState)
        //{
            //lastState = previewState;
            switch (previewState)
            {
                // case 0 -> updateMapQuad(WaterFFT.noiseIndexId, WaterFFT.waveVecIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);
                // case 1 -> updateMapQuad(WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID);
                // case 2 -> updateMapQuad(WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID);
                // case 3 -> updateMapQuad(WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dydxzIndexID);

                case 0 -> updateMapQuad(WaterFFT.h0IndexID, WaterFFT.dydxzIndexID, WaterFFT.waveVecIndexID, WaterFFT.butterflyIndexID);
                case 1 -> updateMapQuad(WaterFFT.dxdzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dyxdyzIndexID, WaterFFT.dxxdzzIndexID);
                case 2 -> updateMapQuad(WaterFFT.dxdzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dyxdyzIndexID, WaterFFT.butterflyIndexID);
                case 3 -> updateMapQuad(WaterFFT.dxdzIndexID, WaterFFT.dydxzIndexID, WaterFFT.dxxdzzIndexID, WaterFFT.butterflyIndexID);

                // case 0 -> updateMapQuad(WaterFFT.h0IndexID, WaterFFT.dydxzIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);
                // case 1 -> updateMapQuad(WaterFFT.h0IndexID, WaterFFT.dydxzIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);
                // case 2 -> updateMapQuad(WaterFFT.h0IndexID, WaterFFT.dydxzIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);
                // case 3 -> updateMapQuad(WaterFFT.h0IndexID, WaterFFT.dydxzIndexID, WaterFFT.h0IndexID, WaterFFT.butterflyIndexID);

                default -> {}
            }
        //}
    }

    private void _previewCallback(long window, int key, int scancode, int action, int mods)
    {
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS)
            previewState = (previewState + 1) % 4;
    }

    private boolean waterUpdate()
    {
        if (waterUpdateErr == 0)
        {
            WaterFFT.fourierComponentCompute();            
            //WaterFFT.computeFinalWaveParameters();
            waterUpdateErr = GLErrors.printIfError("waterUpdate WScene");
            return waterUpdateErr != 0;
        }
        return false;
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
        quadShader = Assets.LoadShader(
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

    private void updateMapQuad(int ID1, int ID2, int ID3, int ID4)
    {
       quadMat = Assets.createMaterial(new MaterialEntry()
            .add("SHADER", quadShader)
            .add("COLOUR",ID1)
            .add("ROUGHNESS",ID2)
            .add("AMBIENT_OCCLUSION",ID3)
            .add("NORMAL", ID4)
        );

        NoisePlane.get(GameObject.class).material = quadMat;
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

