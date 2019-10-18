#version 330 core

#include shader/lib_math.glsl
#include shader/lib_dither8x8.glsl
#include shader/lib_logdepthbuff.glsl

uniform float u_ar;
uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;

in vec4 v_col;
in float v_dust;

layout (location = 0) out vec4 fragColor;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

#define decay 0.2
#define PI 3.1415927

float programmatic(float dist) {
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), decay);
}

vec4 colorDust(float alpha, float dist) {
    return v_col * (1.0 - pow(max(0.0, abs(dist) * 1.2 - 0.2), 2.0)) * alpha;
}

vec4 colorStar(float alpha, float dist) {
    return v_col * v_col.a * programmatic(dist) * alpha;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    uv.y = uv.y * u_ar;
    float dist = min(1.0, distance(vec2(0.5, 0.5 * u_ar), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }

    if (v_dust > 0.5){
        fragColor = colorDust(u_alpha, dist);
        if(fragColor.a < 1.0){
            if(dither(gl_FragCoord.xy, fragColor.a) < 0.5)
                discard;
        }
    } else {
        fragColor = colorStar(u_alpha, dist);
    }

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef velocityBufferFlag
    velocityBuffer(programmatic(dist));
    #endif
}
