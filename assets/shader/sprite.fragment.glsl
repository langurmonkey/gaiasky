#version 120

// v_texCoords are UV coordinates in [0..1]
varying vec2 v_texCoords;
varying vec4 v_color;
varying float v_depth;

uniform sampler2D u_texture0;

vec4 draw() {
    vec4 tex = texture2D(u_texture0, v_texCoords);
    return vec4(tex.rgb * v_color.rgb, 1.0) * v_color.a;
}

void main() {
    gl_FragColor = draw();

    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}
