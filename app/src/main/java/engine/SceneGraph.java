package engine;

import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.GameObject;
import engine.components.Material;
import engine.components.Transform;
import engine.rendering.CubeTexture;
import engine.rendering.Mesh;
import engine.rendering.Shader;
import engine.rendering.ShadowMap;

public class SceneGraph {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    private ShadowMap shadowMap = new ShadowMap(2048, 2048);

    private CubeTexture skyboxTexture = null;
    private Shader skyboxShader;
    private Mesh skyboxMesh = Mesh.Cube();

    public Transform cameraTransform = new Transform(new Matrix4f());
    public Vector3f lightDirection = new Vector3f(1, 1, 0);
    public Vector3f lightColour = new Vector3f(1, 1, 1);

    public int fov = 60;
    public float aspectRatio;
    public int screenWidth;
    public int screenHeight;

    Dominion ECS = Dominion.create();

    private AssetManager assetManager = AssetManager.get();

    public SceneGraph() {
        cameraTransform.setPosition(new Vector3f(0, 0, 0));

        skyboxShader = assetManager.getShader(assetManager.skyboxShader);
    }

    public void updateAspectRatio(int width, int height) {
        aspectRatio = (float) width / height;

        shadowMap.onResize(width, height);

        screenWidth = width;
        screenHeight = height;
    }

    public Matrix4f computeViewportMatrix() {

        Quaternionf conj = new Quaternionf(cameraTransform.orientation).conjugate();

        Matrix4f view = new Matrix4f()
                .rotate(conj)
                .translate(
                        -cameraTransform.position.x,
                        -cameraTransform.position.y,
                        -cameraTransform.position.z
                );

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);

        return projection.mul(view);
    }

    public Matrix4f computeSkyboxMatrix() {

        Quaternionf conj = new Quaternionf(cameraTransform.orientation).conjugate();

        Matrix4f view = new Matrix4f()
                .rotate(conj);

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);

        return projection.mul(view);
    }

    public void generateShadows() {
        var drawables = ECS.findEntitiesWith(GameObject.class, Transform.class);

        shadowMap.genShadowsFromDirection(lightDirection);
        for (var iter = drawables.iterator(); iter.hasNext() != false;) {
            var object = iter.next();
            GameObject gameObject = object.comp1();
            Transform transform = object.comp2();

            ShadowMap.shadowPass.setMatrix4("Model", transform.model);
            gameObject.mesh.draw();
        }

        shadowMap.end();
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public void draw() {
        var drawables = ECS.findEntitiesWith(GameObject.class, Transform.class);
        Matrix4f viewportMatrix = computeViewportMatrix();

        if (skyboxTexture != null) {
            glDepthMask(false);
            glDepthFunc(GL_LEQUAL);
            skyboxShader.use();

            skyboxTexture.use(0, 1, 2, 3);

            skyboxShader.setMatrix4("CameraMatrix", computeSkyboxMatrix());
            skyboxShader.setInt("Skybox", 0);

            skyboxMesh.bind();
            skyboxMesh.draw();

            glDepthFunc(GL_LESS);
            glDepthMask(true);
        }

        for (var iter = drawables.iterator(); iter.hasNext() != false;) {
            var object = iter.next();
            GameObject gameObject = object.comp1();
            Transform transform = object.comp2();

            Material material = gameObject.material;
            material.shader.use();

            material.shader.setMatrix4("Model", transform.model);
            material.shader.setMatrix4("CameraMatrix", viewportMatrix);
            material.shader.setVector3("LightDirection", lightDirection);
            material.shader.setVector3("ViewPosition", cameraTransform.position);
            material.shader.setVector3("DiffuseColour", lightColour);

            material.shader.setMatrix4("LightMatrix", shadowMap.lightMatrix);

            material.shader.setInt("IrradianceMap", 1);
            material.shader.setInt("PrefilterMap", 2);
            material.shader.setInt("brdfIntMap", 3);

            glActiveTexture(GL_TEXTURE0 + 10);
            glBindTexture(GL_TEXTURE_2D, shadowMap.shadowTexture);
            material.shader.setInt("ShadowMap", 10);

            material.bindAsUniform();

            gameObject.mesh.bind();
            gameObject.mesh.drawPatches();
        }
    }

    public Entity createObject(Integer meshID, Material material) {

        Mesh mesh = assetManager.getMesh(meshID);

        GameObject gameObject = new GameObject(mesh, material);
        Transform transform = new Transform(new Matrix4f());

        return ECS.createEntity(gameObject, transform);
    }

    public void SetSkyboxTexture(Integer cubeTexture) {
        skyboxTexture = assetManager.getCubeTexture(cubeTexture);
    }
}
