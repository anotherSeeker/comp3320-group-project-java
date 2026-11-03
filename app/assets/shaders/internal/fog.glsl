#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;
uniform sampler2D PerlinMap;
uniform sampler2D CurlMap;

uniform float CurlStrength = 0.5;
uniform float FogDensity = 0.05;

uniform float Time;
uniform vec3 FogColour = vec3(1.0);

float getDepth(vec2 uv){
    float near = 0.1;
    float far = 100.0;

    float depth = texture(DepthBuffer,uv).r;

    return (near * far) / (far - depth * (far - near));
}

float getFogNoise(vec2 uv,vec2 curl,float phase){
    float perlinLarge = texture(PerlinMap,uv * 0.5 + curl * 0.02 + vec2(phase * 0.2,phase * 0.1)).r;
    float perlinMed = texture(PerlinMap,uv * 1.5 + curl * 0.04 + vec2(phase * 0.4,phase * 0.2)).r;
    float perlinFine = texture(PerlinMap,uv * 4.0 + curl * 0.06 + vec2(phase * 0.6,phase * 0.3)).r;

    return perlinLarge * 0.55 + perlinMed * 0.3 + perlinFine * 0.15;
}

vec3 getBlurredColour(ivec2 pixel, float blurRadius){

    ivec2 size = imageSize(inputImage);

    float[7] kernel = float[](0,1,4,7,4,1,0);

    float sumWeight = 0;
    vec3 result = vec3(0.0);

    for(int y = -3;y <= 3; y++){
        for(int x = -3;x <= 3; x++){
            vec2 offset = vec2(x,y) * blurRadius;
            ivec2 samplePosition = clamp(pixel + ivec2(offset),ivec2(0),size - 1);

            float weight = kernel[x + 3] * kernel[y + 3];

            vec3 colour = imageLoad(inputImage,samplePosition).rgb;
            result += colour * weight;
            sumWeight += weight;
        }
    }

    return result / sumWeight;
}

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 baseColour = imageLoad(inputImage,gid).rgb;
    vec2 uv = (vec2(gid) + 0.5) / vec2(size);

    // float rawDepth = texture(DepthBuffer,uv).r;
    // if (rawDepth > 0.9999) {
    //     imageStore(outputImage,gid,vec4(baseColour,1));
    //     return;
    // }


    float phase = Time * CurlStrength;

    vec2 curl = texture(CurlMap,uv).rg * 20;

    float perlin = getFogNoise(uv, curl, phase);

    perlin *= perlin;

    float depth = getDepth(uv);
    float density = mix(0.6,1.2,perlin);

    float transmittance = exp(-depth * FogDensity * density);

    int blurRadius = int((1 - transmittance) * 4.0);

    vec3 blurredColour = getBlurredColour(gid,blurRadius * 10);

    baseColour = mix(baseColour,blurredColour,(1 - transmittance));

    vec3 output_colour = mix(FogColour * (1.0 + perlin * 0.5),baseColour,transmittance); 

    imageStore(outputImage,gid,vec4(output_colour,1));
}