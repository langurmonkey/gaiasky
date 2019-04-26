#version 120

uniform float u_ar;
uniform float u_alpha;
uniform int u_blending = 1;

varying vec4 v_col;
varying float v_depth;

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    float dist = distance(vec2(0.5), uv) * 2.0;
    if(dist > 0.9){
        discard;
    }
    float profile = pow(1.0 - dist, 4.0);
    if(u_blending == 0) {
        // Default blending
        gl_FragColor = vec4(v_col.rgb, v_col.a * u_alpha * profile);
    } else {
        // Additive blending
        gl_FragColor = v_col * u_alpha * profile;
    }

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
