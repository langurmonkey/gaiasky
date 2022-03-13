#version 330 core

// Uniforms
uniform samplerCube u_environmentCubemap;
uniform float u_opacity;

// INPUT
in vec3 v_texCoords;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

void main() {
    fragColor = texture(u_environmentCubemap, v_texCoords) * u_opacity;

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
