#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_zfar;
uniform float u_k;

in vec4 v_col;

layout (location = 0) out vec4 fragColor;

void main() {
    fragColor = v_col;

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);
}