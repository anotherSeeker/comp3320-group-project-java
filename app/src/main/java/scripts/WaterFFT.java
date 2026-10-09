package scripts;

import org.joml.Vector2f;

import engine.*;

public class WaterFFT extends MonoScript {
    public WaterFFT() {
        
    }

    private class WaveData 
    {
        float frequency;
        float amplitude;
        Vector2f direction;
        float speed;
        float steepness;

        public WaveData(float frequency, float amplitude, Vector2f direction, float speed, float steepness) 
        {
            this.frequency = frequency;
            this.amplitude = amplitude;
            this.direction = direction;
            this.speed = speed;
            this.steepness = steepness;
        }
    }

    @Override
    public void onStart() {
        // Initialization code here
    }

    @Override
    public void onUpdate() {
        // Update logic here
    }

    @Override
    public void onStop() {
        // Cleanup code here
    }
}
