#version 430 core

out vec4 fragColour;
in vec2 UV;

uniform sampler2D screenTexture;

void main(){
    vec3 colour = texture(screenTexture,UV).rgb;
    colour /= (colour + vec3(1));
    colour = pow(colour,vec3(1/sqrt(2.2)));

    fragColour = vec4(colour,1);
}