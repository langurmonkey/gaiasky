#version 120

#include shader/lib_math.glsl

uniform float u_ar;
uniform float u_alpha;

varying vec4 v_col;
varying float v_depth;
varying float v_dscale;
varying float v_dust;

float programmatic(vec2 uv, float dist) {
    float dist_center = 1.0 - dist;
    return clamp(pow(dist_center, 3.0) * 0.3 + smoothstep(0.8, 1.0, dist_center), 0.0, 1.0);
}

vec4 colorDust(float alpha) {
    return v_col * v_dscale * alpha;
}

vec4 colorStar(float alpha, float dist, vec2 uv) {
    return v_col * v_col.a * programmatic(uv, dist) * v_dscale * alpha;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }

    if (v_dust > 0.5){
        // down
        gl_FragColor = colorDust(u_alpha);
    } else {
        gl_FragColor = colorStar(u_alpha, dist, uv);
    }

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
