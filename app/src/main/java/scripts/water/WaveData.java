package scripts.water;

import org.joml.Vector2f;

//old stuff, used for sum of sines but we're using wave spectra now

public class WaveData 
{
    float frequency;
    float amplitude;
    Vector2f direction;
    float speed;
    float steepness;
    Vector2f windDirection;
    float windStrength;

    public WaveData(float frequency, float amplitude, Vector2f direction, float speed, float steepness, Vector2f windDirection, float windStrength) 
    {
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.direction = direction;
        this.speed = speed;
        this.steepness = steepness;
        this.windDirection = windDirection;
        this.windStrength = windStrength;
    }

    public WaveData()
    {
        this.frequency = 0.0f;
        this.amplitude = 0.0f;
        this.direction = new Vector2f(0.0f, 0.0f);
        this.speed = 0.0f;
        this.steepness = 0.0f;
        this.windDirection = new Vector2f(0.0f, 0.0f);
        this.windStrength = 0.0f;
    }

    public float getFrequency() {
        return frequency;
    }
    public float getAmplitude() {
            return amplitude;
    }
    public Vector2f getDirection() {
        return direction;
    }
    public float getSpeed() {
        return speed;
    }
    public float getSteepness() {
        return steepness;
    }
    public Vector2f getWindDirection() {
        return windDirection;
    }
    public float getWindStrength() {
        return windStrength;
    }
}
