#version 430 core
#define PI 3.14159265359

out vec4 fragColour;
in vec3 worldPosition;

uniform samplerCube EnvironmentMap;
uniform float Roughness;

float DistributionGGX(vec3 N, vec3 H, float roughness){
    float a      = roughness*roughness;
    float a2     = a*a;
    float NdotH  = max(dot(N, H), 0.0);
    float NdotH2 = NdotH*NdotH;
	
    float num   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
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

const uint SAMPLE_COUNT = 1024u;

void main(){
    vec3 normal = normalize(worldPosition);
    vec3 R = normal;
    vec3 V = R;

    float totalWeight = 0.0;
    vec3 prefilterColour = vec3(0.0);

    for(uint i = 0; i < SAMPLE_COUNT; ++i){
        vec2 xi = hammersley(i,SAMPLE_COUNT);
        vec3 H = ImportanceSampleGGX(xi,normal,Roughness);
        vec3 L = normalize(2.0 * dot(V,H) * H - V);

        float nDotL = max(dot(normal,L),0.0);
        if(nDotL <= 0.0) continue;

        float D = DistributionGGX(normal,H,Roughness);
        float nDotH = max(dot(normal,H),0.0);
        float vDotH = max(dot(V,H),0.0);

        float pdf = D * nDotH / (4.0 * vDotH) + 0.0001;

        float sourceResolution = 512.0;
        float saTexel = 4.0 * PI / (6.0 * sourceResolution * sourceResolution);
        float saSample = 1.0 / (float(SAMPLE_COUNT) * pdf + 0.0001);

        float mipLevel = Roughness == 0.0 ? 0.0 : 0.5 * log2(saSample / saTexel);

        prefilterColour += textureLod(EnvironmentMap,L, mipLevel).rgb * nDotL;
        totalWeight += nDotL;
    }

    prefilterColour = prefilterColour / totalWeight;
    fragColour = vec4(prefilterColour,1.0);
}