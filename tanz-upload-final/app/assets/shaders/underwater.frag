// underwater.frag - the team's default.frag (PBR + PCSS shadows) with underwater
// lighting added by Tanz. Everything added is marked "TANZ". Use with underwater.vert.
// Everything below the water line (WATER_LEVEL) is lit as under water, everything
// above it as in air, using the physics of light at a water surface:
//   - Snell's law       n1 sin(a1) = n2 sin(a2)   (water n = 1.333): sunlight bends into
//                        the water; from below you only see out within 48.6 degrees of
//                        straight up (the critical angle), outside it the surface is a mirror
//   - Fresnel (Schlick)  R = R0 + (1 - R0)(1 - cos a)^5: part of the light reflects off
//                        the surface instead of going through
//   - Beer-Lambert       I = I0 * exp(-absorption * distance): water absorbs light, red
//                        fastest, then green, then blue - so deeper/further looks blue-green
// Under water also: caustics (light lines) and softer shadows (water scatters light).
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

// ---- TANZ: underwater settings
uniform float Time = 0.0;                    // seconds, sent by SunkenCastleScene
uniform float CausticsStrength = 1.5;        // 0 = no caustics
uniform float RayStrength = 1.0;             // light rays in the water (0 = none)
const float CAUSTICS_SIZE = 0.45;            // bigger = smaller caustic cells
const vec3 WATER_AMBIENT = vec3(0.35, 0.75, 0.85);   // blue-green light from the water around
const float AMBIENT_TINT = 0.7;              // how much the ambient light turns blue-green
const float WATER_LEVEL = 14.0;              // height of the water surface
const float WATER_IOR = 1.333;               // refractive index of water (air = 1.0)
const vec3 ABSORPTION = vec3(0.30, 0.07, 0.05);   // per metre, red / green / blue (Beer-Lambert)
const float VIEW_CLARITY = 0.35;             // the view path uses less absorption, so the castle stays visible
const float ABOVE_WATER_CLARITY = 0.12;      // parts above the water look solid (only a short haze)
const vec3 WATER_LINE_COLOUR = vec3(0.75, 0.95, 1.0);   // bright line where the surface touches things
const float WET_BAND = 0.35;                 // metres above the water line that look wet (darker)
const float SHADOW_SOFTNESS_DEGREES = 6.0;   // the team default is 3 (sun in air)

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

    float lightAngle = radians(SHADOW_SOFTNESS_DEGREES);   // TANZ: softer under water

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

// ---- TANZ: light at the water surface (physics)
// Fresnel reflectance, Schlick's approximation: R0 = ((n1 - n2) / (n1 + n2))^2
float fresnelReflect(float cosAngle) {
    float r0 = pow((1.0 - WATER_IOR) / (1.0 + WATER_IOR), 2.0);   // 0.02 for air/water
    return r0 + (1.0 - r0) * pow(1.0 - clamp(cosAngle, 0.0, 1.0), 5.0);
}

// Sunlight reaching an underwater point: it enters the surface (Fresnel), bends
// (Snell), then is absorbed along its path down through the water (Beer-Lambert).
vec3 sunThroughWater(vec3 point) {
    vec3 toSun = normalize(LightDirection);
    float transmitted = 1.0 - fresnelReflect(max(toSun.y, 0.0));
    vec3 inWater = refract(-toSun, vec3(0.0, 1.0, 0.0), 1.0 / WATER_IOR);   // Snell: direction in water
    float path = (WATER_LEVEL - point.y) / max(-inWater.y, 0.05);             // metres of water travelled
    return transmitted * exp(-ABSORPTION * path);
}

// ---- TANZ: light rays (god rays) in the water
// For this pixel, walk along the line of sight through the water. At each step:
//   - is the point in sunlight? (the engine's shadow map - so the castle and
//     trees block the rays, and the rays point the same way as the shadows)
//   - how much sunlight is left at that depth? (Beer-Lambert absorption)
//   - the rays are brighter or darker in streaks (wave pattern from the surface,
//     projected along the sunlight)
// The total is the light scattered towards the camera along the way.
const float RAY_WATER_LEVEL = 14.0;
const int RAY_STEPS = 24;
const float RAY_MAX_DISTANCE = 60.0;
const vec3 RAY_ABSORPTION = vec3(0.30, 0.07, 0.05);   // per metre, as in underwater.frag
const float RAY_DENSITY = 0.012;                       // how much light the water scatters to us

float rayNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = fract(sin(dot(i, vec2(127.1, 311.7))) * 43758.5453);
    float b = fract(sin(dot(i + vec2(1, 0), vec2(127.1, 311.7))) * 43758.5453);
    float c = fract(sin(dot(i + vec2(0, 1), vec2(127.1, 311.7))) * 43758.5453);
    float d = fract(sin(dot(i + vec2(1, 1), vec2(127.1, 311.7))) * 43758.5453);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

// 1 = this point gets sunlight, 0 = something blocks it (engine shadow map)
float sunVisible(vec3 point) {
    vec4 light = LightMatrix * vec4(point, 1.0);
    vec3 p = light.xyz / light.w * 0.5 + 0.5;
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0 || p.z > 1.0) return 1.0;
    return texture(ShadowMapComparison, vec3(p.xy, p.z - 0.003));
}

vec3 lightRays(vec3 from, vec3 to, float time, float strength) {
    if (strength <= 0.0 || from.y > RAY_WATER_LEVEL) return vec3(0.0);
    vec3 toSun = normalize(LightDirection);
    vec3 across = normalize(cross(toSun, abs(toSun.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
    vec3 up = cross(toSun, across);

    vec3 ray = to - from;
    float rayLength = min(length(ray), RAY_MAX_DISTANCE);
    vec3 direction = normalize(ray);
    float stepLength = rayLength / float(RAY_STEPS);
    float jitter = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);   // hides banding

    vec3 total = vec3(0.0);
    for (int i = 0; i < RAY_STEPS; i++) {
        vec3 point = from + direction * (float(i) + jitter) * stepLength;
        float depth = RAY_WATER_LEVEL - point.y;
        if (depth < 0.0) continue;                                   // above the water: no rays

        // streaks: the same along the sunlight, changing across it (and slowly in time)
        vec2 q = vec2(dot(point, across), dot(point, up));
        float streak = rayNoise(q * 0.35 + vec2(time * 0.08, 0.0)) * 0.7 + rayNoise(q * 1.1 - vec2(0.0, time * 0.12)) * 0.3;
        streak = smoothstep(0.35, 0.85, streak);

        vec3 sunLeft = exp(-RAY_ABSORPTION * depth);                 // Beer-Lambert from the surface
        vec3 towardsCamera = exp(-RAY_ABSORPTION * 0.35 * float(i) * stepLength);   // dimmer further away
        total += sunVisible(point) * streak * sunLeft * towardsCamera;
    }
    return total * stepLength * RAY_DENSITY * strength * DiffuseColour;
}

// ---- TANZ: caustics
// Distance to the nearest Voronoi cell edge (like Blender's Voronoi "Distance to Edge").
vec2 randomPoint(vec2 cell) {
    return fract(sin(vec2(dot(cell, vec2(127.1, 311.7)), dot(cell, vec2(269.5, 183.3)))) * 43758.5453);
}

float voronoiEdge(vec2 p) {
    vec2 cell = floor(p);
    vec2 local = fract(p);
    vec2 closestCell, closestOffset;
    float closest = 8.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 g = vec2(x, y);
            vec2 centre = 0.5 + 0.5 * sin(Time * 0.8 + 6.2831 * randomPoint(cell + g));   // moves with time
            vec2 offset = g + centre - local;
            float d = dot(offset, offset);
            if (d < closest) { closest = d; closestOffset = offset; closestCell = g; }
        }
    }
    float edge = 8.0;
    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            vec2 g = closestCell + vec2(x, y);
            vec2 centre = 0.5 + 0.5 * sin(Time * 0.8 + 6.2831 * randomPoint(cell + g));
            vec2 offset = g + centre - local;
            if (dot(closestOffset - offset, closestOffset - offset) > 0.00001) {
                edge = min(edge, dot(0.5 * (closestOffset + offset), normalize(offset - closestOffset)));
            }
        }
    }
    return edge;
}

// Caustics are sunlight focused by the waves, so the pattern is projected along
// the sunlight onto everything (a flat sheet facing the sun).
float caustics(vec3 worldPosition) {
    vec3 toSun = normalize(LightDirection);
    vec3 across = normalize(cross(toSun, abs(toSun.z) < 0.99 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0)));
    vec3 up = cross(across, toSun);
    vec2 p = vec2(dot(worldPosition, across), dot(worldPosition, up)) * CAUSTICS_SIZE;
    p += 0.35 * vec2(sin(p.y * 1.3 + Time * 0.7), cos(p.x * 1.1 + Time * 0.6));   // wobble = wavy lines
    return 1.0 - smoothstep(0.0, 0.09, voronoiEdge(p));   // 1 on a line, 0 inside a cell
}

void main() {

    Material material = getMaterial();

    vec3 norm = texture(NormalMap,UV).rgb * 2 - 1;
    norm = normalize(TBN * norm);

    vec3 ambientLight = generateAmbientLight(material,norm);

    // TANZ: under water or in air? (a soft 0.2 m band at the water line)
    float underwater = 1.0 - smoothstep(WATER_LEVEL - 0.1, WATER_LEVEL + 0.1, position.y);

    // sunlight: full in air; under water it goes through the surface and is absorbed
    vec3 sun = mix(vec3(1.0), sunThroughWater(position), underwater);

    // caustics (light focused by the waves) only exist under the water
    float focus = 1.0 + CausticsStrength * caustics(position) * underwater;

    // ambient light: the sky in air, blue-green light from the water around under water
    vec3 waterAmbient = mix(ambientLight, ambientLight * WATER_AMBIENT * 2.0, AMBIENT_TINT);
    ambientLight = mix(ambientLight, waterAmbient, underwater);

    float shadow = generateShadow(norm);
    vec3 colour = brdf(material,norm) * shadow * focus * sun + ambientLight * mix(0.35,1.0,shadow);

    // TANZ: the water line - a thin bright line where the surface touches the
    // castle, and a darker wet band just above it
    float aboveLine = position.y - WATER_LEVEL;
    float wet = 1.0 - smoothstep(0.0, WET_BAND, aboveLine);
    colour *= mix(1.0, 0.7, wet * step(0.0, aboveLine));
    float line = 1.0 - smoothstep(0.0, 0.07, abs(aboveLine));
    colour = mix(colour, WATER_LINE_COLOUR * length(DiffuseColour) * 0.25, line * 0.8);

    // TANZ: the light travelling from this point to a camera under the water
    if (ViewPosition.y < WATER_LEVEL) {
        vec3 toPoint = position - ViewPosition;
        float distance = length(toPoint);
        vec3 direction = toPoint / distance;
        float waterPath = distance;   // metres of water the light passes through

        if (position.y > WATER_LEVEL) {
            // above the water: the light crosses the surface on its way to the camera
            waterPath = (WATER_LEVEL - ViewPosition.y) / max(direction.y, 0.001);
            float cosWater = max(direction.y, 0.0);                    // angle from straight up, in water
            float sinAir = sqrt(1.0 - cosWater * cosWater) * WATER_IOR;   // Snell's law
            if (sinAir >= 1.0) {
                // outside Snell's window: total internal reflection - the surface is a
                // mirror there, so the camera sees the water, not the castle above it
                colour = WATER_AMBIENT * 0.25;
            } else {
                colour *= 1.0 - fresnelReflect(sqrt(1.0 - sinAir * sinAir));   // part reflects back
            }
        }

        // Beer-Lambert along the view path, with the water's own colour scattered in
        float clarity = position.y > WATER_LEVEL ? ABOVE_WATER_CLARITY : VIEW_CLARITY;
        vec3 kept = exp(-ABSORPTION * clarity * waterPath);
        colour = colour * kept + WATER_AMBIENT * 0.25 * (1.0 - kept);
    }

    // TANZ: light rays in the water between the camera and this point
    colour += lightRays(ViewPosition, position, Time, RayStrength);

    fragColour = vec4(colour, 1.0);
}