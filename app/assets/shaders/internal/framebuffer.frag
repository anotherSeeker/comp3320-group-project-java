#version 430 core

out vec4 fragColour;
in vec2 UV;

uniform sampler2D screenTexture;

// might consider switching to using hable or uchimura tone mapping functions
vec3 ACESCurve(vec3 x){
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;

    return (x * (a * x + b)) / (x * (c * x + d) + e);
}

void main(){
    vec3 colour = texture(screenTexture,UV).rgb;

    // so i can control the brightness and inputted light that contribuites to the final output.
    float whitepoint = 11.2;
    float exposure = -1.5;

    vec3 w = vec3(whitepoint);
    vec3 whitescale = clamp(ACESCurve(w),0.0,1.0);

    vec3 x = colour * pow(2.0,exposure);

    colour = clamp(ACESCurve(x) / whitescale,0.0,1.0);

    // colour /= colour + vec3(1);
    colour = pow(colour,vec3(1.0/2.2));

    fragColour = vec4(colour,1);
}