#version 430 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec3 aBitangent;



out VS_OUT {
    vec3 position;
    vec3 normal;
    vec2 UV;
    vec3 tangent;
    vec3 bitangent;
} vs_out;

out mat3 TBN;

void main() {

    vs_out.position = aPos;
    vs_out.normal = aNormal;
    vs_out.UV = aUV;
    vs_out.tangent = aTangent;
    vs_out.bitangent = aBitangent;

    // float height = texture(DisplacementMap,aUV).r;
    
    // vec3 displaced = aPos + aNormal * height * 0.1;
    
    // gl_Position = CameraMatrix * Model * vec4(displaced,1);
    // position = (Model * vec4(displaced,1)).xyz;


    // UV = aUV;
    // normal = normalize(mat3(transpose(inverse(Model))) * aNormal);
    // tangent = normalize(mat3(Model) * aTangent);
    // bitangent = normalize(mat3(Model) * aBitangent);

    // TBN = mat3(tangent,bitangent,normal);
}