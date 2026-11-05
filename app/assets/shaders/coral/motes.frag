#version 410 core
in vec3 colour;
in float opacity;
out vec4 fragColour;
void main() {
    vec2 p=gl_PointCoord*2.0-1.0;
    float r=dot(p,p);
    if(r>1.0) discard;
    float core=exp(-r*8.0);
    float halo=exp(-r*3.0)*.15;
    fragColour=vec4(colour*(core+halo)*opacity*3.0,0.0);
}
