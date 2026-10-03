package engine.components;

import engine.Shader;
import engine.Texture;

public class Material {
    public float roughness = 1;

    public Shader shader;
    public Texture albedoMap;
    public Texture roughnessMap;
}
