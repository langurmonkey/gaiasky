#version 330 core

uniform float u_ar;
uniform float u_falloff;

in vec4 v_col;
in float v_depth;

out vec4 fragColor;

#define PI 3.1415927

float programmatic(vec2 uv) {
    float dist = distance(vec2(0.5), uv) * 2.0;
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), u_falloff);
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    fragColor = vec4(v_col.rgb * programmatic(uv), 1.0) * v_col.a;

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
