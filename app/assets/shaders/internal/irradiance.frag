#version 430 core
#define PI 3.14159265359

out vec4 fragColour;
in vec3 worldPosition;

uniform samplerCube EnvironmentMap;

void main(){
    vec3 normalised = normalize(worldPosition);

    vec3 irradiance = vec3(0.0);
    vec3 right = cross(vec3(0,1,0),normalised);
    vec3 up = cross(normalised,right);

    float sampleDelta = 0.025;
    float nrSamples = 0.0;

    for(int phi = 0; phi < 2.0 * PI; phi += sampleDelta){
        for(int theta = 0; theta < 0.5 * PI; theta += sampleDelta){
            vec3 tangentSample = vec3(sin(theta) * cos(phi), sin(theta) * sin(phi) - cos(theta));

            vec3 sampleVector = tangentSample.x * right + tangentSample.y * up + tangentSample.z * normalised;

            irradiance += texture(EnvironmentMap,sampleVector).rgb * cos(theta) * sin(theta);
            nrSamples++;
        }
    }

    irradiance *= PI / nrSamples;

    fragColour = vec4(irradiance,1.0)
}