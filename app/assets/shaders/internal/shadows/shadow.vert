#version 430 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aUV;
layout (location = 3) in vec3 aTangent;
layout (location = 4) in vec3 aBitangent;

uniform mat4 LightMatrix;
uniform mat4 Model;

void main() {
    gl_Position = LightMatrix * Model * vec4(aPos,0.0);
}