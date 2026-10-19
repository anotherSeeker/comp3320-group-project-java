#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba8,binding = 0) readonly uniform image2D inputImage;
layout(rgba8,binding = 1) writeonly uniform image2D outputImage;
layout(rgba8,binding = 2) readonly uniform image2D rawImage;

const float[9] vert_kernel = float[](
    -1,-2,-1,
     0, 0, 0,
     1, 2, 1
);

const float[9] hori_kernel = float[](
    -1,0,1,
    -2,0,2,
    -1,0,1
);

float luminance(vec3 colour){
    return dot(colour,vec3(0.2126,0.7152,0.0722));
}

void main(){

    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    float sobelX = 0.0f;
    float sobelY = 0.0f;

    for(int kernelY = 0; kernelY < 3; kernelY++){
        for(int kernelX = 0; kernelX < 3; kernelX++){
            ivec2 sampleOffset = ivec2(kernelX-1,kernelY-1);

            ivec2 samplePosition = clamp(gid + sampleOffset,ivec2(0),size - 1);

            vec3 sampleColour = imageLoad(inputImage,samplePosition).rgb;
            float lum = luminance(sampleColour);

            int kernelIndex = kernelY * 3 + kernelX;
            
            sobelX += lum * hori_kernel[kernelIndex];
            sobelY += lum * vert_kernel[kernelIndex];
        }
    }


    float edge = clamp(length(vec2(sobelX, sobelY)),0,1);

    vec4 colour = vec4(imageLoad(rawImage,gid).rgb * vec3(1 - edge),1);

    imageStore(outputImage,gid,colour);
}