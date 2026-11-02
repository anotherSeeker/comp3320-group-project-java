#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;

uniform sampler2D DepthBuffer;

float getDepth(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    float near = 0.1;
    float far = 100.0;

    float depth = texture(DepthBuffer,(vec2(gid) + 0.5) / vec2(size)).r * 2.0 - 1.0;

    return (2.0 * near * far) / (far + near - depth * (far - near));
}

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 baseColour = imageLoad(rawImage,gid).rgb;

    float fog_density = 0.05;

    float depth = getDepth();

    float fog_factor = exp(-depth*fog_density);

    vec3 output_colour = mix(vec3(1.0),baseColour,fog_factor); 

    imageStore(outputImage,gid,vec4(output_colour,1));
}