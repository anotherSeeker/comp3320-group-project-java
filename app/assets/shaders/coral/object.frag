#version 410 core
in vec2 uv;
in vec3 normal;
in vec3 worldPosition;
uniform sampler2D Albedo;
uniform vec3 CoralTint, LightDirection, ViewPosition;
out vec4 fragColour;
void main(){ vec3 colour=texture(Albedo,uv).rgb*CoralTint;
float light=.6+.4*max(dot(normalize(normal),normalize(LightDirection)),0);
float fog=1-exp(-length(worldPosition-ViewPosition)*.006);
fragColour=vec4(mix(colour*light,vec3(.035,.13,.17),fog),1); }
