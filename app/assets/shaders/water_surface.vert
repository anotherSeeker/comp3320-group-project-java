#version 430 core
// water_surface.vert - builds the water plane (Tanz)
//
// The WaterSurface script scales the object to 0 so it casts no shadow, so the
// plane is built here instead: the quad's corners (-1..1) are spread out to
// WATER_HALF_SIZE metres around the object's position.

layout (location = 0) in vec3 aPos;

uniform mat4 Model;          // sent by the engine (only its position is used)
uniform mat4 CameraMatrix;   // sent by the engine

const float WATER_HALF_SIZE = 45.0;   // inside the engine's 100 m view distance

out vec3 worldPosition;

void main() {
    vec3 centre = Model[3].xyz;   // the object's position (not affected by the scale)
    worldPosition = centre + vec3(aPos.x, 0.0, -aPos.y) * WATER_HALF_SIZE;   // lie flat

    gl_Position = CameraMatrix * vec4(worldPosition, 1.0);
}
