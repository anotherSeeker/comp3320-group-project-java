#version 430

layout (location=0) in vec3 position;
layout (location=2) in vec2 texCoord;

out vec2 outTexCoord;

uniform mat4 projectionMatrix;
uniform mat4 modelMatrix;

void main()
{
    gl_Position = /*projectionMatrix * modelMatrix * */ vec4(position/4, 1.0);
    outTexCoord = texCoord;
}

