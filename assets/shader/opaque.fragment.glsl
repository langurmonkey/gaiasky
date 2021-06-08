#version 330 core

layout (location = 0) out vec4 fragColor;

#define N_LIGHTS 3
flat in int v_numDirectionalLights;
// Light directions in world space
in vec3 v_directionalLightDir[N_LIGHTS];
// Light colors
in vec3 v_directionalLightColor[N_LIGHTS];
// View direction in world space
in vec3 v_viewDir;
// Logarithmic depth
in float v_depth;
// Fragment position in world space
in vec3 v_fragPosWorld;

// Renders all black for the occlusion testing
void main() {
    fragColor = vec4(0.0, 0.0, 0.0, 1.0);
}
