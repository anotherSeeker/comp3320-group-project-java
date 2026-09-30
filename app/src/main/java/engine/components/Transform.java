package engine.components;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {
    public Matrix4f model;
    public Vector3f position = new Vector3f();
    public Quaternionf orientation = new Quaternionf();

    public Transform(Matrix4f model) {
        this.model = model;
        model.getTranslation(position);
        model.getNormalizedRotation(orientation);
    }

    public Transform(Vector3f position, Quaternionf orientation) {
        this.position = position;
        this.orientation = orientation;

        this.model = new Matrix4f().translationRotate(position, orientation);
    }
}
