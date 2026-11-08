#version 410 core
in vec2 uv;
in vec3 normal, worldPosition, tint;
in float canopy, colonyPhase, response;
uniform sampler2D Albedo;
uniform vec3 LightDirection, LightColour, ViewPosition;
uniform float Time, LifeStrength;
uniform int Species;
out vec4 fragColour;

float lattice(vec2 p) {
    float a = sin(p.x + sin(p.y * 1.17));
    float b = sin(p.y - cos(p.x * .83));
    return pow(max(0.0, 1.0 - abs(a + b) * 1.8), 5.0);
}

void main() {
    vec3 n = normalize(normal);
    vec3 v = normalize(ViewPosition - worldPosition);
    vec3 l = normalize(LightDirection + vec3(.0001));
    vec3 h = normalize(l + v + vec3(.0001));
    vec3 root = Species == 0 ? vec3(.018,.095,.13) :
                Species == 1 ? vec3(.075,.027,.16) : vec3(.17,.043,.075);
    vec3 body = Species == 0 ? vec3(.07,.47,.44) :
                Species == 1 ? vec3(.38,.13,.56) : vec3(.69,.19,.24);
    vec3 glow = Species == 0 ? vec3(.15,1.0,.78) :
                Species == 1 ? vec3(.56,.32,1.0) : vec3(1.0,.46,.16);
    vec3 pearl = Species == 0 ? vec3(.57,1.0,.89) :
                 Species == 1 ? vec3(1.0,.54,.84) : vec3(1.0,.83,.44);
    float textureDetail = dot(texture(Albedo, uv * vec2(1.0,2.0)).rgb, vec3(.3,.5,.2));
    float heightBlend = smoothstep(.05,.82,canopy);
    vec3 albedo = mix(root, body, heightBlend) * (.76 + textureDetail * .48) * tint;
    // Periodic in U so the tube's duplicated UV seam remains continuous.
    float channel = sin(uv.x * 37.6991 + sin(uv.y * 12.0 + colonyPhase) * .9);
    float veins = pow(max(0.0, 1.0 - abs(channel)), 12.0);
    float pores = pow(max(0.0, sin(uv.x * 62.83185) * sin(uv.y * 67.0)), 12.0);
    float tip = smoothstep(.72, .99, uv.y) * smoothstep(.28,.68,canopy);
    float breath = .73 + .17 * sin(Time * 1.25 - canopy * 4.8 + colonyPhase);
    float traveling = pow(.5 + .5 * sin(canopy * 11.0 - Time * 1.9 + colonyPhase), 8.0);
    float fresnel = pow(1.0 - max(dot(n,v), 0.0), 3.0);
    float diffuse = max(dot(n,l),0.0);
    float wrapped = max((dot(n,l)+.45)/1.45,0.0);
    float backlight = pow(max(dot(-l,v),0.0), 3.0) * (.35 + .65 * canopy);
    float specular = pow(max(dot(n,h),0.0),48.0) * (.35+.65*heightBlend);
    float caustic = lattice(worldPosition.xz * 2.5 + vec2(Time*.19,Time*.13) + worldPosition.y*.6);
    vec3 light = vec3(.10,.21,.29) + LightColour * (.15 + .65 * diffuse);
    vec3 colour = albedo * light;
    colour += body * wrapped * .12 + glow * backlight * .15;
    colour += pearl * (specular*.65 + fresnel*.18 + caustic*.11);
    float luminous = (veins * (.30 + traveling*.45) + pores*.25 + tip*2.2) * heightBlend;
    luminous *= breath * (1.0 + response * 1.6);
    luminous += response * (veins*.9 + tip*1.2 + fresnel*.35);
    colour += LifeStrength * (glow * luminous * 2.0 + pearl * pow(tip,6.0)*.35);
    float distanceFog = 1.0 - exp(-length(worldPosition-ViewPosition)*.014);
    colour = mix(colour,vec3(.012,.052,.082),distanceFog);
    fragColour = vec4(colour,1.0);
}
