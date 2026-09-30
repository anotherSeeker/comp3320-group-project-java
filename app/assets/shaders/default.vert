#version 430 core
layout (location = 0) in vec3 aPos;

uniform mat4 CameraMatrix;
uniform mat4 Model;

void main() {
    gl_Position = CameraMatrix * Model * vec4(aPos,1);
}