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
uniform sampler2D ShadowMap;
uniform sampler2DShadow ShadowMapComparison;

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

uniform mat4 LightMatrix;

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
// utility

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

float random(vec2 p){
    return fract(sin(dot(p,vec2(127.1,311.7))) * 43758.5453);
}

// brdf

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

// image based lighting

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

// shadows

float calculatePCF(vec4 projected,float bias,float penumbra){
    float shadow = 0.0f;
    vec2 texelSize = 1.0 / textureSize(ShadowMap,0);

    int kernelSize = 7;
    int kernelWidth = kernelSize / 2;

    float orthoWidth = 60;
    float orthoHeight = 60;

    vec2 blurRadius = vec2(penumbra / orthoWidth,penumbra / orthoHeight);

    vec2 noiseUV = floor(gl_FragCoord.xy / 4.0);

    for(int y = -kernelWidth; y <= kernelWidth; y++) {
        for(int x = -kernelWidth; x <= kernelWidth; x++) {
            vec2 offset = vec2(
                random(noiseUV.xy + vec2(x,y)),
                random(noiseUV.xy + vec2(x,y) + vec2(17.13,91.71))
            ) - 0.5;

            vec2 uv = projected.xy + offset * blurRadius;

            shadow += texture(ShadowMapComparison,vec3(uv,projected.z - bias));
        }
    }

    return shadow / (kernelSize * kernelSize);
}

float PCSS(vec4 projected,float bias){
    vec2 texelSize = 1.0 / textureSize(ShadowMap,0);

    float near = 0.1;
    float far = 100.0;

    float penumbra = 0.0f;

    int kernelSize = 5;
    int kernelWidth = kernelSize / 2;

    float orthoWidth = 60;
    float orthoHeight = 60;

    float reciever = near + projected.z * (far - near);
    float converted_bias =  bias* (far - near);

    float lightAngle = radians(3);

    float blocker = 0.0;
    float blocker_count = 0.0;
    for(int y = -kernelWidth; y <= kernelWidth; y++) {
        for(int x = -kernelWidth; x <= kernelWidth; x++) {
            vec2 uv = projected.xy + vec2(x,y) * texelSize;

            float block = near + texture(ShadowMap,uv).r * (far - near);

            if(block >= reciever - converted_bias) continue;

            blocker += block;
            blocker_count += 1.0;
        }
    }

    if(blocker_count == 0.0) return 0.0;

    return (reciever - (blocker / blocker_count)) * tan(lightAngle);
}

float generateShadow(vec3 norm){
    float nDotL = dot(norm,LightDirection);

    vec4 lightSpacePosition = LightMatrix * vec4(position,1.0);

    vec4 projected = lightSpacePosition / lightSpacePosition.w;
    projected = projected * 0.5 + 0.5;

    if(projected.z > 1.0  
    || projected.z < 0   
    || projected.x < 0   
    || projected.x > 1.0 
    || projected.y < 0   
    || projected.y > 1.0
    ){
        return 1.0f;
    }

    float bias = max(0.025 * (1.0 - nDotL),0.005 );

    float penumbra = PCSS(projected,bias);

    return calculatePCF(projected,bias,penumbra);    
}

void main() {

    Material material = getMaterial();

    vec3 norm = texture(NormalMap,UV).rgb * 2 - 1;
    norm = normalize(TBN * norm);

    vec3 ambientLight = generateAmbientLight(material,norm);

    vec3 colour = brdf(material,norm) * generateShadow(norm) + ambientLight * mix(0.35,1.0,generateShadow(norm));

    fragColour = vec4(colour, 1.0);
}