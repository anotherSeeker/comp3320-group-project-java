package scripts.Animation;

import org.joml.Matrix4f;

import java.util.*;

public class Model {

    private List<Animation> animationList;
    private int meshId;

    public int getMeshId() {
        return meshId;
    }

    public Model(int meshId, List<Animation> animationList) {
        this.animationList = animationList;
        this.meshId = meshId;
    }

    public List<Animation> getAnimationList() {
        return animationList;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {
    }

    public record Animation(String name, double duration, List<AnimatedFrame> frames) {
    }
}
