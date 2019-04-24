varying float v_depth;
varying vec4 v_col;

void main() {
    gl_FragColor = v_col;

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}