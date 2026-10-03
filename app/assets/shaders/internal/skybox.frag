#version 430 core
out vec4 FragColor;

in vec3 TexCoords;

uniform samplerCube Skybox;

void main()
{    
    vec4 colour = texture(Skybox, TexCoords);

    // since colours are in HDR
    vec4 toneMapped = colour / (colour + vec4(1));

    vec4 gammaCorrected = pow(colour,vec4(1/2.2));

    FragColor = toneMapped;
}