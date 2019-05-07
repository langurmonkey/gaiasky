#version 330 core

// UNIFORMS
uniform float u_ar;

// INPUT
in vec4 v_col;
in float v_depth;

// OUTPUT
layout (location = 0) out vec4 fragColor;


float programmatic(vec2 uv) {
    float dist_center = 1.0 - clamp(distance(vec2(0.5, 0.5), uv) * 2.0, 0.0, 1.0);
    return pow(dist_center, 3.0) * 0.3 + dist_center * 0.05;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
    uv.y = uv.y / u_ar;
    float alpha = v_col.a * programmatic(uv);

    if(alpha <= 0.0){
        discard;
    }

    fragColor = v_col * alpha;
    gl_FragDepth = v_depth;
}
