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

void main() {

    vs_out.position = aPos;
    vs_out.normal = aNormal;
    vs_out.UV = aUV;
    vs_out.tangent = aTangent;
    vs_out.bitangent = aBitangent;
}