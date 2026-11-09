#version 430 core
// water_background.frag - the colour of deep water all around (Tanz)
// Dark looking down, the water colour looking sideways, lighter looking up
// towards the surface - plus light
// rays from the sun shining down through the water.

in vec3 direction;

out vec4 fragColour;

uniform vec3 WaterColour = vec3(0.12, 0.40, 0.46);   // colour of the water itself
uniform vec3 ViewPosition;          // camera position (sent by the engine)
uniform vec3 LightDirection;        // towards the sun (sent by the engine)
uniform vec3 DiffuseColour;         // sun colour (sent by the engine)
uniform mat4 LightMatrix;           // the sun's view, for the shadow map (sent by the engine)
uniform sampler2DShadow ShadowMapComparison;   // the shadow map (sent by the engine, unit 10)
uniform float Time;                 // seconds (sent by WaterSurface)
uniform float RayStrength = 1.0;    // light rays in the water (sent by UnderwaterLighting)
uniform float SunBoost = 1.0;       // how strongly this shader uses the engine's sun (sent by UnderwaterLighting)


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
    return total * stepLength * RAY_DENSITY * strength * DiffuseColour * SunBoost;
}

void main() {
    vec3 dir = normalize(direction);
    vec3 deep = WaterColour * 0.15;
    vec3 colour = mix(deep, WaterColour, smoothstep(-0.6, 0.1, dir.y));     // down -> sideways
    colour = mix(colour, WaterColour * 1.6, smoothstep(0.1, 1.0, dir.y));    // sideways -> up
    // light rays in the open water around the scene
    colour += lightRays(ViewPosition, ViewPosition + dir * RAY_MAX_DISTANCE, Time, RayStrength);

    fragColour = vec4(colour, 1.0);
}
