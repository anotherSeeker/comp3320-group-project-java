package engine.particles;

import java.util.List;

public abstract class ParticleEmitter {
    public List<Particle> particles;
    public Particle baseParticle;
    public int maxParticles;
    public boolean active;
    public long creationPeriodMillis;
    public long LastCreationTime;
    public float velocityRndRangeMin;
    public float velocityRndRangeMax;
    public float positionRndRange;
    public float scaleRndRangeMin;
    public float scaleRndRangeMax;

    

    public void setMaxParticles (int maxParticles) {
        this.maxParticles = maxParticles;
    }

    public void setActive (boolean active) {
        this.active = active;
    }

    public void setCreationPeriodMillis (long creationPeriodMillis) {
        this.creationPeriodMillis = creationPeriodMillis;
    }

    public void setAllRndRange (float rndRangeMin, float rndRangeMax) {
        this.velocityRndRangeMin = rndRangeMin;
        this.velocityRndRangeMax = rndRangeMax;
        this.positionRndRange = rndRangeMax;
        this.scaleRndRangeMin = rndRangeMin;
        this.scaleRndRangeMax = rndRangeMax;
    }

    public void setVelocityRndRange (float velocityRndRangeMin, float velocityRndRangeMax) {
        this.velocityRndRangeMin = velocityRndRangeMin;
        this.velocityRndRangeMax = velocityRndRangeMax;
    }

    public void setPositionRndRange (float positionRndRange) {
        this.positionRndRange = positionRndRange;
    }

    public void setScaleRndRange (float scaleRndRangeMin, float scaleRndRangeMax) {
        this.scaleRndRangeMin = scaleRndRangeMin;
        this.scaleRndRangeMax = scaleRndRangeMax;
    }

    public void update () {};

    void cleanup () {};
}
