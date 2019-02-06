#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

varying vec4 v_col;
void main() {
    gl_FragColor = v_col;
}