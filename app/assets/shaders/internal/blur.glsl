#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;

const float[9] gaussian_kernel = float[](
    1, 2, 1,
    2, 4, 2,
    1, 2, 1
);

void main(){

    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 result = vec3(0.0);

    for(int kernelY = 0; kernelY < 3; kernelY++){
        for(int kernelX = 0; kernelX < 3; kernelX++){
            ivec2 sampleOffset = ivec2(kernelX-1,kernelY-1);

            ivec2 samplePosition = clamp(gid + sampleOffset,ivec2(0),size - 1);

            vec3 sampleColour = imageLoad(inputImage,samplePosition).rgb;

            int kernelIndex = kernelY * 3 + kernelX;
            
            result += sampleColour * vec3(gaussian_kernel[kernelIndex]);
        }
    }

    result /= 16;

    float focusDepth = 0.5;
    float focusRange = 0.15;

    float depth = texelFetch(DepthBuffer,gid,0).r;
    float blurAmmount = clamp(abs(depth - focusDepth) / max(focusRange,0.0001),0,1);

    vec3 sharp = imageLoad(rawImage,gid).rgb;

    vec3 dof = mix(sharp,result,blurAmmount);

    vec4 colour = vec4(dof,1);

    imageStore(outputImage,gid,colour);
}