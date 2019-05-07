#version 330 core

uniform float u_ar;
uniform float u_falloff;
uniform float u_glowFactor;

in vec4 v_col;
in float v_depth;

out vec4 fragColor;

float programmatic(vec2 uv) {
    float dist = distance(vec2(0.5), uv) * 2.0;
    float core = pow(smoothstep(0.6, 0.1, dist), u_falloff);
    float glow = smoothstep(1.0, 0.0, dist) * u_glowFactor;
    return max(core, glow);
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    fragColor = vec4(v_col.rgb * programmatic(uv), 1.0) * v_col.a;

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
