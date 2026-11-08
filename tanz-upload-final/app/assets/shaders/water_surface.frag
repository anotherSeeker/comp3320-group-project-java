#version 430 core
// water_surface.frag - the water surface seen from underneath (Tanz)
//
// What a real sea surface looks like from below:
//   - straight above you: a bright round "window" where you see out of the water
//     (Snell's window) - the sky, bent and broken up by the ripples
//   - everywhere else: light reflects back down (total internal reflection), so
//     the surface is a dark, softly rippled mirror of the water, with a faint shimmer
//   - further away it fades into the murky water colour, like everything else
//   - things sticking out above the water (the tower tops) show through it
//
// The colours are HDR: the engine's tone mapping, bloom and fog are applied after.

in vec3 worldPosition;

out vec4 fragColour;

uniform mat4 CameraMatrix;          // projection * view (sent by the engine)
uniform vec3 ViewPosition;          // camera position (sent by the engine)
uniform vec3 LightDirection;        // towards the sun (sent by the engine)
uniform samplerCube PrefilterMap;   // the sky picture (sent by the engine, unit 2)
uniform vec3 DiffuseColour;         // sun colour (sent by the engine)
uniform mat4 LightMatrix;           // the sun's view, for the shadow map (sent by the engine)
uniform sampler2DShadow ShadowMapComparison;   // the shadow map (sent by the engine, unit 10)
uniform float RayStrength = 1.0;    // light rays in the water (sent by UnderwaterLighting)
uniform float Time;                 // seconds (sent by the WaterSurface script)
uniform vec3 WaterColour = vec3(0.12, 0.40, 0.46);   // the fog colour (sent by WaterSurface)

const float WATER_IOR = 1.333;      // how much water bends light
const float SKY_BRIGHTNESS = 0.9;   // brightness of the window above (kept soft)
const float SUN_GLINT = 4.0;        // the sun seen through the window
const float SHIMMER = 0.15;         // faint light shimmer on the underside
const float RIPPLE_SIZE = 2.5;      // bigger = smaller ripples
const float MURK = 0.03;            // how fast the surface fades into the water colour
const float ABOVE_WATER = 8.0;      // things up to this high above the water show through (tower tops)

// ---------------------------------------------------------------- ripples
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// smooth random value (value noise)
float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x),
               mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

// height of the surface: gentle swells plus small moving ripples
float waveHeight(vec2 p) {
    float h = sin(dot(p, vec2(0.21, 0.07)) + Time * 0.6) * 0.20
            + sin(dot(p, vec2(-0.09, 0.26)) + Time * 0.8) * 0.15;
    vec2 q = p * RIPPLE_SIZE;
    h += noise(q + vec2(Time * 0.7, Time * 0.3)) * 0.06;
    h += noise(q * 2.3 - vec2(Time * 1.1, Time * 0.8)) * 0.025;
    return h;
}

// the rippled normal, pointing down (towards a camera under the water)
vec3 waveNormal(vec2 p) {
    float gap = 0.03;
    float dx = (waveHeight(p + vec2(gap, 0.0)) - waveHeight(p - vec2(gap, 0.0))) / (2.0 * gap);
    float dz = (waveHeight(p + vec2(0.0, gap)) - waveHeight(p - vec2(0.0, gap))) / (2.0 * gap);
    return normalize(vec3(dx, -1.0, dz));
}

// the same colour as the water all around (water_background.frag), for fading
vec3 murkColour(vec3 dir) {
    vec3 colour = mix(WaterColour * 0.15, WaterColour, smoothstep(-0.6, 0.1, dir.y));
    return mix(colour, WaterColour * 1.6, smoothstep(0.1, 1.0, dir.y));
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

// ---------------------------------------------------------------- main
void main() {
    vec3 view = normalize(worldPosition - ViewPosition);   // from the camera to this point
    float distance = length(worldPosition - ViewPosition);
    vec3 normal = waveNormal(worldPosition.xz);

    if (ViewPosition.y > worldPosition.y) {
        // camera above the water: the sky reflected, a little water colour
        normal.y = -normal.y;
        float fresnel = 0.02 + 0.98 * pow(1.0 - max(dot(-view, normal), 0.0), 5.0);
        vec3 sky = textureLod(PrefilterMap, reflect(view, normal), 0.0).rgb;
        fragColour = vec4(mix(WaterColour * 0.3, sky, fresnel), 1.0);
        gl_FragDepth = gl_FragCoord.z;   // normal depth from above
        return;
    }

    // how straight we look at the surface (1 = straight up)
    float facing = max(dot(-view, normal), 0.0);

    // Snell's window: light only gets out when it meets the surface steeply enough
    // (the edge is where sin(angle) * 1.333 = 1, so cos(angle) = 0.66)
    float edge = sqrt(1.0 - 1.0 / (WATER_IOR * WATER_IOR));
    float window = smoothstep(edge - 0.03, edge + 0.07, facing);

    // inside the window: the sky, bent by the ripples, plus the sun
    vec3 through = refract(view, normal, 1.0 / WATER_IOR);
    vec3 sky = textureLod(PrefilterMap, through, 0.0).rgb * SKY_BRIGHTNESS;
    float sun = pow(max(dot(through, normalize(LightDirection)), 0.0), 300.0);
    vec3 outside = mix(sky, WaterColour * 2.0, 0.25) + SUN_GLINT * sun;

    // outside the window: a dark mirror of the water; ripples facing the camera
    // catch a little more light, plus a faint moving shimmer
    float catchLight = clamp(dot(normal.xz, -view.xz) * 5.0, -1.0, 1.0);
    float shimmer = noise(worldPosition.xz * RIPPLE_SIZE * 1.3 + Time * 0.5);
    vec3 mirror = WaterColour * (0.55 + 0.35 * catchLight) + WaterColour * SHIMMER * shimmer * 4.0;

    // near the window edge some light reflects (Fresnel)
    float transmit = window * (1.0 - pow(1.0 - facing, 5.0));
    vec3 colour = mix(mirror, outside, transmit);

    // further away the surface fades into the murky water
    float fade = exp(-distance * MURK);
    colour = mix(murkColour(view), colour, fade);

    // light rays in the water between the camera and the surface
    colour += lightRays(ViewPosition, worldPosition, Time, RayStrength);

    fragColour = vec4(colour, 1.0);

    // See-through: the surface is drawn as if it were a little further away - as far
    // as a point ABOVE_WATER metres above the surface along the same view ray. So
    // anything sticking out above the water (the tower tops) is closer and drawn in
    // front of it, as if looking up through clear water. Only as far as needed,
    // because the engine's fog uses this distance (too far = the sky window gets
    // fogged). The engine has no transparency, so this is done with the depth value.
    float pushedBack = min(distance + ABOVE_WATER / max(view.y, 0.05), 88.0);   // stays in front of the background (92 m)
    vec4 far = CameraMatrix * vec4(ViewPosition + view * pushedBack, 1.0);
    gl_FragDepth = (far.z / far.w) * 0.5 + 0.5;
}
