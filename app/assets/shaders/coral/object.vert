#version 410 core
layout(location=0) in vec3 aPos;
layout(location=1) in vec3 aNormal;
layout(location=2) in vec2 aUV;
uniform mat4 CameraMatrix, Model;
out vec2 uv;
out vec3 normal;
out vec3 worldPosition;
void main(){ vec4 world=Model*vec4(aPos,1); worldPosition=world.xyz; uv=aUV;
normal=normalize(transpose(inverse(mat3(Model)))*aNormal); gl_Position=CameraMatrix*world; }
