#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;
uniform sampler1D ColourRamp;

const float RESOLUTION = 256 - 1;

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 baseColour = imageLoad(inputImage,gid).rgb;

    float maxBaseCc = max(max(baseColour.r,baseColour.g),baseColour.b);

    vec3 sdr = baseColour / maxBaseCc;
    sdr = clamp(sdr,0.0,1.0);

    vec3 sdrGraded = vec3(
        texture(ColourRamp,sdr.r).r,
        texture(ColourRamp,sdr.g).g,
        texture(ColourRamp,sdr.b).b
    );

    vec3 result = sdrGraded * maxBaseCc;

    imageStore(outputImage,gid,vec4(result,1));
}