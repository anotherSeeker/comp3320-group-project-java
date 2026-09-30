#version 430 core
layout (location = 0) in vec3 aPos;

uniform mat4 CameraMatrix;

void main() {
    gl_Position = CameraMatrix * vec4(aPos,1);
}