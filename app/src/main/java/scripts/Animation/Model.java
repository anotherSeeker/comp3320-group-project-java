package scripts.Animation;


import org.joml.Matrix4f;
import engine.*;
import engine.components.Material;

import java.util.*;

public class Model {

    private final String id;
    private List<Animation> animationList;
    private List<Entity> entitiesList;
    private List<Material> materialList = new ArrayList<>();

    public Material getMaterial(Integer assetID) {
        return materialList.get(assetID);
    }

    public void LoadMaterial(Material material){

        materialList.add(material);
    }

    public Model(String id, List<Material> materialList, List<Animation> animationList) {
        entitiesList = new ArrayList<>();
        this.id = id;
        this.materialList = materialList;
        this.animationList = animationList;
    }

    public List<Animation> getAnimationList() {
        return animationList;
    }

    public List<Entity> getEntitiesList() {
        return entitiesList;
    }

    public String getId() {
        return id;
    }

    public List<Material> getmaterialList() {
        return materialList;
    }

    public record AnimatedFrame(Matrix4f[] boneMatrices) {
    }

    public record Animation(String name, double duration, List<AnimatedFrame> frames) {
    }
}
