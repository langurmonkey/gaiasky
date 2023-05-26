#version 330 core

#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_zfar;
uniform float u_k;

// INPUT
in vec4 v_col;
in vec2 v_uv;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

void main() {
    fragColor = v_col * v_col.a;
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer(fragColor.a);
    #endif
}
