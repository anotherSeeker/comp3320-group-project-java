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
    float perlinLarge = texture(PerlinMap,uv * 0.5 + curl * phase * 0.2).r;
    float perlinMed = texture(PerlinMap,uv * 1.5 + curl * phase * 0.7).r;
    float perlinFine = texture(PerlinMap,uv * 4.0 + curl * phase).r;

    return perlinLarge * 0.55 + perlinMed * 0.3 + perlinFine * 0.15;
}

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 baseColour = imageLoad(rawImage,gid).rgb;
    vec2 uv = (vec2(gid) + 0.5) / vec2(size);

    float rawDepth = texture(DepthBuffer,uv).r;
    if (rawDepth > 0.9999) {
        imageStore(outputImage,gid,vec4(baseColour,1));
        return;
    }


    float phase = Time * CurlStrength;

    vec2 curl = texture(CurlMap,uv).rg * 20;

    float perlin = getFogNoise(uv + vec2(phase,phase * 0.37), curl, phase * 0.1);

    perlin *= perlin;

    float depth = getDepth(uv);
    float density = mix(0.6,1.2,perlin);

    float transmittance = exp(-depth * FogDensity * density);

    vec3 output_colour = mix(FogColour * (1 + perlin * 0.5),baseColour,transmittance); 

    imageStore(outputImage,gid,vec4(output_colour,1));
}