package engine.particles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Vector3f;
import org.joml.Matrix4f;

import engine.components.GameObject;
import engine.Mesh;
import engine.Shader;
import engine.Texture;

public class BubbleParticleEmitter extends ParticleEmitter {
    
    public final List<Particle> particles;
    public final Particle baseParticle;
    public boolean grow = false;
    
    public BubbleParticleEmitter (Mesh mesh, Shader shader, Texture texture, Matrix4f model, long ttl, Vector3f velocity, float initScale, float scale, int blendFunction, int maxParticles, long creationPeriodMillis) {
        baseParticle = new Particle(mesh, shader, texture, model, new Vector3f(velocity), ttl, initScale, scale, blendFunction);
        particles = new ArrayList<Particle>();
        this.maxParticles = maxParticles;
        this.active = false;
        this.LastCreationTime = 0;
        this.creationPeriodMillis = creationPeriodMillis;
    }

    public void setGrow () {
        grow = (grow) ? false : true;
    }

    public List<Particle> getParticles () {
        return particles;
    }

    public Particle getBaseParticle () {
        return baseParticle;
    }

    public void update (long elapsedTime) {
        long now = System.currentTimeMillis();
        if (LastCreationTime == 0) {
            LastCreationTime = now;
        }
        Iterator<? extends GameObject> it = particles.iterator();
        while (it.hasNext()) {
            Particle particle = (Particle) it.next();
            if (particle.updateTtl(elapsedTime) < 0) {
                it.remove();
            }
            else {
                updatePosition(particle, elapsedTime);
            }
        }
        int length = this.getParticles().size();
        if (now - LastCreationTime >= this.creationPeriodMillis && length < maxParticles) {
            createParticle();
            this.LastCreationTime = now;
        }
    }

    public void updatePosition (Particle particle, long elapsedTime) {
        Vector3f velocity = particle.getVelocity();
        float delta = elapsedTime / 10000.0f;
        float dx = velocity.x * delta;
        float dy = velocity.y * delta;
        float dz = velocity.z * delta;
        float sign = 1.0f;
        Random rand = new Random();
        float stopSpeed = 0.0004f;

        if (velocity.x > 0.1f || velocity.x < -0.1f) {
            if (velocity.x > 0) {
                particle.velocity.x -= stopSpeed;
            }
            else if (velocity.x < 0) {
                particle.velocity.x += stopSpeed;
            }
        }
        else {
            sign = (rand.nextBoolean()) ? sign = 1.0f : -1.0f;
            particle.velocity.x += sign * ((0.02f) * Math.sin(rand.nextFloat(0.0f, (float)Math.PI)));
        }

        if (velocity.z > 0.1f || velocity.z < -0.1f) {
            if (velocity.z > 0) {
                particle.velocity.z -= stopSpeed;
            }
            else if (velocity.z < 0) {
                particle.velocity.z += stopSpeed;
            }
        }
        else {
            sign = (rand.nextBoolean()) ? sign = 1.0f : -1.0f;
            particle.velocity.z += sign * ((0.02f) * Math.sin(rand.nextFloat(0.0f, (float)Math.PI)));
        }

        if (velocity.y <= rand.nextFloat(0.5f, 0.8f)) {
            particle.velocity.y += 0.05f;
        }


        Vector3f currentPos = particle.transform.position;
        Vector3f pos = new Vector3f((currentPos.x + dx), (currentPos.y + dy), (currentPos.z + dz));
        particle.transform.setPosition(pos);

        if (grow) {
            if (particle.scale < particle.scaleFinal) {
                particle.scale += delta;
            }
        }
        else {
            if (particle.scale < particle.scaleFinal) {
                particle.scale = particle.scaleFinal;
            }
        }
    }

    private void createParticle () {
        Particle particle = new Particle(this.getBaseParticle());
        Random rand = new Random();

        float sign;
        Vector3f velocitySign = new Vector3f(0.0f);
        sign = (rand.nextFloat() > 0.5d) ? -1.0f : 1.0f;
        velocitySign.x = sign;
        sign = (rand.nextFloat() > 0.5d) ? -1.0f : 1.0f;
        velocitySign.z = sign;

        Vector3f velocityInc = new Vector3f(0.0f);
        velocityInc.x = velocitySign.x * (float) rand.nextFloat(velocityRndRangeMin, velocityRndRangeMax);
        velocityInc.y = (float) rand.nextFloat((particle.velocity.y - 0.5f), (particle.velocity.y + 0.5f));
        velocityInc.z = velocitySign.z * (float) rand.nextFloat(velocityRndRangeMin, velocityRndRangeMax);
        float scaleInc = (float) rand.nextFloat(scaleRndRangeMin, scaleRndRangeMax);
        particle.velocity.add(velocityInc);
        particle.scaleFinal = scaleInc;
        particles.add(particle);                                                          
    }
    
    @Override 
    public void cleanup () {
        for (Particle p : getParticles()) {
            p.mesh.delete();
        }
    }
}
