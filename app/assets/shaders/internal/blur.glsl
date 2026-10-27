#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;

const float[25] gaussian_kernel = float[](
    1,  4,  6,  4, 1,
    4, 16, 24, 16, 4,
    6, 24, 36, 24, 6,
    4, 16, 24, 16, 4,
    1,  4,  6,  4, 1
);

const int KERNEL_WIDTH = 5;
const int KERNEL_HEIGHT = 5;

void main(){

    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 result = vec3(0.0);

    for(int kernelY = 0; kernelY < KERNEL_HEIGHT; kernelY++){
        for(int kernelX = 0; kernelX < KERNEL_WIDTH; kernelX++){
            ivec2 sampleOffset = ivec2(kernelX-2,kernelY-2);

            ivec2 samplePosition = clamp(gid + sampleOffset,ivec2(0),size - 1);

            vec3 sampleColour = imageLoad(inputImage,samplePosition).rgb;

            int kernelIndex = kernelY * KERNEL_WIDTH + kernelX;
            
            result += sampleColour * vec3(gaussian_kernel[kernelIndex]);
        }
    }

    result /= 256;

    vec4 colour = vec4(result,1);

    imageStore(outputImage,gid,colour);
}