#version 430 core

in vec2 outTexCoord;

//ID1
uniform sampler2D Albedo;
//ID2
uniform sampler2D RoughnessMap;
//ID3
uniform sampler2D AmbientOcclusionMap;
//ID4
uniform sampler2D NormalMap;

layout(location = 0) out vec4 color;

void main() 
{
    if (outTexCoord.x < 1)
    {
        if (outTexCoord.y > 1)
            color = texture(Albedo, outTexCoord);
        else
            color = texture(AmbientOcclusionMap, outTexCoord);
    }
    else
    {
        if (outTexCoord.y > 1)
            color = texture(RoughnessMap, outTexCoord);
        else
            color = texture(NormalMap, outTexCoord);
    }

    //proving to myself that we are actually doing something with the fourier components, and we are
    if (color.r == 0 && color.g == 0 && color.b == 0)
    {
        if (color.a == 0)
            color = vec4(1,0.25,1,1);
        else
            color = vec4(0.5,0.5,0.5,1);
    }

    // if (color.r == color.b)
    // {
    //     color = vec4(1,0.5,1,1);
    // }

    // if (-color.g == color.a)
    // {
    //     color = vec4(0.5,1,1,1);
    // }
}