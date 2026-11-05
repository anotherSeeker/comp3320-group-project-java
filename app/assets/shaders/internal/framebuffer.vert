#version 430 core
layout (location = 0) in vec3 aPos;
layout (location = 2) in vec2 aUV;

out vec2 UV;

void main() {
    gl_Position = vec4(aPos.x,aPos.y,0.0,1);
    UV = aUV;
}