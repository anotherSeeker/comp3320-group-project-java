package scripts.Animation;


import org.joml.Matrix4f;
import engine.components.Material;
import engine.components.Transform;

import java.util.*;

public class Model {

    private List<Animation> animationList;
    private List<Transform> entitiesList;
    private List<Material> materialList = new ArrayList<>();

    public Material getMaterial(Integer assetID) {
        return materialList.get(assetID);
    }

    public void LoadMaterial(Material material){

        materialList.add(material);
    }

    public Model(List<Material> materialList, List<Animation> animationList) {
        entitiesList = new ArrayList<>();
        this.materialList = materialList;
        this.animationList = animationList;
    }

    public List<Animation> getAnimationList() {
        return animationList;
    }

    public List<Transform> getEntitiesList() {
        return entitiesList;
    }

    public List<Material> getmaterialList() {
        return materialList;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {
    }

    public record Animation(String name, double duration, List<AnimatedFrame> frames) {
    }
}
