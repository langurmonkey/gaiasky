#version 330 core

// Uniforms
uniform mat4 u_worldTrans;
uniform samplerCube u_diffuseCubemap;
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
    fragColor = texture(u_diffuseCubemap, v_texCoords) * u_opacity;

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
