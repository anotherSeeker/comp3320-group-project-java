package scripts;

import java.nio.file.Path;

import org.joml.Vector3f;

import dev.dominion.ecs.api.Entity;

import engine.*;
import engine.components.*;
import engine.particles.*;

public class ParticlesDemoScene extends MonoScript {
    public String pathPrefix = "app/";

    Integer particleShaderID = Assets.LoadShader(
            Path.of(pathPrefix + "assets/shaders/particles/particleShader.vert"),
            Path.of(pathPrefix + "assets/shaders/particles/particleShader.frag")
    );

    Integer shaderID = Assets.LoadShader(
            Path.of(pathPrefix + "assets/shaders/default.vert"),
            Path.of(pathPrefix + "assets/shaders/default.frag")
    );

    Integer sphereID = Assets.LoadMesh(Path.of(pathPrefix + "assets/meshes/uvsmooth.obj"));
    Integer particleID = Assets.LoadMesh(Mesh.Quad(), "particle_mesh");
    
    Material material = Assets.createMaterial(
            shaderID,
            Assets.LoadTexture(Path.of(pathPrefix + "assets/textures/Tiles/Tiles108_1K-JPG_Color.jpg")),
            Assets.LoadTexture(Path.of(pathPrefix + "assets/textures/Tiles/Tiles108_1K-JPG_Roughness.jpg")),
            Assets.LoadTexture(Path.of(pathPrefix + "assets/textures/Tiles/Tiles108_1K-JPG_AmbientOcclusion.jpg")),
            Assets.LoadTexture(Path.of(pathPrefix + "assets/textures/Tiles/Tiles108_1K-JPG_NormalGL.jpg"))
    );

    Material particleMaterial = Assets.createMaterial(
            particleShaderID,
            Assets.LoadTexture(Path.of(pathPrefix + "assets/textures/particles/particle.png"))
    );

    Entity P1;
    Entity P2;

    @Override
    protected void Start() {

        Vector3f particleVelocity = new Vector3f(0.0f, 1.5f, 0.0f);
        long ttl = 20000;
        int maxParticles = 50;
        long creationPeriodMillis = 300;
        float range = 0.2f;
        float scale = 0.1f;

        P1 = Scene.createParticleEmitter(particleID, particleMaterial, scale, maxParticles, creationPeriodMillis, particleVelocity, ttl);

        P1.get(ExampleParticleEmitter.class).setActive(true);
        P1.get(ExampleParticleEmitter.class).setAllRndRange(range);

        P1.get(ExampleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(1.5f, 0, 0));


        P2 = Scene.createParticleEmitter(particleID, particleMaterial, scale, maxParticles, creationPeriodMillis, particleVelocity, ttl);

        P2.get(ExampleParticleEmitter.class).setActive(true);
        P2.get(ExampleParticleEmitter.class).setAllRndRange(range);

        P2.get(ExampleParticleEmitter.class).baseParticle.transform.setPosition(new Vector3f(-1.5f, 0, 0));


        Scene.setParticleEmitters(new ExampleParticleEmitter[] {P1.get(ExampleParticleEmitter.class), P2.get(ExampleParticleEmitter.class)});

    }

    @Override
    protected void Update(double deltaTime) {
        //Scene.getParticleEmitters()[0].update((long)(deltaTime * 10000));
        for (ExampleParticleEmitter PE : Scene.getParticleEmitters()) {
            PE.update((long)(deltaTime * 10000));
        }
    }
}
