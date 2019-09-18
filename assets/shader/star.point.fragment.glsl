#version 330 core

uniform float u_ar;

in vec4 v_col;

layout (location = 0) out vec4 fragColor;

float programmatic(vec2 uv) {
    float dist = 1.0 - min(distance(vec2(0.5), uv) * 2.0, 1.0);
    return pow(dist, 6.0) * 0.5;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    fragColor = v_col * v_col.a * programmatic(uv);
}
