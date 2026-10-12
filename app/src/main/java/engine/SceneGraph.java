package engine;

import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.GameObject;
import engine.components.Material;
import engine.components.Transform;

public class SceneGraph {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    private CubeTexture skyboxTexture = null;
    private Shader skyboxShader;
    private Mesh skyboxMesh = Mesh.Cube();

    public Transform cameraTransform = new Transform(new Matrix4f());
    public Vector3f lightDirection = new Vector3f(1, 1, 0);

    public int fov = 60;
    public float aspectRatio;

    Dominion ECS = Dominion.create();

    private AssetManager assetManager = AssetManager.get();

    public SceneGraph() {
        cameraTransform.setPosition(new Vector3f(0, 0, 0));

        skyboxShader = assetManager.getShader(assetManager.skyboxShader);
    }

    public void updateAspectRatio(int width, int height) {
        aspectRatio = (float) width / height;
    }

    public Matrix4f computeViewportMatrix() {
        Matrix4f view = new Matrix4f()
                .rotate(cameraTransform.orientation.conjugate())
                .translate(
                        -cameraTransform.position.x,
                        -cameraTransform.position.y,
                        -cameraTransform.position.z
                );

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);

        return projection.mul(view);
    }

    public Matrix4f computeSkyboxMatrix() {
        Matrix4f view = new Matrix4f()
                .rotate(cameraTransform.orientation.conjugate()).invert();

        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);

        return projection.mul(view);
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

            material.shader.setInt("IrradianceMap", 1);
            material.shader.setInt("PrefilterMap", 2);
            material.shader.setInt("brdfIntMap", 3);

            material.bindAsUniform();

            gameObject.mesh.bind();
            gameObject.mesh.draw();
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
