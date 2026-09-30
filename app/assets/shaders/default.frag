#version 430 core

in vec3 normal;
in vec2 UV;

out vec4 fragColour;

void main() {
    fragColour = vec4(normal * 0.5 + 0.5,1);
}