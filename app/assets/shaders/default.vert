#version 430 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;

uniform mat4 CameraMatrix;
uniform mat4 Model;

out vec3 normal;
out vec2 UV;

void main() {
    gl_Position = CameraMatrix * Model * vec4(aPos,1);

    UV = aUV;
    normal = aNormal;
}