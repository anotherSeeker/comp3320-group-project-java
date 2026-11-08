#version 410 core
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
layout(location=5) in vec4 instancePositionScale;
layout(location=6) in vec4 instanceRotationPhase;
layout(location=7) in vec3 instanceTint;
uniform mat4 CameraMatrix;
uniform float NormalDetail;
mat4 Model;
float phaseOffset;
uniform float SwayBaseHeight;
uniform float Time, SwayHeight, SwayStrength, SwayPhase, Growth;
uniform vec3 PlayerPosition, WakePosition, PlayerVelocity;
uniform float PlayerRadius, PlayerHeight, InteractionStrength;
out vec2 uv;
out vec3 worldPosition;
out vec3 tint;
out vec3 normal;
out float canopy, colonyPhase, response;
uniform vec3 PulseOrigin;
uniform float PulseAge, LifeStrength;

float pulseAt(vec3 world) {
    float radius = length(world.xz - PulseOrigin.xz);
    float front = radius - PulseAge * 6.0;
    return exp(-front * front * 1.8) * exp(-radius * .045) *
           step(0.0,PulseAge) * (1.0-smoothstep(8.0,10.0,PulseAge));
}

// Distance to a vertical player capsule: reacts to the body, including swimming height.
float influence(vec3 world, vec3 feet) {
    vec3 closest = feet + vec3(0.0, clamp(world.y - feet.y, 0.0, PlayerHeight), 0.0);
    return 1.0 - smoothstep(0.0, max(PlayerRadius, 0.001), length(world - closest));
}

vec3 displacement(vec3 p) {
    vec3 world = (Model * vec4(p * Growth, 1.0)).xyz;
    // The thick primary trunk stays rigid; upper branches retain water/contact motion.
    float base = max(0.1, SwayBaseHeight);
    float h = clamp((p.y - base) / max(SwayHeight - base, 0.001), 0.0, 1.0);
    float flex = h * h;
    // Smooth spatial waves give separate tips different phases without opening seams.
    // Nearby points share the same field; there is no random per-vertex jitter.
    float phase = (SwayPhase + phaseOffset) + world.x * 1.35 + world.z * 1.10 + p.y * 0.65;
    vec2 current = vec2(sin(Time * 0.62 + SwayPhase + phaseOffset + world.z * 0.35),
                        cos(Time * 0.48 + (SwayPhase + phaseOffset) + world.x * 0.30));
    vec2 flutter = vec2(sin(Time * 1.45 + phase), cos(Time * 1.17 + phase * 1.13));
    // Slow tide, smaller tip flutter and a coherent pulse response; roots stay fixed.
    vec2 offset = SwayStrength * flex * (current * vec2(.85,.62) + flutter * .22);
    float pulse = pulseAt(world) * LifeStrength;
    vec2 pulseDirection = world.xz - PulseOrigin.xz;
    pulseDirection /= sqrt(dot(pulseDirection,pulseDirection)+.1);
    offset += pulseDirection * pulse * flex * .18;
    if (InteractionStrength > 0.0) {
        vec2 away = world.xz - PlayerPosition.xz;
        // Soft normalization stays finite even if a vertex is exactly at the player centre.
        away /= sqrt(dot(away, away) + 0.04);
        vec2 wake = PlayerVelocity.xz * 0.12;
        offset += InteractionStrength * flex *
            (away * influence(world, PlayerPosition) + wake * influence(world, WakePosition));
    }
    return vec3(offset.x, 0.0, offset.y) * Growth;
}

void main() {
    float c=instanceRotationPhase.x, s=instanceRotationPhase.y, scale=instancePositionScale.w;
    Model=mat4(vec4(c*scale,0,-s*scale,0),vec4(0,scale,0,0),vec4(s*scale,0,c*scale,0),vec4(instancePositionScale.xyz,1));
    phaseOffset=instanceRotationPhase.z; tint=instanceTint;
    colonyPhase=phaseOffset;
    canopy=clamp(aPos.y/max(SwayHeight,.001),0.0,1.0);
    vec3 world = (Model * vec4(aPos * Growth, 1.0)).xyz + displacement(aPos);
    mat3 frame = mat3(Model);
    if (NormalDetail > 0.5 && (SwayStrength > 0.0 || InteractionStrength > 0.0 || (PulseAge >= 0.0 && LifeStrength > 0.0))) {
        // Differentiate only the displacement, adding the exact model derivative, so
        // normals follow both sideways bending directions and model transforms.
        const float e = 0.002;
        vec3 dx = (displacement(aPos + vec3(e,0,0)) - displacement(aPos - vec3(e,0,0))) / (2.0*e);
        vec3 dy = (displacement(aPos + vec3(0,e,0)) - displacement(aPos - vec3(0,e,0))) / (2.0*e);
        vec3 dz = (displacement(aPos + vec3(0,0,e)) - displacement(aPos - vec3(0,0,e))) / (2.0*e);
        mat3 deformedFrame = mat3(Model) * Growth + mat3(dx,dy,dz);
        if (abs(determinant(deformedFrame)) > 1e-9) frame = deformedFrame;
    }
    normal = normalize(transpose(inverse(frame)) * aNormal);
    uv = aUV;
    worldPosition=world;
    response = InteractionStrength * (influence(world,PlayerPosition) +
               .6 * influence(world,WakePosition) * min(length(PlayerVelocity)*.25,1.0))
               + pulseAt(world) * LifeStrength;
    gl_Position = CameraMatrix * vec4(world, 1.0);
}
