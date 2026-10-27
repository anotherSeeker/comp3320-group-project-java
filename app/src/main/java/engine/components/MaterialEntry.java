package engine.components;

public class MaterialEntry {
    public Integer shaderID = null;
    public Integer albedoID = null;
    public Integer roughnessID = null;
    public Integer ambientOcclusionID = null;
    public Integer normalID = null;
    public Integer metallicID = null;

    public MaterialEntry add(String string, Integer ID) {

        //@formatter:off
        switch (string) {
            case "SHADER": shaderID = ID; break;
            case "COLOUR": albedoID = ID; break;
            case "ROUGHNESS": roughnessID = ID; break;
            case "AMBIENT_OCCLUSION": ambientOcclusionID = ID; break;
            case "NORMAL": normalID = ID; break;
            case "METAL": metallicID = ID; break;

            default: break;
        }
        //@formatter:on

        return this;
    }

}
