#version 430 core

in vec2 outTexCoord;

//noiseTex
uniform sampler2D Albedo;
//waveVecTex
uniform sampler2D RoughnessMap;
//h0Tex
uniform sampler2D AmbientOcclusionMap;
//dyTex
uniform sampler2D NormalMap;

layout(location = 0) out vec4 color;

void main() 
{
    if (outTexCoord.x < 1)
    {
        if (outTexCoord.y < 1)
            color = texture(Albedo, outTexCoord);
        else
            color = texture(NormalMap, outTexCoord);
    }
    else
    {
        if (outTexCoord.y < 1)
            color = texture(RoughnessMap, outTexCoord);
        else
            color = texture(AmbientOcclusionMap, outTexCoord);
    }
}