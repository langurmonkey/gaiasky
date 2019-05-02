#version 330 core

uniform float u_ar;

in vec4 v_col;
out vec4 fragColor;

void main() {
    float alpha = v_col.a;
    fragColor = vec4(v_col.rgb * alpha, alpha);
}
