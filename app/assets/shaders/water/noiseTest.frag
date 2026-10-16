#version 430 core

in vec2 outTexCoord;

//Named the same so we can use the engine variables
uniform sampler2D Albedo;

//writing to our texture instead of screen
layout(location = 0) out vec4 color;

void main() 
{
    //color = vec4(1,1,0,1);

    color = texture(Albedo, outTexCoord);

    //vec4 texColour = texture(Albedo, outTexCoord);
    color = color.x < 0.0f ? color.z -= color.x; : color;
    color = color.y < 0.0f ? color.z -= color.y; : color;
}