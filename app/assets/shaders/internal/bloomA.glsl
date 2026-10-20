#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;

float luminance(vec3 colour){
    return dot(colour,vec3(0.2126,0.7152, 0.0722));
}

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec4 baseColour = imageLoad(inputImage,gid);

    float lum = luminance(baseColour.rgb);

    vec4 hdrValue;

    float threshold = 1.0;
    float knee = 0.5;

    float soft = clamp(lum - threshold + knee,0,2.0 * knee);
    soft = soft * soft / (4.0 * knee + 0.0001);

    float contrib = max(lum - threshold, soft);

    vec3 bloom = baseColour.rgb * contrib / max(lum,0.0001);

    imageStore(outputImage,gid,vec4(bloom,1));
}
