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

void draw4Quads()
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

    //our displacement/derivative have none of their values == 0 but infuriatingly I haven't been able to make them visible no matter what i multiply by???
    //we might just need to try using them and pray it just works
    // if (color.r == 0)
    // {
    //     color.r = 1;
    // }

    // if (color.g == 0)
    // {
    //     color.g = 1;
    // }

    // if (color.b == 0.0)
    // {
    //     color.b = 1;
    // }

}

void texCompare()
{
    vec4 a = texture(Albedo, outTexCoord/2);
    vec4 b = texture(AmbientOcclusionMap, outTexCoord/2);
    vec4 c = texture(RoughnessMap, outTexCoord/2);
    vec4 d = texture(NormalMap, outTexCoord/2);

    color = vec4(0,0,0,1);

    if ((a.x == b.x) && (a.x == c.x) && (a.x == d.x))
    {
        color.x = 0.25;
    }
    if ((a.y == b.y) && (a.y == c.y) && (a.y == d.y))
    {
        color.y = 0.25;
    }
    if ((a.z == b.z) && (a.z == c.z) && (a.z == d.z))
    {
        color.z = 0.25;
    }
    if ((a.w == b.w) && (a.w == c.w) && (a.w == d.w))
    {
        color.x += 0.25;
        color.y += 0.25;
        color.z += 0.25;
    }
}

void main() 
{
    draw4Quads();

    //texCompare();
}