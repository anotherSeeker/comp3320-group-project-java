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
            color = vec4(0,0,1,1);//texture(RoughnessMap, outTexCoord);
        else
            color = vec4(1,1,1,1);//texture(AmbientOcclusionMap, outTexCoord);
    }
    else
    {
        if (outTexCoord.y < 1)
            color = vec4(1,0,0,1);//texture(Albedo, outTexCoord);
        else
            color = vec4(0,1,0,1);//texture(NormalMap, outTexCoord);
    }
}