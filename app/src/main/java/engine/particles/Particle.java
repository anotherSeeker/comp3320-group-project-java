package engine.particles;

import engine.rendering.Mesh;
import engine.rendering.Shader;
import engine.rendering.Texture;
import engine.components.*;

import org.joml.Vector3f;
import org.joml.Matrix4f;

public class Particle extends GameObject {
    public Transform transform;
    public float scale;
    public float scaleFinal;
    public Vector3f velocity;
    public long ttl;
    public int blendFunction;

    public Particle (Mesh mesh, Shader shader, Texture texture, Matrix4f model, Vector3f velocity, long ttl, float scale, float scaleMax, int blendFunction) {
        super(mesh, new Material());
        this.material.shader = shader;
        this.material.albedoMap = texture;
        transform = new Transform(model);
        this.velocity = new Vector3f(velocity);
        this.scale = scale;
        this.scaleFinal = scaleMax;
        setScale(scale);
        this.ttl = ttl;
        this.blendFunction = blendFunction;
    }

    public Particle (Particle baseParticle) {
        super(baseParticle.mesh, baseParticle.material);
        this.transform = new Transform(baseParticle.transform.model);
        this.velocity = new Vector3f(baseParticle.velocity);
        this.scale = baseParticle.scale;
        this.scaleFinal = baseParticle.scaleFinal;
        this.ttl = baseParticle.ttl;
        this.blendFunction = baseParticle.blendFunction;
    }

    public Vector3f getVelocity () {
        return velocity;
    }

    public void setVelocity (Vector3f velocity) {
        this.velocity = velocity;
    }

    public void setScale () {
        transform.model.scale(this.scale);
    }

    public void setScale (float scale) {
        transform.model.scale(scale);
    }

    public long getTtl () {
        return ttl;
    }

    public void setTtl (long ttl) {
        this.ttl = ttl;
    }

    public long updateTtl (long elapsedTime) {
        this.ttl -= elapsedTime;
        return this.ttl;
    }
}
