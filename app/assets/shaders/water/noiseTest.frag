#version 430 core

in vec2 outTexCoord;

uniform sampler2D Albedo;

//writing to our texture instead of screen
layout(location = 0) out vec4 color;

float negativeComponent = 0.0f;

void main() 
{
    color = texture(Albedo, outTexCoord);

    // if (color.x < 0)
    // {
    //     negativeComponent += 0.5;
    //     color = vec4(-(color.x/2),color.y,negativeComponent,1);
    // }

    // if (color.y < 0)
    // {
    //     negativeComponent += 0.5;
    //     color = vec4(color.x,-(color.y/2),negativeComponent,1);
    // }
}