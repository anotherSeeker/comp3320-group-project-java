#version 430 core

in vec2 UV;

uniform sampler2D Albedo;

out vec4 fragColour;

void main () {
    fragColour = texture(Albedo, UV) * vec4(1.0, 1.0, 1.0, 0.75);
}