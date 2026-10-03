#version 430 core
#define PI 3.14159265359

out vec4 fragColour;
in vec2 UV;

uniform samplerCube EnvironmentMap;

float DistributionGGX(vec3 N, vec3 H, float roughness){
    float a = roughness*roughness;
    float a2 = a*a;
    float nDotH  = max(dot(N, H), 0.0);
    float nDotH2 = nDotH*nDotH;
	
    float num = a2;
    float denom = (nDotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
	
    return num / denom;
}


// hammersley sequence
float RadicalInverse_VdC(uint bits) {
    bits = (bits << 16u) | (bits >> 16u);
    bits = ((bits & 0x55555555u) << 1u) | ((bits & 0xAAAAAAAAu) >> 1u);
    bits = ((bits & 0x33333333u) << 2u) | ((bits & 0xCCCCCCCCu) >> 2u);
    bits = ((bits & 0x0F0F0F0Fu) << 4u) | ((bits & 0xF0F0F0F0u) >> 4u);
    bits = ((bits & 0x00FF00FFu) << 8u) | ((bits & 0xFF00FF00u) >> 8u);
    return float(bits) * 2.3283064365386963e-10;
}
    
vec2 hammersley(uint i, uint normal){
    return vec2(float(i)/float(normal),RadicalInverse_VdC(i));
}

vec3 ImportanceSampleGGX(vec2 xi, vec3 normal, float roughness){
    float a = roughness * roughness;

    float phi = 2.0 * PI * xi.x;
    float cosTheta = sqrt((1.0 - xi.y) / (1.0 + (a*a - 1.0) * xi.y));
    float sinTheta = sqrt(1.0 - cosTheta*cosTheta);

    vec3 H = vec3(cos(phi) * sinTheta, sin(phi) * sinTheta, cosTheta); 

    vec3 up = abs(normal.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
    vec3 tangent = normalize(cross(up, normal));
    vec3 bitangent = cross(normal, tangent);

    vec3 sampleVector = tangent * H.x + bitangent * H.y + normal * H.z;

    return normalize(sampleVector);
}


// non annisotropic variants since i dont want to deal with annisotropy
float geometrySchlickGGX(float nDotV,float roughness){
    float k = roughness * roughness / 2;

    float denom = nDotV * (1 - k) + k;

    return nDotV / denom;
}

float geometrySmith(vec3 N,vec3 V,vec3 L,float roughness){
    float nDotV = max(dot(N,V),0.0);
    float nDotL = max(dot(N,L),0.0);

    float ggx2 = geometrySchlickGGX(nDotV,roughness);
    float ggx1 = geometrySchlickGGX(nDotL,roughness);

    return ggx1 * ggx2;
}

const uint SAMPLE_COUNT = 1024u;

vec2 integrateBRDF(float nDotV,float roughness){
    vec3 V = vec3(
        sqrt(1 - nDotV * nDotV),
        0.0,
        nDotV
    );

    float A = 0.0;
    float B = 0.0;

    vec3 N = vec3(0.0,0.0,1.0);

    for(uint i = 0; i < SAMPLE_COUNT; ++i){
        vec2 xi = hammersley(i,SAMPLE_COUNT);
        vec3 H = ImportanceSampleGGX(xi,N,roughness);
        vec3 L = normalize(2.0 * dot(V,H) * H - V);

        float nDotL = max(L.z,0.0);
        float nDotH = max(H.z,0.0);
        float vDotH = max(dot(V,H),0.0);
        
        if(nDotL <= 0) continue;

        float G = geometrySmith(N,V,L,roughness);
        float GVisible = (G * vDotH) / (nDotH * nDotV);
        float fc = pow(1.0 - vDotH,5.0);

        A += (1.0 - fc) * GVisible;
        B += fc * GVisible;
    }

    A /= SAMPLE_COUNT;
    B /= SAMPLE_COUNT;
    
    return vec2(A,B);
}

void main(){
    vec2 integrated = integrateBRDF(UV.x,UV.y);
    fragColour = vec4(integrated,0,1);
}