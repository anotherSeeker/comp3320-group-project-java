package engine.particles;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.joml.Vector3f;

import engine.components.GameObject;

public class GenericParticleEmitter extends ParticleEmitter {
    
    public final List<Particle> particles;
    public final Particle baseParticle;
    
    public GenericParticleEmitter (Particle baseParticle, int maxParticles, long creationPeriodMillis) {
        particles = new ArrayList<Particle>();
        this.baseParticle = baseParticle;
        this.maxParticles = maxParticles;
        this.active = false;
        this.LastCreationTime = 0;
        this.creationPeriodMillis = creationPeriodMillis;
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
            if (particle.updateTtl(elapsedTime) < 0 || !active) {
                it.remove();
            }
            else {
                updatePosition(particle, elapsedTime);
            }
        }
        int length = this.getParticles().size();
        if (now - LastCreationTime >= this.creationPeriodMillis && length < maxParticles && active) {
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
        Vector3f currentPos = particle.transform.position;
        Vector3f pos = new Vector3f((currentPos.x + dx), (currentPos.y + dy), (currentPos.z + dz));
        particle.transform.setPosition(pos);
    }

    private void createParticle () {
        Random rand = new Random();
        Particle particle = new Particle(this.getBaseParticle());

        // can add randomness here
        float sign;
        if (rand.nextFloat() > 0.5d) {
            sign = -1.0f;
        }
        else {
            sign = 1.0f;
        }
        float velocityInc = sign * (float) rand.nextFloat(velocityRndRangeMin, velocityRndRangeMax);
        float positionInc = sign * (float) rand.nextFloat() * this.positionRndRange;
        float scaleInc = (float) rand.nextFloat(scaleRndRangeMin, scaleRndRangeMax);
        particle.transform.position.add(positionInc, positionInc, positionInc);
        particle.velocity.add(velocityInc, velocityInc, velocityInc);
        particle.scale = scaleInc;
        particles.add(particle);                                                          
    }
    
    @Override 
    public void cleanup () {
        for (Particle p : getParticles()) {
            p.mesh.delete();
        }
    }
}
