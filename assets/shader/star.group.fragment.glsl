#version 330 core

#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_ar;
uniform float u_zfar;
uniform float u_k;

// INPUT
in vec4 v_col;


// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

float programmatic(vec2 uv) {
    float dist_center = 1.0 - clamp(distance(vec2(0.5, 0.5 * u_ar), uv) * 2.0, 0.0, 1.0);
    return pow(dist_center, 3.0) * 0.3 + dist_center * 0.05;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    uv.y = uv.y * u_ar;
    float profile = programmatic(uv);
    float alpha = v_col.a * profile;

    if(alpha <= 0.0){
        discard;
    }

    fragColor = v_col * alpha;
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
