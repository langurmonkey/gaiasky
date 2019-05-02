#version 330 core

in vec4 v_col;
in vec2 v_uv;
in float v_depth;
out vec4 fragColor;

void main() {
    // Distance from the middle of the line in [0..1]
    // Middle is 0, edge is 1
    float alpha = pow(1.0 - 2.0 * abs(v_uv.y - 0.5), 4.0);

    fragColor = vec4(v_col.rgb, 1.0) * v_col.a * alpha;
    gl_FragDepth = v_depth;
}