#version 430 core

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec3 aBitangent;

uniform mat4 CameraMatrix;
uniform mat4 Model;

out vec3 position;
out vec3 normal;
out vec2 UV;
out vec3 tangent;
out vec3 bitangent;

out mat3 TBN;

void main() {
    gl_Position = CameraMatrix * Model * vec4(aPos,1);

    position = (Model * vec4(aPos,1)).xyz;
    UV = aUV;
    normal = normalize(mat3(transpose(inverse(Model))) * aNormal);
    tangent = normalize(mat3(Model) * aTangent);
    bitangent = normalize(mat3(Model) * aBitangent);

    TBN = mat3(tangent,bitangent,normal);
}