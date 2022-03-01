#version 330 core

// Diffuse base texture
uniform sampler2D u_diffuseTexture;
// Grayscale lookup table
uniform sampler2D u_normalTexture;
// Diffuse color
uniform vec4 u_diffuseColor;

// VARYINGS

// Time in seconds
in float v_time;
// Ambient color (star color in this case)
in vec3 v_lightDiffuse;
// The normal
in vec3 v_normal;
// Coordinate of the texture
in vec2 v_texCoords0;
// Opacity
in float v_opacity;
// View vector
in vec3 v_viewVec;
// Color
in vec4 v_color;

layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#define time v_time * 0.003

void main() {
    float softedge = pow(dot(normalize(v_normal), normalize(vec3(v_viewVec))), 2.0) * 1.0;
    softedge = clamp(softedge, 0.0, 1.0);
    fragColor = vec4(u_diffuseColor.rgb, u_diffuseColor.a * (1.0 - v_texCoords0.y) * softedge);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}