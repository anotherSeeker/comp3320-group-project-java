#version 430

layout(vertices = 3) out;

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
        gl_TessLevelOuter[0] = 16.0;
        gl_TessLevelOuter[1] = 16.0;
        gl_TessLevelOuter[2] = 16.0;
        gl_TessLevelInner[0] = 16.0;
    }

}