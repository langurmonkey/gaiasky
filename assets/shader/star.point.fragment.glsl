#version 120

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform float u_ar;
varying vec4 v_col;

float programmatic(vec2 uv) {
    float dist = 1.0 - min(distance(vec2(0.5), uv) * 2.0, 1.0);
    return pow(dist, 6.0) * 0.5;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    gl_FragColor = v_col * v_col.a * programmatic(uv);
    //gl_FragColor *= 0.95;
}
