package engine.components;

public class MaterialEntry {
    public Integer shaderID = null;
    public Integer albedoID = null;
    public Integer roughnessID = null;
    public Integer ambientOcclusionID = null;
    public Integer normalID = null;
    public Integer metallicID = null;

    public MaterialEntry add(String string, Integer ID) {

        switch (string) {
            case "SHADER":
                shaderID = ID;

            case "COLOUR":
                albedoID = ID;
            case "ROUGHNESS":
                roughnessID = ID;
            case "AMBIENT_OCCLUSION":
                ambientOcclusionID = ID;
            case "NORMAL":
                normalID = ID;
            case "METAL":
                metallicID = ID;

            default:
                break;
        }

        return this;
    }

}
