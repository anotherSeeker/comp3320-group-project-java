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
}