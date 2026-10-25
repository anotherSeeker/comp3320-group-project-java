#version 430 core

in vec2 outTexCoord;

uniform sampler2D Albedo;
uniform sampler2D RoughnessMap;
uniform sampler2D AmbientOcclusionMap;

layout(location = 0) out vec4 color;

float negativeComponent = 0.0f;

void main() 
{
    if (outTexCoord.x < 1)
    {
        if (outTexCoord.y < 1)
            color = texture(Albedo, outTexCoord);
        else
            color = texture(RoughnessMap, outTexCoord);
    }
    else
    {
        if (outTexCoord.y < 1)
            color = vec4(0.1,0.1,0.1,1);
        else
            color = texture(AmbientOcclusionMap, outTexCoord);
    }

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