#version 430 core

in vec3 position;
in vec3 normal;
in vec2 UV;

out vec4 fragColour;

uniform sampler2D Albedo;
uniform vec3 LightDirection;
uniform vec3 ViewPosition;

const float ambient = 0.2;
const float diffuseStrength = 0.8;
const float specularStrength = 0.5;
const float specularSharpness = 32;

const float roughness = 0.2;
const vec3 diffuseColour = vec3(0.39f, 0.58f, 0.93f);

float schlickFresnel(float u)
{
    float m = clamp(1-u, 0, 1);
    float m2 = m * m;
    return m2 * m2 * m;
}

float burleyDiffuse(float nDotL,float nDotV,float LDotH){
    float fresnelLight = schlickFresnel(nDotL);
    float fresnelView = schlickFresnel(nDotV);

    float fd90 = 0.5 + 2 * LDotH * LDotH * roughness;

    return mix(1.0,fd90,mix(1.0,fd90,fresnelLight)) * mix(1.0,fd90,mix(1.0,fd90,fresnelView));
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

    vec3 diffuse = burleyDiffuse(nDotL,nDotV,lDotH) * diffuseColour;
    float specular = pow(nDotH,specularSharpness) * specularStrength;

    vec3 lighting = (diffuse + ambient ) * baseColour.rgb + specular;

    return lighting;
}

void main() {
    fragColour = vec4(brdf(),1);
}