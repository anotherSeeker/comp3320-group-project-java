package engine;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import engine.components.*;
import engine.particles.ExampleParticleEmitter;
import engine.particles.Particle;

public class SceneGraph {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    private ExampleParticleEmitter[] particleEmitters;

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

    public Matrix4f computeViewMatrix () {
        Matrix4f view = new Matrix4f()
                .rotate(cameraTransform.orientation.conjugate())
                .translate(-cameraTransform.position.x,
                           -cameraTransform.position.y,
                           -cameraTransform.position.z
                );
        return view;
    }

    public Matrix4f computeViewportMatrix (Matrix4f viewMatrix) {
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, 0.01f, 100.0f);
        return projection.mul(viewMatrix);
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
            material.shader.setTexture("Albedo", material.albedoMap, 4);
            material.shader.setTexture("RoughnessMap", material.roughnessMap, 5);
            material.shader.setTexture("AmbientOcclusionMap", material.ambientOcclusionMap, 6);
            material.shader.setTexture("NormalMap", material.normalMap, 7);
            material.shader.setVector3("LightDirection", lightDirection);
            material.shader.setVector3("ViewPosition", cameraTransform.position);

            material.shader.setInt("IrradianceMap", 1);
            material.shader.setInt("PrefilterMap", 2);
            material.shader.setInt("brdfIntMap", 3);

            material.shader.setFloat("Roughness", material.roughness);
            material.shader.setFloat("AmbientOcclusion", material.ambientOcclusion);

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

    // Particle methods
    public void drawParticles () {
        var emitters = ECS.findEntitiesWith(ExampleParticleEmitter.class); 
    
        glDepthMask(false);
        glEnable(GL_BLEND);

        Matrix4f viewMatrix = computeViewMatrix();
        Matrix4f viewportMatrix = computeViewportMatrix(viewMatrix);

        for (var iter = emitters.iterator(); iter.hasNext() != false;) {
            var object = iter.next();
            glBlendFunc(GL_SRC_ALPHA, object.comp().baseParticle.blendFunction);

            object.comp().baseParticle.material.shader.use();

            for (Particle p : object.comp().particles) {

                viewMatrix.transpose3x3(p.transform.model);
                p.setScale();

                p.mesh.bind();
                p.material.shader.setMatrix4("CameraMatrix", viewportMatrix);
                p.material.shader.setTexture("Albedo", p.material.albedoMap, 0);
                p.material.shader.setMatrix4("Model", p.transform.model);
                p.mesh.drawParticles();
            }
        }

        glDepthFunc(GL_LESS);
        glDisable(GL_BLEND);
        glDepthMask(true);
    }

    public void setParticleEmitters (ExampleParticleEmitter[] particleEmitters) {
        this.particleEmitters = particleEmitters;
    }

    public ExampleParticleEmitter[] getParticleEmitters () {
        return particleEmitters;
    }

    public Entity createParticleEmitter (Integer meshID, Material material, float scale, int maxParticles, long creationPeriodMillis, Vector3f velocity, long ttl, int blendFunction) {
        Mesh mesh = assetManager.getMesh(meshID);

        Transform transform = new Transform(new Matrix4f());

        Particle baseParticle = new Particle(mesh, material.shader, material.albedoMap, transform.model, velocity, ttl, scale, blendFunction);
        ExampleParticleEmitter particleEmitter = new ExampleParticleEmitter(baseParticle, maxParticles, creationPeriodMillis);

        return ECS.createEntity(particleEmitter);
    }
}
