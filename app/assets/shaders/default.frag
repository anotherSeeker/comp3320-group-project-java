#version 430 core
#define PI 3.14159265359

in vec3 position;
in vec3 normal;
in vec2 UV;
in vec3 tangent;
in vec3 bitangent;

in mat3 TBN;

out vec4 fragColour;

uniform sampler2D Albedo;
uniform sampler2D RoughnessMap;
uniform sampler2D AmbientOcclusionMap;
uniform sampler2D NormalMap;
uniform sampler2D MetallicMap;

uniform vec3 LightDirection;
uniform vec3 ViewPosition;

uniform float Roughness = 0.2;
uniform float AmbientOcclusion = 0.5;
uniform float Metallic = 0;

uniform float Subsurface = 0;
uniform float Sheen = 0;
uniform float Annisotropy = 0.5;
uniform float SpecularStrength = 0.5;

uniform vec3 SheenColour = vec3(1,1,1);
uniform vec3 DiffuseColour = vec3(1,1,1);

struct Material {
    float roughness;
    float ambientOcclusion;
    float metallic;
    vec3 albedo;
};

uniform samplerCube IrradianceMap;
uniform samplerCube PrefilterMap;
uniform sampler2D brdfIntMap;

Material getMaterial(){
    Material material;

    material.roughness = max(texture(RoughnessMap,UV).r * Roughness,0.02);
    material.ambientOcclusion = texture(AmbientOcclusionMap,UV).r * AmbientOcclusion;
    material.albedo = pow(texture(Albedo,UV).rgb,vec3(2.2));

    material.metallic = clamp(texture(MetallicMap,UV).r * Metallic,0,1);

    return material;
}

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

vec3 burleyDiffuse(Material material,float nDotL,float nDotV,float lDotH){
    float fresnelLight = schlickFresnel(nDotL);
    float fresnelView = schlickFresnel(nDotV);

    float fd90 = 0.5 + 2 * lDotH * lDotH * material.roughness;

    float diffuse = mix(1.0,fd90,fresnelLight) * mix(1.0,fd90,fresnelView);

    float fresnelSubSurface90 = lDotH * lDotH * material.roughness;
    float fresnelSubSurface = mix(1.0,fresnelSubSurface90,fresnelLight) * mix(1.0,fresnelSubSurface90,fresnelView);

    float ss = 1.25 * (fresnelSubSurface * (1 / (nDotL + nDotV) - 0.5) + 0.5);

    vec3 fresnelSheen = schlickFresnel(lDotH) * Sheen * pow(SheenColour,vec3(2.2));

    return (mix(diffuse,ss,Subsurface) + fresnelSheen) * (1 - material.metallic);
}

vec3 disneySpecular(Material material,vec3 norm,vec3 halfway,vec3 viewDirection,vec3 lightDirection){
    float lDotH = max(dot(lightDirection,halfway),0.0f);
    float nDotH = max(dot(norm,halfway),0.0f);
    float xDotH = dot(tangent,halfway);
    float yDotH = dot(bitangent,halfway);

    float nDotL = max(dot(norm,lightDirection),0.0f);
    float nDotV = max(dot(norm,viewDirection),0.0f);

    float xDotL = dot(tangent,lightDirection);
    float yDotL = dot(bitangent,lightDirection);

    float xDotV = dot(tangent,viewDirection);
    float yDotV = dot(bitangent,viewDirection);

    float aspect = sqrt(1 - Annisotropy * 0.9);
    float ax = max(0.001,(material.roughness * material.roughness) / aspect);
    float ay = max(0.001,(material.roughness * material.roughness) * aspect);

    float distribution = GTR2Aniso(nDotH,xDotH,yDotH,ax,ay);
    float fresnelHalfWay = schlickFresnel(lDotH);

    vec3 cSpec0 = mix(SpecularStrength * vec3(0.8),material.albedo,material.metallic);

    vec3 fresnelResponse = mix(cSpec0,vec3(1.0),fresnelHalfWay);

    float geometricAttenuation = smithG_GGX_aniso(nDotL,xDotL,yDotL,ax,ay);
    geometricAttenuation *= smithG_GGX_aniso(nDotV,xDotV,yDotV,ax,ay);

    return (distribution * fresnelResponse * geometricAttenuation) ;
}

vec3 brdf(Material material,vec3 norm){
    vec3 lightDirection = normalize(LightDirection);
    vec3 viewDirection = normalize(ViewPosition - position);
    vec3 halfway = normalize(lightDirection + viewDirection);

    float nDotL = dot(norm,lightDirection);
    float nDotV = dot(norm,viewDirection);

    if(nDotL < 0 || nDotV < 0) return vec3(0.0f);

    nDotL = max(nDotL,0.0f);
    nDotV = max(nDotV,0.0f);

    float lDotH = max(dot(lightDirection,halfway),0.0f);

    vec3 diffuse = burleyDiffuse(material,nDotL,nDotV,lDotH) * DiffuseColour / PI;
    vec3 specular = disneySpecular(material,norm,halfway,viewDirection,lightDirection);

    vec3 lighting = diffuse * material.albedo + specular;

    return lighting * nDotL;
}

vec3 generateAmbientLight(Material material,vec3 norm){

    vec3 viewDirection = normalize(ViewPosition - position);
    float nDotV = max(dot(norm,viewDirection),0.0f);

    vec3 baseReflectivity = vec3(0.04);

    vec3 F0 = mix(baseReflectivity,material.albedo,material.metallic);

    vec3 kS = F0 + (max(vec3(1.0 - material.roughness),F0) - F0) * pow((1 - nDotV),5.0);
    vec3 kD = (1.0 - kS) * (1.0 - material.metallic);

    vec3 diffuse = texture(IrradianceMap,norm).rgb * material.albedo * kD;

    const float MAX_REFLECTION_LOD = 7.0;
    vec3 prefilterColour = textureLod(PrefilterMap,reflect(-viewDirection,norm),material.roughness* MAX_REFLECTION_LOD).rgb;

    vec2 brdfIBL = texture(brdfIntMap,vec2(nDotV,material.roughness)).rg;

    vec3 specular = prefilterColour * (kS * brdfIBL.r + brdfIBL.g);

    return (diffuse * material.ambientOcclusion + specular) ;
}

void main() {

    Material material = getMaterial();

    vec3 norm = texture(NormalMap,UV).rgb * 2 - 1;
    norm = normalize(TBN * norm);

    vec3 ambientLight = generateAmbientLight(material,norm);

    vec3 colour = brdf(material,norm) + ambientLight;

    fragColour = vec4(colour ,1.0);
}