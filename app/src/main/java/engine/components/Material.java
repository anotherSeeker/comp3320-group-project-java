package engine.components;

import engine.Shader;
import engine.Texture;

public class Material {
    public float roughness = 1;
    public float ambientOcclusion = 1;

    public Shader shader;
    public Texture albedoMap;
    public Texture roughnessMap;
    public Texture ambientOcclusionMap;
    public Texture normalMap;
}
