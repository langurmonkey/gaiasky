#version 330 core

#include shader/lib_math.glsl
#include shader/lib_dither8x8.glsl

uniform float u_ar;
uniform float u_alpha;

in vec4 v_col;
in float v_depth;
in float v_dscale;
in float v_dust;

out vec4 fragColor;

#define decay 0.2
#define PI 3.1415927

float programmatic(vec2 uv, float dist) {
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), decay);
}

vec4 colorDust(float alpha) {
    return v_col * v_dscale * alpha;
}

vec4 colorStar(float alpha, float dist, vec2 uv) {
    return v_col * v_col.a * programmatic(uv, dist) * v_dscale * alpha;
}


void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }

    if (v_dust > 0.5){
        fragColor = colorDust(u_alpha);
        if(u_alpha < 1.0){
            if(dither(gl_FragCoord.xy, u_alpha) < 0.5)
                discard;
        }
    } else {
        fragColor = colorStar(u_alpha, dist, uv);
    }


    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
