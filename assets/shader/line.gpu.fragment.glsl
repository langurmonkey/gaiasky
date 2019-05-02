#version 330 core

uniform float u_alpha;

in float v_depth;
in vec4 v_col;
out vec4 fragColor;

void main() {
    fragColor = vec4(v_col.rgb, v_col.a * u_alpha);
    gl_FragDepth = v_depth;
}
