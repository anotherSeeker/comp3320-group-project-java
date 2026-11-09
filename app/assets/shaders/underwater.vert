#version 430 core
// underwater.vert - vertex shader for underwater.frag (Tanz)
// Same outputs as the team's tessellation stage (default.tese), without the
// tessellation, so it can be loaded with Assets.LoadShader(vert, frag).

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec3 aBitangent;

uniform mat4 CameraMatrix;   // sent by the engine
uniform mat4 Model;          // sent by the engine

out vec3 position;   // in the world
out vec3 normal;
out vec2 UV;
out vec3 tangent;
out vec3 bitangent;

out mat3 TBN;

void main() {
    vec4 worldPosition = Model * vec4(aPos, 1.0);
    position = worldPosition.xyz;

    normal = normalize(mat3(transpose(inverse(Model))) * aNormal);
    tangent = mat3(Model) * aTangent;
    bitangent = mat3(Model) * aBitangent;
    UV = aUV;

    TBN = mat3(tangent, bitangent, normal);

    gl_Position = CameraMatrix * worldPosition;
}
