#version 430 core

in vec2 outTextCoord;

uniform sampler2D txtSampler

//writing to our texture instead of screen
layout(location = 0) out vec4 color;

void main() 
{
    color = vec3(1,1,0,1);
    //color = texture(txtSampler, outTextCoord);
}