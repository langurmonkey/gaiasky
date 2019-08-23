#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_ar;
uniform float u_falloff;

in vec4 v_col;
in vec3 v_fragPosView;

out vec4 fragColor;

#define PI 3.1415927

float programmatic(float dist) {
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), u_falloff);
}

void main() {
    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    uv.y = uv.y * u_ar;
    float dist = distance(vec2(0.5, 0.5 * u_ar), uv) * 2.0;
    if(dist > 1.0)
        discard;

    fragColor = vec4(v_col.rgb * programmatic(dist), 1.0) * v_col.a;
    gl_FragDepth = getDepthValue(length(v_fragPosView));
}
