#version 330 core

#include shader/lib_logdepthbuff.glsl

in vec4 v_col;
out vec4 fragColor;

void main() {
    fragColor = v_col;

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue();
}