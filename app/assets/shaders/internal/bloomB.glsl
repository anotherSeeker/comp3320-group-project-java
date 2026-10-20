#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;


void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 clipped = imageLoad(inputImage,gid).rgb;
    vec3 baseColour = imageLoad(rawImage,gid).rgb;

    vec3 bloomColour = baseColour + clipped;
    imageStore(outputImage,gid,vec4(bloomColour,1));
}