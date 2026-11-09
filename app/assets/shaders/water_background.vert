#version 430 core
// water_background.vert - a big ball of deep water around the camera (Tanz)
//
// Without it, the engine's sky picture shows wherever there is no scene (past
// the edges of the seabed and water surface). The WaterSurface script scales the
// object to 0 so it casts no shadow; the ball is built here instead, always
// centred on the camera, just inside the engine's 100 m view distance.

layout (location = 0) in vec3 aPos;

uniform mat4 CameraMatrix;   // sent by the engine
uniform vec3 ViewPosition;   // sent by the engine

const float RADIUS = 92.0;

out vec3 direction;   // from the camera outwards

void main() {
    direction = normalize(aPos);
    gl_Position = CameraMatrix * vec4(ViewPosition + direction * RADIUS, 1.0);
}
