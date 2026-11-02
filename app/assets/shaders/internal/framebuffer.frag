#version 430 core

out vec4 fragColour;
in vec2 UV;

uniform sampler2D screenTexture;

vec3 uchimura(vec3 x){
    float maxBrightness = 1.0f; 
    float contrast = 1.0f;
    float linSectStart = 0.22f;
    float linSectLength = 0.4f;
    float black = 1.33f;
    float pedestal = 0.0f;

    float l0 = ((maxBrightness - linSectStart) * linSectLength) / contrast;
    float s0 = linSectStart + l0;
    float s1 = linSectStart + contrast * l0;

    float c2 = (contrast * maxBrightness) / (maxBrightness - s1);
    float cp = -c2 / maxBrightness;

    vec3 w0 = 1.0 - smoothstep(0.0,linSectStart,x);
    vec3 w2 = step(linSectStart + l0,x);
    vec3 w1 = 1.0 - w0 - w2;

    vec3 toe = linSectStart * pow(max(x,vec3(0.0)) / linSectStart, vec3(black)) + pedestal;
    vec3 saturation = maxBrightness - (maxBrightness - s1) * exp(cp * (x - s0));
    vec3 linear = linSectStart + contrast * (x - linSectStart);

    return toe * w0 + linear * w1 + saturation * w2;
}

void main(){
    vec3 colour = texture(screenTexture,UV).rgb;

    // so i can control the brightness and inputted light that contribuites to the final output.
    float whitepoint = 11.2;
    float exposure = -1.5;

    vec3 x = colour * exp2(exposure);    
    vec3 whitescale = uchimura(vec3(whitepoint));

    colour = clamp(uchimura(x) / whitescale,0.0,1.0);

    // colour /= colour + vec3(1);
    colour = pow(colour,vec3(1.0/2.2));

    fragColour = vec4(colour,1);
}