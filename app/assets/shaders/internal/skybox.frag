#version 430 core
out vec4 FragColor;
in vec3 TexCoords;
uniform samplerCube Skybox;
uniform float CoralAtmosphere, Time;
void main() {
    vec3 original=texture(Skybox,TexCoords).rgb;
    vec3 d=normalize(TexCoords);
    float upper=smoothstep(-.15,.8,d.y);
    vec3 ocean=mix(vec3(.012,.052,.082),vec3(.004,.014,.038),upper);
    float rays=pow(max(0.0,sin(d.x*18.0+d.z*7.0+sin(Time*.08)*.35)),14.0);
    rays*=smoothstep(-.15,.25,d.y)*(.5+.5*sin(d.z*9.0+Time*.06));
    ocean+=vec3(.008,.025,.035)*rays;
    FragColor=vec4(mix(original,ocean,CoralAtmosphere),1.0);
}
