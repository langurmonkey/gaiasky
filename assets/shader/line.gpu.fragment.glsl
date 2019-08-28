#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;

in vec4 v_col;
out vec4 fragColor;

void main() {
    fragColor = vec4(v_col.rgb, v_col.a * u_alpha);
    gl_FragDepth = getDepthValue(u_zfar, u_k);
}
