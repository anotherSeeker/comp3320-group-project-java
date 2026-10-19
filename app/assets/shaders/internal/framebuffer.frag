#version 430 core

out vec4 fragColour;
in vec2 UV;

uniform sampler2D screenTexture;

void main(){
    fragColour = texture(screenTexture,UV);
}