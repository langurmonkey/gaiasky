#version 330 core

#include shader/lib_logdepthbuff.glsl
//#include shader/lib_star.glsl

// UNIFORMS
uniform float u_zfar;
uniform float u_k;
uniform sampler2D u_starTex;

// INPUT
in vec4 v_col;
in vec2 v_uv;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

float starTexture(vec2 uv) {
    return texture(u_starTex, uv).r;
}

void main() {
    vec2 uv = v_uv;
    float profile = starTexture(uv);
    float alpha = v_col.a * profile;

    if(alpha <= 0.0){
        discard;
    }

    // White core
    float core = 1.0 - smoothstep(0.0, 0.04, distance(vec2(0.5), uv) * 2.0);
    // Final color
    fragColor = clamp(alpha * (v_col + core * 2.0), 0.0, 1.0);
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    // Add outline
    //if (uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
