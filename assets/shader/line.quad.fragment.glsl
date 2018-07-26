#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

varying vec4 v_col;
varying vec2 v_uv;

void main() {
    // Distance from the middle of the line in [0..1]
    // Middle is 0, edge is 1
    float alpha = 1.0 - 2.0 * abs(v_uv.y - 0.5);
    // Adding alpha^4 to the color will make the center of the line withe-ish
    gl_FragColor = vec4(v_col.rgb, 1.0) * alpha * v_col.a;
    // Debug UV
    //gl_FragColor = vec4(0.0, v_uv.y, 0.0, 1.0);
}