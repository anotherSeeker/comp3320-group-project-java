package engine;

import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
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
import scripts.Animation.AnimationData;
import engine.rendering.CubeTexture;
import engine.rendering.Mesh;
import engine.rendering.Shader;
import scripts.Animation.Model;
import engine.particles.*;
import engine.rendering.ShadowMap;

public class SceneGraph {
    static {
        System.setProperty("dominion.show-banner", "false");
    }

    public enum EmitterType {
        GENERIC, BUBBLE
    }

    private int numOfEmitterTypes = EmitterType.values().length;
    private GenericParticleEmitter[] genericParticleEmitters;
    private BubbleParticleEmitter[] bubbleParticleEmitters;

    private Matrix4f viewMatrix;
    private Matrix4f viewportMatrix;
    
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
        viewportMatrix = computeViewportMatrix();

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
            material.shader.setInt("ShadowMapComparison", 10);

            material.bindAsUniform();

            gameObject.mesh.bind();

            AnimationData animationData = transform.getAnimationData();
            if (animationData == null) {
                material.shader.setUniform("bonesMatrices", AnimationData.DEFAULT_BONES_MATRICES);
            } else {
                material.shader.setUniform("bonesMatrices", animationData.getCurrentFrame().boneMatrices());
            }   
            
            gameObject.mesh.draw();
            gameObject.mesh.drawPatches();
        }
    }

    public Entity createObject(Integer meshID, Material material) {

        Mesh mesh = assetManager.getMesh(meshID);

        GameObject gameObject = new GameObject(mesh, material);
        Transform transform = new Transform(new Matrix4f());

        return ECS.createEntity(gameObject, transform);
    }

    public Entity createAnimatedObject(Material material, Integer modelID) {
        Model model = assetManager.getModel(modelID);
        Mesh mesh = assetManager.getMesh(model.getMeshId());

        GameObject gameObject = new GameObject(mesh, material, model);
        Transform transform = new Transform(new Matrix4f());

        return ECS.createEntity(gameObject, transform);
    }

    public void SetSkyboxTexture(Integer cubeTexture) {
        skyboxTexture = assetManager.getCubeTexture(cubeTexture);
    }


    // Particle methods

    public Matrix4f computeViewMatrix () {
        Quaternionf conj = new Quaternionf(cameraTransform.orientation).conjugate();
        Matrix4f view = new Matrix4f()
                .rotate(conj)
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

    public void drawParticles () {
        var genericEmitters = ECS.findEntitiesWith(GenericParticleEmitter.class);
        var bubbleEmitters = ECS.findEntitiesWith(BubbleParticleEmitter.class);

        glDepthMask(false);
        glEnable(GL_BLEND);

        viewMatrix = computeViewMatrix();
        viewportMatrix = computeViewportMatrix(viewMatrix);

        for (int i = 0; i < numOfEmitterTypes; i++) {
            if (i == 0) {
                for (var iter = genericEmitters.iterator(); iter.hasNext() != false;) {
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
            }
            else if (i == 1) {
                for (var iter = bubbleEmitters.iterator(); iter.hasNext() != false;) {
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
            }
        }

        glDepthFunc(GL_LESS);
        glDisable(GL_BLEND);
        glDepthMask(true);
    }

    // Generic particle emitters
    public void setGenericParticleEmitters (GenericParticleEmitter[] particleEmitters) {
        this.genericParticleEmitters = particleEmitters;
    }
    public GenericParticleEmitter[] getGenericParticleEmitters () {
        return genericParticleEmitters;
    }
    // Bubble particle emiiters
    public void setBubbleParticleEmitters (BubbleParticleEmitter[] particleEmitters) {
        this.bubbleParticleEmitters = particleEmitters;
    }
    public BubbleParticleEmitter[] getBubbleParticleEmitters () {
        return bubbleParticleEmitters;
    }

    public Entity createParticleEmitter (EmitterType emitterType, Integer meshID, Material material, float initScale, float scale, int maxParticles, long creationPeriodMillis, Vector3f velocity, long ttl, int blendFunction) {
        Mesh mesh = assetManager.getMesh(meshID);

        Transform transform = new Transform(new Matrix4f());

        switch (emitterType) {
            case GENERIC: {
                Particle baseParticle = new Particle(mesh, material.shader, material.albedoMap, transform.model, velocity, ttl, initScale, scale, blendFunction);
                GenericParticleEmitter particleEmitter = new GenericParticleEmitter(baseParticle, maxParticles, creationPeriodMillis);
                return ECS.createEntity(particleEmitter);
            }
            case BUBBLE: {
                BubbleParticleEmitter particleEmitter = new BubbleParticleEmitter(mesh, material.shader, material.albedoMap, transform.model, ttl, velocity, initScale, scale, blendFunction, maxParticles, creationPeriodMillis);
                return ECS.createEntity(particleEmitter);
            }
            default: {
                Particle baseParticle = new Particle(mesh, material.shader, material.albedoMap, transform.model, velocity, ttl, initScale, scale, blendFunction);
                GenericParticleEmitter particleEmitter = new GenericParticleEmitter(baseParticle, maxParticles, creationPeriodMillis);
                return ECS.createEntity(particleEmitter);
            }
        }
    }
}
