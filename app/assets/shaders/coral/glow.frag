#version 410 core
in vec2 UV;
out vec4 fragColour;
uniform sampler2D Source;
uniform int Pass;
uniform float TexelX, TexelY;
void main() {
    vec2 direction = Pass == 1 ? vec2(TexelX,0) : vec2(0,TexelY);
    vec3 colour;
    if (Pass == 0) {
        // Four taps suppress flicker when small emissive tips enter a reduced pixel.
        vec2 d = vec2(TexelX,TexelY)*.5;
        colour = (texture(Source,UV+d).rgb+texture(Source,UV-d).rgb+
                  texture(Source,UV+vec2(d.x,-d.y)).rgb+texture(Source,UV+vec2(-d.x,d.y)).rgb)*.25;
        float brightness = max(colour.r,max(colour.g,colour.b));
        float soft = clamp(brightness-.65,0.0,.8);
        float contribution = max(brightness-1.05,soft*soft/3.2);
        colour *= contribution / max(brightness,.0001);
    } else {
        colour = texture(Source,UV).rgb * .227027;
        colour += (texture(Source,UV+direction*1.384615).rgb+texture(Source,UV-direction*1.384615).rgb)*.316216;
        colour += (texture(Source,UV+direction*3.230769).rgb+texture(Source,UV-direction*3.230769).rgb)*.070270;
    }
    fragColour=vec4(colour,1.0);
}
