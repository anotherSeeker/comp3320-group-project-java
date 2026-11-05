#version 430 core

out vec4 fragColour;
in vec2 UV;

uniform sampler2D screenTexture;

uniform float MaxBrightness = 1.0f; 
uniform float Contrast = 1.0f;
uniform float LinSectStart = 0.22f;
uniform float LinSectLength = 0.4f;
uniform float Black = 1.33f;
uniform float Pedestal = 0.0f;

// so i can control the brightness and inputted light that contribuites to the final output.
uniform float Whitepoint = 11.2;
uniform float Exposure = -1.5;

vec3 uchimura(vec3 x){
    float l0 = ((MaxBrightness - LinSectStart) * LinSectLength) / Contrast;
    float s0 = LinSectStart + l0;
    float s1 = LinSectStart + Contrast * l0;

    float c2 = (Contrast * MaxBrightness) / (MaxBrightness - s1);
    float cp = -c2 / MaxBrightness;

    vec3 w0 = 1.0 - smoothstep(0.0,LinSectStart,x);
    vec3 w2 = step(LinSectStart + l0,x);
    vec3 w1 = 1.0 - w0 - w2;

    vec3 toe = LinSectStart * pow(max(x,vec3(0.0)) / LinSectStart, vec3(Black)) + Pedestal;
    vec3 saturation = MaxBrightness - (MaxBrightness - s1) * exp(cp * (x - s0));
    vec3 linear = LinSectStart + Contrast * (x - LinSectStart);

    return toe * w0 + linear * w1 + saturation * w2;
}

void main(){
    vec3 colour = texture(screenTexture,UV).rgb;

    vec3 x = colour * exp2(Exposure);    
    vec3 whitescale = uchimura(vec3(Whitepoint));

    colour = clamp(uchimura(x) / whitescale,0.0,1.0);

    // colour /= colour + vec3(1);
    colour = pow(colour,vec3(1.0/2.2));

    fragColour = vec4(colour,1);
}