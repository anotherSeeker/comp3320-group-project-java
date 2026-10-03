#version 430 core
layout (location = 0) in vec3 aPos;

uniform mat4 Projection;
uniform mat4 View;

out vec3 worldPosition;

void main() {
    gl_Position = Projection * View * vec4(aPos,1);
    worldPosition = aPos;
}