#version 330 core

#include shader/lib_logdepthbuff.glsl
//#include shader/lib_star.glsl

// UNIFORMS
uniform float u_ar;
uniform float u_zfar;
uniform float u_k;
uniform sampler2D u_starTex;

// INPUT
in vec4 v_col;


// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

float starTexture(vec2 uv) {
    return texture(u_starTex, uv).r;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    uv.y = uv.y * u_ar;
    float profile = starTexture(uv);
    float alpha = v_col.a * profile;

    if(alpha <= 0.0){
        discard;
    }

    fragColor = alpha * v_col;
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
