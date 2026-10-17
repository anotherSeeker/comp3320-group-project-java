package scripts;

import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.*;
import engine.particles.*;

public class ParticlesDemoScene extends MonoScript {
    Integer particleShaderID = Assets.LoadShader(
            Path.of("assets/shaders/particles/particleShader.vert"),
            Path.of("assets/shaders/particles/particleShader.frag")
    );

    Integer particleID = Assets.LoadMesh(Mesh.Quad(), "particle_mesh");

    Material particle1Material = Assets.createMaterial(
            particleShaderID,
            Assets.LoadTexture(Path.of("assets/textures/particles/particle.png"))
    );

    Material particle2Material = Assets.createMaterial(
            particleShaderID,
            Assets.LoadTexture(Path.of("assets/textures/particles/particle_tmp.png"))
    );

    Entity P1;
    Entity P2;

    @Override
    protected void Start() {

        Vector3f P1_particleVelocity = new Vector3f(0.0f, 1.5f, 0.0f);
        long P1_ttl = 20000;
        int P1_maxParticles = 50;
        long P1_creationPeriodMillis = 300;
        float P1_range = 0.2f;
        float P1_scale = 0.3f;
        int P1_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        Vector3f P2_particleVelocity = new Vector3f(0.0f, 1.0f, 0.0f);
        long P2_ttl = 20000;
        int P2_maxParticles = 15;
        long P2_creationPeriodMillis = 300;
        float P2_range = 0.3f;
        float P2_scale = 0.1f;
        int P2_blendFunction = GL_ONE_MINUS_SRC_ALPHA;

        P1 = Scene.createParticleEmitter(particleID, particle1Material, P1_scale, P1_maxParticles, P1_creationPeriodMillis, P1_particleVelocity, P1_ttl, P1_blendFunction);

        P1.get(ExampleParticleEmitter.class).setActive(true);
        P1.get(ExampleParticleEmitter.class).setAllRndRange(P1_range);

        P1.get(ExampleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(2f, 0, 0));


        P2 = Scene.createParticleEmitter(particleID, particle2Material, P2_scale, P2_maxParticles, P2_creationPeriodMillis, P2_particleVelocity, P2_ttl, P2_blendFunction);

        P2.get(ExampleParticleEmitter.class).setActive(true);
        P2.get(ExampleParticleEmitter.class).setAllRndRange(P2_range);

        P2.get(ExampleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(-2f, 0, 0));


        Scene.setParticleEmitters(new ExampleParticleEmitter[] {P1.get(ExampleParticleEmitter.class), P2.get(ExampleParticleEmitter.class)});
    }

    @Override
    protected void Update(double deltaTime) {
        for (ExampleParticleEmitter PE : Scene.getParticleEmitters()) {
            PE.update((long)(deltaTime * 10000));
        }
    }
}
