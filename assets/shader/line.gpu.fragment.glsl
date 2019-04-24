uniform float u_alpha;
varying float v_depth;

varying vec4 v_col;

void main() {
    gl_FragColor = vec4(v_col.rgb, v_col.a * u_alpha);

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
