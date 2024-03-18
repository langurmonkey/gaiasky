#version 330 core

#include <shader/lib/logdepthbuff.glsl>

// UNIFORMS
uniform float u_zfar;
uniform float u_k;
uniform sampler2D u_starTex;

// INPUT
in vec4 v_col;
in vec2 v_uv;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#define saturate(x) clamp(x, 0.0, 1.0)

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#ifdef velocityBufferFlag
#include <shader/lib/velbuffer.frag.glsl>
#endif // velocityBufferFlag

float starTexture(vec2 uv) {
    return texture(u_starTex, uv).r;
}

void main() {
    if (v_col.a <= 0.0) {
        discard;
    }

    vec2 uv = v_uv;
    float profile = starTexture(uv);

    if (profile <= 0.0) {
        discard;
    }
    float alpha = v_col.a * profile;

    // White core
    float core = saturate(1.0 - smoothstep(0.0, 0.04, distance(vec2(0.5), uv) * 2.0));
    // Final color
    fragColor = saturate(alpha * (vec4(v_col.rgb, 1.0) + core * 2.0));
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    // Add outline
    //if (uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif // velocityBufferFlag
}
