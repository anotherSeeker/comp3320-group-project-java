package engine.components;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import scripts.Animation.AnimationData;

public class Transform {
    public Matrix4f model;
    public Vector3f position = new Vector3f();
    public Quaternionf orientation = new Quaternionf();
    private AnimationData animationData;
    public float scale = 1.0f;

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

    public void setModel(Matrix4f model) {
        this.model = model;
        model.getTranslation(position);
        model.getNormalizedRotation(orientation);
    }

    public void setPosition(Vector3f position) {
        this.position = position;
        this.model = new Matrix4f().translationRotate(position, orientation);
    }

    public void setOrientation(Quaternionf orientation) {
        this.orientation = orientation;
        this.model = new Matrix4f().translationRotate(position, orientation);
    }

    public void setOrientationEuler(Vector3f eulerAngles) {
        this.orientation = new Quaternionf().rotationYXZ(eulerAngles.y, eulerAngles.x, eulerAngles.z);
        this.model = new Matrix4f().translationRotate(position, orientation);
    }

    public void setScale(float scale) {
        this.model.scale(1.0f / this.scale, 1.0f / this.scale, 1.0f / this.scale);

        this.scale = scale;
        this.model.scale(this.scale, this.scale, this.scale);
    }

    public void lookAt(Vector3f target) {
        Vector3f direction = target.sub(this.position).normalize();

        this.orientation.identity().lookAlong(direction, new Vector3f(0, 1, 0));

        model.translationRotate(this.position, this.orientation);
    }

    public AnimationData getAnimationData() {
        return animationData;
    }

    public void setAnimationData(AnimationData animationData) {
        this.animationData = animationData;
    }

}
