package engine.particles;

import java.util.List;

public abstract class ParticleEmitter {
    public List<Particle> particles;
    public Particle baseParticle;
    public int maxParticles;
    public boolean active;
    public long creationPeriodMillis;
    public long LastCreationTime;
    public float velocityRndRange;
    public float positionRndRange;
    public float scaleRndRange;

    

    public void setMaxParticles (int maxParticles) {
        this.maxParticles = maxParticles;
    }

    public void setActive (boolean active) {
        this.active = active;
    }

    public void setCreationPeriodMillis (long creationPeriodMillis) {
        this.creationPeriodMillis = creationPeriodMillis;
    }

    public void setAllRndRange (float rndRange) {
        this.velocityRndRange = rndRange;
        this.positionRndRange = rndRange;
        this.scaleRndRange = rndRange;
    }

    public void setSpeedRndRange (float speedRndRange) {
        this.velocityRndRange = speedRndRange;
    }

    public void setPositionRndRange (float positionRndRange) {
        this.positionRndRange = positionRndRange;
    }

    public void setScaleRndRange (float scaleRndRange) {
        this.scaleRndRange = scaleRndRange;
    }

    public void update () {};

    void cleanup () {};
}
