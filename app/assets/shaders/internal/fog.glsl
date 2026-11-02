#version 430 core

layout(local_size_x = 8,local_size_y = 8,local_size_z = 1) in;
layout(rgba16f,binding = 0) readonly uniform image2D inputImage;
layout(rgba16f,binding = 1) writeonly uniform image2D outputImage;
layout(rgba16f,binding = 2) readonly uniform image2D rawImage;
layout(rgba16f,binding = 3) uniform image2D advectionImage;

uniform sampler2D DepthBuffer;
uniform sampler2D PerlinMap;
uniform sampler2D CurlMap;

uniform float CurlStrength = 0.5;
uniform float FogDensity = 0.05;

uniform float DeltaTime;

float getDepth(vec2 uv){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    float near = 0.1;
    float far = 100.0;

    float depth = texture(DepthBuffer,uv).r * 2.0 - 1.0;

    return (2.0 * near * far) / (far + near - depth * (far - near));
}

// backwards particle tracing
vec2 advection(ivec2 gid){
    
    vec2 position = imageLoad(advectionImage,gid).rg;

    vec2 velocity = texture(CurlMap,position).rg * 2.0 - 1.0;

    position -= velocity * DeltaTime * CurlStrength;

    return fract(position);
}

void main(){
    ivec2 gid = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(inputImage);

    if(any(greaterThanEqual(gid,size))){
        return;
    }

    vec3 baseColour = imageLoad(rawImage,gid).rgb;
    vec2 uv = (vec2(gid) + 0.5) / vec2(size);

    vec2 flowUV = advection(gid);

    imageStore(advectionImage,gid,vec4(flowUV,0.0,1.0));

    float perlin = texture(PerlinMap,flowUV * 3).r;

    float depth = getDepth(uv);

    float density = mix(0.8,1.2,perlin);

    float fog_factor = exp(-depth*FogDensity * density);

    vec3 output_colour = mix(vec3(1.0),baseColour,fog_factor); 

    imageStore(outputImage,gid,vec4(output_colour,1));
}