#version 430 core
#define PI 3.14159265359

in vec3 position;
in vec3 normal;
in vec2 UV;
in vec3 tangent;
in vec3 bitangent;

out vec4 fragColour;

uniform sampler2D Albedo;
uniform vec3 LightDirection;
uniform vec3 ViewPosition;

vec3 albedoColour = pow(texture(Albedo,UV).rgb,vec3(2.2));

const float roughness = 0.2;
const float subsurface = 0;
const float sheen = 0;
const float annisotropy = 0.5;
const float specularStrength = 0.5;
const float ambientOcclussion = 0.2;

const vec3 sheenColour = vec3(1,1,1);
const vec3 diffuseColour = vec3(1,1,1);

uniform samplerCube IrradianceMap;

float GTR2Aniso(float ndotH, float hdotX, float hdotY, float ax, float ay){
    return 1 / (PI * ax*ay * pow( pow(hdotX/ax,2) + pow(hdotY/ay,2) + ndotH*ndotH,2 ));
}

float smithG_GGX_aniso(float NdotV, float VdotX, float VdotY, float ax, float ay){
    return 1 / (NdotV + sqrt( pow(VdotX*ax,2) + pow(VdotY*ay,2) + pow(NdotV,2) ));
}


float schlickFresnel(float u){
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

    vec3 fresnelSheen = schlickFresnel(lDotH) * sheen * pow(sheenColour,vec3(2.2));

    return mix(diffuse,ss,subsurface) + fresnelSheen;
}

vec3 disneySpecular(vec3 halfway,vec3 viewDirection,vec3 lightDirection){
    float lDotH = max(dot(lightDirection,halfway),0.0f);
    float nDotH = max(dot(normal,halfway),0.0f);
    float xDotH = dot(tangent,halfway);
    float yDotH = dot(bitangent,halfway);

    float nDotL = max(dot(normal,lightDirection),0.0f);
    float nDotV = max(dot(normal,viewDirection),0.0f);

    float xDotL = dot(tangent,lightDirection);
    float yDotL = dot(bitangent,lightDirection);

    float xDotV = dot(tangent,viewDirection);
    float yDotV = dot(bitangent,viewDirection);

    float aspect = sqrt(1 - annisotropy * 0.9);
    float ax = max(0.001,(roughness * roughness) / aspect);
    float ay = max(0.001,(roughness * roughness) * aspect);

    float distribution = GTR2Aniso(nDotH,xDotH,yDotH,ax,ay);
    float fresnelHalfWay = schlickFresnel(lDotH);
    vec3 fresnelResponse = mix(specularStrength * vec3(0.8),vec3(1.0),fresnelHalfWay);

    float geometricAttenuation = smithG_GGX_aniso(nDotL,xDotL,yDotL,ax,ay);
    geometricAttenuation *= smithG_GGX_aniso(nDotV,xDotV,yDotV,ax,ay);

    return (distribution * fresnelResponse * geometricAttenuation) ;
}

vec3 brdf(){
    vec3 lightDirection = normalize(LightDirection);
    vec3 viewDirection = normalize(ViewPosition - position);
    vec3 halfway = normalize(lightDirection + viewDirection);

    float nDotL = dot(normal,lightDirection);
    float nDotV = dot(normal,viewDirection);

    if(nDotL < 0 || nDotV < 0) return vec3(0.0f);

    nDotL = max(nDotL,0.0f);
    nDotV = max(nDotV,0.0f);

    float lDotH = max(dot(lightDirection,halfway),0.0f);

    vec3 diffuse = burleyDiffuse(nDotL,nDotV,lDotH) * diffuseColour / PI;
    vec3 specular = disneySpecular(halfway,viewDirection,lightDirection);

    vec3 lighting = diffuse * albedoColour + specular;

    return lighting * nDotL;
}

vec3 generateAmbientLight(){
    vec3 viewDirection = normalize(ViewPosition - position);
    float nDotV = max(dot(normal,viewDirection),0.0f);

    vec3 baseReflectivity = vec3(0.04);
    vec3 kS = baseReflectivity + (1.0 - baseReflectivity) * pow((1 - nDotV),5.0);
    vec3 kD = 1.0 - kS;

    vec3 diffuse = texture(IrradianceMap,normal).rgb * albedoColour * kD;

    return diffuse * ambientOcclussion;
}

void main() {
    vec3 ambientLight = generateAmbientLight();

    vec3 colour = brdf() + ambientLight;
    colour = colour / (colour + vec3(1.0));
    colour = pow(colour,vec3(1/2.2));

    fragColour = vec4(colour,1);
}