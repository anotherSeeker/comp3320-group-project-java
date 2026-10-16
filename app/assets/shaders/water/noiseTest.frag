#version 430 core

in vec2 outTexCoord;

//Named the same so we can use the engine variables
uniform sampler2D Albedo;

//writing to our texture instead of screen
layout(location = 0) out vec4 color;

void main() 
{
    //color = vec3(1,1,0,1);
    color = texture(Albedo, outTexCoord);
}