#version 430 core
out vec4 fragColour;
in vec3 worldPosition;

uniform sampler2D EquirectangularMap;

const vec2 invAtan = vec2(0.1591,0.3183);

vec2 sampleSphericalMap(vec3 position){
    vec2 uv = vec2(atan(position.z,position.x),asin(position.y));
    uv *= invAtan;
    uv += 0.5;

    return uv;
}

void main(){
    vec2 uv = sampleSphericalMap(normalize(worldPosition));
    vec3 colour = texture(EquirectangularMap,uv);

    fragColour = vec4(colour,1.0);
}