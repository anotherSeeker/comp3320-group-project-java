#version 410 core
layout(location=0) in vec3 Origin;
layout(location=1) in float Phase;
layout(location=2) in float Species;
uniform mat4 CameraMatrix;
uniform vec3 ViewPosition, PlayerPosition, WakePosition, PlayerVelocity;
uniform vec3 PulseOrigin;
uniform float Time, Growth, ViewHeight, InteractionStrength, PulseAge;
out vec3 colour;
out float opacity;
void main() {
    float age=fract(Time*(.075+.012*sin(Phase))+Phase);
    float angle=Phase*6.28318+Time*.27;
    vec3 p=Origin;
    p.y=p.y*Growth + age*2.4;
    p.xz+=vec2(sin(angle),cos(angle*.83))*(.25+age*.55);
    vec3 away=p-PlayerPosition-vec3(0,.8,0);
    float response=exp(-dot(away,away)*.2)*InteractionStrength;
    p+=away*response*.12;
    p.xz+=PlayerVelocity.xz*.08*exp(-dot(p-WakePosition,p-WakePosition)*.3)*InteractionStrength;
    float radius=length(p.xz-PulseOrigin.xz);
    float front=radius-PulseAge*6.0;
    float pulse=exp(-front*front)*step(0.0,PulseAge)*(1.0-smoothstep(8.0,10.0,PulseAge));
    colour=Species<.5?vec3(.12,1.0,.72):Species<1.5?vec3(.61,.35,1.0):vec3(1.0,.57,.2);
    colour=mix(colour,vec3(.8,1.0,1.0),response*.3);
    opacity=sin(age*3.14159)*(.55+.2*sin(Time*1.1+Phase*9.0));
    opacity*=smoothstep(.1,.8,Growth)*exp(-length(p-ViewPosition)*.025)*(1.0+pulse+response);
    gl_Position=CameraMatrix*vec4(p,1.0);
    gl_PointSize=clamp(ViewHeight*.020/max(length(p-ViewPosition),.1),2.0,16.0);
}
