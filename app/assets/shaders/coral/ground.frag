#version 410 core
in vec2 uv;
in vec3 normal, worldPosition;
uniform vec3 LightDirection, ViewPosition, CoralTint;
uniform float Time;
out vec4 fragColour;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.0-2.0*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+1.0),f.x),f.y);}
void main(){
    vec2 p=worldPosition.xz;
    float grain=noise(p*9.0)*.25+noise(p*.8)*.75;
    vec3 sand=mix(vec3(.018,.045,.067),vec3(.065,.13,.16),grain);
    vec2 q=p*1.4+vec2(Time*.13,Time*.09);
    float network=sin(q.x+sin(q.y*1.4))+sin(q.y-cos(q.x*1.2));
    float caustics=pow(max(0.0,1.0-abs(network)*1.7),7.0);
    float pools=noise(p*.18+4.0);
    sand+=vec3(.045,.21,.23)*caustics*(.25+pools*.5);
    sand+=vec3(.035,.015,.075)*pow(pools,3.0);
    float light=.55+.45*max(dot(normalize(normal),normalize(LightDirection+vec3(.0001))),0.0);
    float fog=1.0-exp(-length(worldPosition-ViewPosition)*.014);
    fragColour=vec4(mix(sand*light,vec3(.012,.052,.082),fog),1.0);
}
