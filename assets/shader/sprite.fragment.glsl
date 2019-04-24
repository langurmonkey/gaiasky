#version 120

// v_texCoords are UV coordinates in [0..1]
varying vec2 v_texCoords;
varying vec4 v_color;

uniform sampler2D u_texture0;

vec4 draw() {
    vec4 tex = texture2D(u_texture0, v_texCoords);
    return vec4(tex.rgb * v_color.rgb, 1.0) * v_color.a;
}

void main() {
    gl_FragColor = draw();
    // Debug! - visualise depth buffer
    //gl_FragColor = vec4(vec3(gl_FragCoord.z), 1.0f);
}
