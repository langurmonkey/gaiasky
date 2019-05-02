#version 330 core

in float v_depth;
in vec4 v_col;
out vec4 fragColor;

void main() {
    fragColor = v_col;

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}