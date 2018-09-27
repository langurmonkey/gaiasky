#version 120

/**
 * UNIFORMS
 */
uniform float u_ar;

/**
 * VARYINGS
 */
varying vec4 v_col;

float programmatic(vec2 uv) {
    float dist_center = 1.0 - clamp(distance(vec2(0.5, 0.5), uv) * 2.0, 0.0, 1.0);
    return pow(dist_center, 3.0) * 0.3 + dist_center * 0.05;
}


void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    gl_FragColor = v_col * v_col.a * programmatic(uv);
    //gl_FragColor *= 0.95;
}
