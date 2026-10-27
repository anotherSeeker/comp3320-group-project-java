package engine.components;

import engine.Shader;
import engine.Texture;
import org.joml.Vector3f;

public class Material {
    public float roughness = 1;
    public float ambientOcclusion = 1;
    public float metallic = 0;

    public float subsurface = 0;
    public float sheen = 0;
    public float annisotropy = 0.5f;
    public float specularStrength = 0.5f;

    public Vector3f sheenColour = new Vector3f(1, 1, 1);

    public Shader shader;
    public Texture albedoMap;
    public Texture roughnessMap;
    public Texture ambientOcclusionMap;
    public Texture normalMap;
    public Texture metallicMap;

    public void bindAsUniform() {
        this.shader.setTexture("Albedo", this.albedoMap, 4);
        this.shader.setTexture("RoughnessMap", this.roughnessMap, 5);
        this.shader.setTexture("AmbientOcclusionMap", this.ambientOcclusionMap, 6);
        this.shader.setTexture("NormalMap", this.normalMap, 7);
        this.shader.setTexture("MetallicMap", this.normalMap, 8);

        this.shader.setFloat("Roughness", this.roughness);
        this.shader.setFloat("AmbientOcclusion", this.ambientOcclusion);
        this.shader.setFloat("Metallic", this.metallic);

        this.shader.setFloat("Subsurface", this.subsurface);
        this.shader.setFloat("Sheen", this.sheen);
        this.shader.setFloat("Annisotropy", this.annisotropy);
        this.shader.setFloat("SpecularStrength", this.specularStrength);

        this.shader.setVector3("SheenColour", this.sheenColour);
    }
}
