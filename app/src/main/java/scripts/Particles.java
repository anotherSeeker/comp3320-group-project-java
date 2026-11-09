package scripts;

import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.glfw.GLFW.*;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.rendering.*;
import engine.components.*;
import engine.particles.*;

public class Particles extends MonoScript {
    private final int toggleKey;
    private boolean toggleHeld;

    public Particles() { this(GLFW_KEY_P); }
    public Particles(int toggleKey) { this.toggleKey = toggleKey; }

    Integer particleShaderID = Assets.LoadShader(
            Path.of("assets/shaders/particles/particleShader.vert"),
            Path.of("assets/shaders/particles/particleShader.frag")
    );

    Integer particleID = Assets.LoadMesh(Mesh.Quad(), "particle_mesh");

    Material bubbleMaterial = Assets.createMaterial(
            particleShaderID,
            Assets.LoadTexture(Path.of("assets/textures/particles/bubble.png"))
            
    );

    Entity P1;
    Entity P2;
    Entity P3;
    Entity P4;

    @Override
    protected void Start() {
        Random rand = new Random();

        boolean startEnabled = true;

        Vector3f P1_particleVelocity = new Vector3f(0.0f, 0.1f, 0.0f);
        long P1_ttl = 50000;
        int P1_maxParticles = 20;
        long P1_creationPeriodMillis = 2000;
        float P1_velocity_Min = 0.4f;
        float P1_velocity_Max = 0.5f;
        float P1_scale = 0.0f;
        float P1_scale_Min = 0.2f;
        float P1_scale_Max = 0.5f;
        int P1_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        P1 = Scene.createParticleEmitter(SceneGraph.EmitterType.BUBBLE, particleID, bubbleMaterial, P1_scale, P1_scale_Max, P1_maxParticles, P1_creationPeriodMillis, P1_particleVelocity, P1_ttl, P1_blendFunction);

        P1.get(BubbleParticleEmitter.class).setVelocityRndRange(P1_velocity_Min, P1_velocity_Max);
        P1.get(BubbleParticleEmitter.class).setScaleRndRange(P1_scale_Min, P1_scale_Max);
        P1.get(BubbleParticleEmitter.class).setGrow();

        P1.get(BubbleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(-5.0f, 0.5f, -11.0f));


        Vector3f P2_particleVelocity = new Vector3f((float) -(rand.nextFloat() + 0.1), 0.1f, (float) (rand.nextFloat() + 0.1));
        long P2_ttl = 100000;
        int P2_maxParticles = 50;
        long P2_creationPeriodMillis = 500;
        float P2_velocity_Min = 0.2f;
        float P2_velocity_Max = 0.5f;
        float P2_scale = 0.01f;
        float P2_scale_Min = 0.05f;
        float P2_scale_Max = 0.15f;
        int P2_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        P2 = Scene.createParticleEmitter(SceneGraph.EmitterType.BUBBLE, particleID, bubbleMaterial, P2_scale, P2_scale_Max, P2_maxParticles, P2_creationPeriodMillis, P2_particleVelocity, P2_ttl, P2_blendFunction);

        P2.get(BubbleParticleEmitter.class).setVelocityRndRange(P2_velocity_Min, P2_velocity_Max);
        P2.get(BubbleParticleEmitter.class).setScaleRndRange(P2_scale_Min, P2_scale_Max);

        P2.get(BubbleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(-1.0f, 0, 11.0f));

        
        Vector3f P3_particleVelocity = new Vector3f((float) (rand.nextFloat() + 0.1), 0.1f, (float) (rand.nextFloat() + 0.1));
        long P3_ttl = 100000;
        int P3_maxParticles = 50;
        long P3_creationPeriodMillis = 500;
        float P3_velocity_Min = 0.2f;
        float P3_velocity_Max = 0.5f;
        float P3_scale = 0.01f;
        float P3_scale_Min = 0.05f;
        float P3_scale_Max = 0.15f;
        int P3_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        P3 = Scene.createParticleEmitter(SceneGraph.EmitterType.BUBBLE, particleID, bubbleMaterial, P3_scale, P3_scale_Max, P3_maxParticles, P3_creationPeriodMillis, P3_particleVelocity, P3_ttl, P3_blendFunction);

        P3.get(BubbleParticleEmitter.class).setVelocityRndRange(P3_velocity_Min, P3_velocity_Max);
        P3.get(BubbleParticleEmitter.class).setScaleRndRange(P3_scale_Min, P3_scale_Max);

        P3.get(BubbleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(6.0f, 0f, 11.0f));
        

        Vector3f P4_particleVelocity = new Vector3f(0.0f, 0.1f, 0.0f);
        long P4_ttl = 50000;
        int P4_maxParticles = 20;
        long P4_creationPeriodMillis = 2000;
        float P4_velocity_Min = 0.4f;
        float P4_velocity_Max = 0.5f;
        float P4_scale = 0.0f;
        float P4_scale_Min = 0.2f;
        float P4_scale_Max = 0.5f;
        int P4_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        P4 = Scene.createParticleEmitter(SceneGraph.EmitterType.BUBBLE, particleID, bubbleMaterial, P4_scale, P4_scale_Max, P4_maxParticles, P4_creationPeriodMillis, P4_particleVelocity, P4_ttl, P4_blendFunction);

        P4.get(BubbleParticleEmitter.class).setVelocityRndRange(P4_velocity_Min, P4_velocity_Max);
        P4.get(BubbleParticleEmitter.class).setScaleRndRange(P4_scale_Min, P4_scale_Max);
        P4.get(BubbleParticleEmitter.class).setGrow();

        P4.get(BubbleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(-12.0f, 0.5f, 7.5f));

        if (startEnabled) {
            P1.get(BubbleParticleEmitter.class).setActive();
            P2.get(BubbleParticleEmitter.class).setActive();
            P3.get(BubbleParticleEmitter.class).setActive();
            P4.get(BubbleParticleEmitter.class).setActive();
        }

        Scene.setBubbleParticleEmitters(new BubbleParticleEmitter[] {P1.get(BubbleParticleEmitter.class), 
                                                                     P2.get(BubbleParticleEmitter.class), 
                                                                     P3.get(BubbleParticleEmitter.class),
                                                                     P4.get(BubbleParticleEmitter.class)});
    }

    @Override
    protected void Update(double deltaTime) {
        boolean pressed = Input.IsKeyDown(toggleKey);
        if (pressed && !toggleHeld) {
            App.toggleParticles();
            if (Scene.getGenericParticleEmitters() != null) {
                for (GenericParticleEmitter PE : Scene.getGenericParticleEmitters()) {
                    PE.setActive();
                }
            }
            if (Scene.getBubbleParticleEmitters() != null) {
                for (BubbleParticleEmitter PE : Scene.getBubbleParticleEmitters()) {
                    PE.setActive();
                }
            }
        }

        toggleHeld = pressed;
        if (deltaTime <= 0) return;

        if (Scene.getGenericParticleEmitters() != null) {
            for (GenericParticleEmitter PE : Scene.getGenericParticleEmitters()) {
                if (PE.active || !PE.getParticles().isEmpty()) {
                    PE.update((long)(deltaTime * 10000));
                }
            }
        }
        if (Scene.getBubbleParticleEmitters() != null) {
            for (BubbleParticleEmitter PE : Scene.getBubbleParticleEmitters()) {
                if (PE.active || !PE.getParticles().isEmpty()) {
                    PE.update((long)(deltaTime * 10000));
                }
            }
        }
    }
}
