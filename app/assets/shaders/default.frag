#version 430 core

in vec3 position;
in vec3 normal;
in vec2 UV;

out vec4 fragColour;

uniform sampler2D Albedo;
uniform vec3 LightDirection;
uniform vec3 ViewPosition;

void main() {
    float ambient = 0.2;
    float diffuseStrength = 0.8;
    float specularStrength = 0.5;
    float specularSharpness = 32;

    vec4 baseColour = texture(Albedo,UV);

    vec3 norm = normalize(normal);
    vec3 lightDirection = normalize(LightDirection);
    vec3 viewDirection = normalize(ViewPosition - position);
    vec3 halfway = normalize(lightDirection + viewDirection);

    float diffuse = max(dot(norm,lightDirection),0.0f) * diffuseStrength;
    float specular = pow(max(dot(norm,halfway),0.0f),specularSharpness) * specularStrength;

    vec3 lighting = (diffuse + ambient ) * baseColour.rgb + specular;

    fragColour = vec4(lighting,1);
}