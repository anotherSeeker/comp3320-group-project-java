#version 430 core

const int MAX_WEIGHTS = 4;
const int MAX_BONES = 150;

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec3 aBitangent;
layout (location = 5) in vec4 boneWeights;
layout (location = 6) in ivec4 boneIndices;

uniform mat4 CameraMatrix;
uniform mat4 Model;
uniform mat4 bonesMatrices[MAX_BONES];

out vec3 position;
out vec3 normal;
out vec2 UV;
out vec3 tangent;
out vec3 bitangent;

out mat3 TBN;

void main() {
    vec4 initPos = vec4(0, 0, 0, 0);
    vec4 initNormal = vec4(0, 0, 0, 0);
    vec4 initTangent = vec4(0, 0, 0, 0);
    vec4 initBitangent = vec4(0, 0, 0, 0);

    int count = 0;
    for (int i = 0; i < MAX_WEIGHTS; i++) {
        float weight = boneWeights[i];
        if (weight > 0) {
            count++;
            int boneIndex = boneIndices[i];
            vec4 tmpPos = bonesMatrices[boneIndex] * vec4(aPos, 1.0);
            initPos += weight * tmpPos;

            vec4 tmpNormal = bonesMatrices[boneIndex] * vec4(aNormal, 0.0);
            initNormal += weight * tmpNormal;

            vec4 tmpTangent = bonesMatrices[boneIndex] * vec4(aTangent, 0.0);
            initTangent += weight * tmpTangent;

            vec4 tmpBitangent = bonesMatrices[boneIndex] * vec4(aBitangent, 0.0);
            initBitangent += weight * tmpBitangent;
        }
    }
    if (count == 0) {
        initPos = vec4(aPos, 1.0);
        initNormal = vec4(aNormal, 0.0);
        initTangent = vec4(aTangent, 0.0);
        initBitangent = vec4(aBitangent, 0.0);
    }

    gl_Position = CameraMatrix * Model * vec4(aPos,1);

    position = ((Model * vec4(aPos,1))* initPos).xyz;
    UV = aUV;
    normal = normalize((mat3(transpose(inverse(Model))) * aNormal)* initNormal);
    tangent = normalize((mat3(Model) * aTangent)* initTangent);
    bitangent = normalize((mat3(Model) * aBitangent)* initBitangent);

    TBN = mat3(tangent,bitangent,normal);
}