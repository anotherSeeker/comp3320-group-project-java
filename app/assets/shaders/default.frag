#version 430 core

in vec3 normal;
in vec2 UV;

out vec4 fragColour;

uniform sampler2D Albedo;

void main() {
    vec3 colour = texture(Albedo,UV);

    fragColour = vec4(colour,1);
}