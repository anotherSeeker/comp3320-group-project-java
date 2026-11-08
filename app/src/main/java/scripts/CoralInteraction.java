package scripts;

import engine.components.Transform;
import org.joml.Vector3f;

/** Tracks a real character transform; independent of camera and keyboard controls. */
public final class CoralInteraction {
    private Transform target;
    private final Vector3f previous = new Vector3f();
    public final Vector3f position = new Vector3f(1000);
    public final Vector3f wakePosition = new Vector3f(1000);
    public final Vector3f velocity = new Vector3f();

    public void follow(Transform character) {
        target = character;
        velocity.zero();
        if (target == null) position.set(1000);
        else target.model.getTranslation(position);
        previous.set(position);
        wakePosition.set(position);
    }
    public boolean active() { return target != null; }
    public void update(double deltaTime) {
        if (target == null) return;
        target.model.getTranslation(position);
        float dt = (float)Math.clamp(deltaTime, 0, .1);
        if (dt <= 0) return;
        Vector3f step = new Vector3f(position).sub(previous);
        // A teleport should not send a huge impulse through the coral.
        if (step.lengthSquared() > 9) {
            velocity.zero(); wakePosition.set(position);
        } else {
            Vector3f measured = step.div(dt);
            if (measured.lengthSquared() > 36) measured.normalize(6);
            velocity.lerp(measured, (float)(1 - Math.exp(-dt * 5)));
            wakePosition.lerp(position, (float)(1 - Math.exp(-dt * 4)));
        }
        previous.set(position);
    }
}
