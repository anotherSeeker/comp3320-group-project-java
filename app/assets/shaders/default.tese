#version 430

in TC_OUT {
    vec3 position;
    vec3 normal;
    vec2 UV;
    vec3 tangent;
    vec3 bitangent;
} te_in[];

uniform mat4 CameraMatrix;
uniform mat4 Model;

uniform sampler2D DisplacementMap;

out vec3 position;
out vec3 normal;
out vec2 UV;
out vec3 tangent;
out vec3 bitangent;

out mat3 TBN;

void main(){
    vec3 barycentric = gl_TessCoord;

    vec3 localPosition = te_in[0].position * barycentric.x 
                       + te_in[1].position * barycentric.y 
                       + te_in[2].position * barycentric.z;

    vec3 localNormal = te_in[0].normal * barycentric.x 
                     + te_in[1].normal * barycentric.y 
                     + te_in[2].normal * barycentric.z;

    UV = te_in[0].UV * barycentric.x 
       + te_in[1].UV * barycentric.y 
       + te_in[2].UV * barycentric.z;


    vec3 localTangent = te_in[0].tangent * barycentric.x 
                      + te_in[1].tangent * barycentric.y 
                      + te_in[2].tangent * barycentric.z;


    vec3 localBitangent = te_in[0].bitangent * barycentric.x 
                      + te_in[1].bitangent * barycentric.y 
                      + te_in[2].bitangent * barycentric.z;


    localNormal = normalize(localNormal);

    float height = texture(DisplacementMap,UV).r;
    
    localPosition += localNormal * height * 0.1;
    
    vec4 worldPosition = (Model * vec4(localPosition,1));
    position = worldPosition.xyz;

    normal = normalize(mat3(transpose(inverse(Model))) * localNormal);
    tangent = normalize(mat3(Model) * localTangent);
    bitangent = normalize(mat3(Model) * localBitangent);

    TBN = mat3(tangent,bitangent,normal);

    gl_Position = CameraMatrix * Model * vec4(displaced,1);
}