#version 120

uniform float u_ar;
uniform float u_profileDecay;

varying vec4 v_col;
varying float v_depth;

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    gl_FragColor = vec4(v_col.rgb + pow(1.0 - dist, 3.0), 1.0) * v_col.a * pow(1.0 - dist, u_profileDecay);

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
