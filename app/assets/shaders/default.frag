#version 430 core
#define PI 3.14159265359

in vec3 position;
in vec3 normal;
in vec2 UV;

out vec4 fragColour;

uniform sampler2D Albedo;
uniform vec3 LightDirection;
uniform vec3 ViewPosition;

const float ambient = 0.2;
const float specularStrength = 0.5;
const float specularSharpness = 32;

const float roughness = 0.2;
const float subsurface = 0.7;
const float sheen = 1;

const vec3 sheenColour = vec3(1,1,1);
const vec3 diffuseColour = vec3(1,1,1);

float schlickFresnel(float u)
{
    float m = clamp(1-u, 0, 1);
    float m2 = m * m;
    return m2 * m2 * m;
}

vec3 burleyDiffuse(float nDotL,float nDotV,float lDotH){
    float fresnelLight = schlickFresnel(nDotL);
    float fresnelView = schlickFresnel(nDotV);

    float fd90 = 0.5 + 2 * lDotH * lDotH * roughness;

    float diffuse = mix(1.0,fd90,fresnelLight) * mix(1.0,fd90,fresnelView);

    float fresnelSubSurface90 = lDotH * lDotH * roughness;
    float fresnelSubSurface = mix(1.0,fresnelSubSurface90,fresnelLight) * mix(1.0,fresnelSubSurface90,fresnelView);

    float ss = 1.25 * (fresnelSubSurface * (1 / (nDotL + nDotV) - 0.5) + 0.5);

    float fresnelSheen = schlickFresnel(lDotH) * sheen * sheenColour;

    return mix(diffuse,ss,subsurface) + fresnelSheen;
}

vec3 brdf(){
    vec4 baseColour = texture(Albedo,UV);

    vec3 norm = normalize(normal);
    vec3 lightDirection = normalize(LightDirection);
    vec3 viewDirection = normalize(ViewPosition - position);
    vec3 halfway = normalize(lightDirection + viewDirection);

    float nDotL = dot(norm,lightDirection);
    float nDotV = dot(norm,viewDirection);

    if(nDotL < 0 || nDotV < 0) return vec3(0.0f);

    nDotL = max(nDotL,0.0f);
    nDotV = max(nDotV,0.0f);

    float nDotH = max(dot(norm,halfway),0.0f);
    float lDotH = max(dot(lightDirection,halfway),0.0f);

    vec3 diffuse = burleyDiffuse(nDotL,nDotV,lDotH) * diffuseColour / PI;
    float specular = pow(nDotH,specularSharpness) * specularStrength;

    vec3 lighting = (diffuse + ambient ) * baseColour.rgb + specular;

    return lighting * nDotL;
}

void main() {
    fragColour = vec4(brdf(),1);
}