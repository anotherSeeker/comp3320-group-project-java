#version 430

layout(vertices = 3) out;

uniform vec3 ViewPosition;
uniform mat4 Model;

const int MIN_TES = 1;
const int MAX_TES = 16;
const float MIN_DIST = 0.0;
const float MAX_DIST = 12.0;

in VS_OUT {
    vec3 position;
    vec3 normal;
    vec2 UV;
    vec3 tangent;
    vec3 bitangent;
} tc_in[];

out TC_OUT {
    vec3 position;
    vec3 normal;
    vec2 UV;
    vec3 tangent;
    vec3 bitangent;
} tc_out[];

void main(){
    tc_out[gl_InvocationID].position = tc_in[gl_InvocationID].position;
    tc_out[gl_InvocationID].normal = tc_in[gl_InvocationID].normal;
    tc_out[gl_InvocationID].UV = tc_in[gl_InvocationID].UV;
    tc_out[gl_InvocationID].tangent = tc_in[gl_InvocationID].tangent;
    tc_out[gl_InvocationID].bitangent = tc_in[gl_InvocationID].bitangent;

    if(gl_InvocationID == 0){
        vec3 center0 = vec3(Model * vec4((tc_in[0].position + tc_in[2].position) / 2.0,1));
        vec3 center1 = vec3(Model * vec4((tc_in[1].position + tc_in[0].position) / 2.0,1));
        vec3 center2 = vec3(Model * vec4((tc_in[2].position + tc_in[1].position) / 2.0,1));

        float dist0 = length(ViewPosition - center0);
        float dist1 = length(ViewPosition - center1);
        float dist2 = length(ViewPosition - center2);

        float tes0 = mix(float(MAX_TES),float(MIN_TES),clamp(dist0 / MAX_DIST,0.0,1.0));
        float tes1 = mix(float(MAX_TES),float(MIN_TES),clamp(dist1 / MAX_DIST,0.0,1.0));
        float tes2 = mix(float(MAX_TES),float(MIN_TES),clamp(dist2 / MAX_DIST,0.0,1.0));

        gl_TessLevelOuter[0] = tes0;
        gl_TessLevelOuter[1] = tes1;
        gl_TessLevelOuter[2] = tes2;
        gl_TessLevelInner[0] = max(tes1,tes2);
    }

}