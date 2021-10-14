#version 330 core

#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_ar;
uniform float u_falloff;
uniform float u_zfar;
uniform float u_k;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in vec4 v_vcol;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#define PI 3.1415927

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

float programmatic(float dist) {
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), u_falloff);
}

void main() {
    vec2 uv = v_uv;
    uv.y = uv.y * u_ar;
    float dist = distance(vec2(0.5, 0.5 * u_ar), uv) * 2.0;
    if(dist > 1.0) {
        discard;
    }

    float profile = programmatic(dist);

    fragColor = vec4(v_col.rgb * profile, 1.0) * v_col.a;
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    // Add outline
    if(uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
        //fragColor = v_vcol;
    }

    #ifdef velocityBufferFlag
    velocityBuffer(profile);
    #endif
}
