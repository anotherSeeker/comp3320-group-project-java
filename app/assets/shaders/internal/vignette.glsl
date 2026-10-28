#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;
uniform float Radius = 0.75; 
uniform float Softness = 0.2;

void main(){

    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec4 colour = imageLoad(inputImage,gid);
    vec2 uv = vec2(gid) / vec2(size);

    vec2 centerDist = uv - 0.5;
    float dist = length(centerDist);

    float vignette = smoothstep(Radius,Radius - Softness,dist);

    colour.rgb *= vignette;
    
    imageStore(outputImage,gid,colour);
}