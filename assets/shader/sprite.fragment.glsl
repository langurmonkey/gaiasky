#version 330 core

#include shader/lib_logdepthbuff.glsl

uniform sampler2D u_texture0;
uniform float u_zfar;
uniform float u_k;

// v_texCoords are UV coordinates in [0..1]
in vec2 v_texCoords;
in vec4 v_color;

layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 velMap;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

vec4 draw() {
    vec4 tex = texture(u_texture0, v_texCoords);
    return vec4(tex.rgb * v_color.rgb, 1.0) * v_color.a;
}

void main() {
    fragColor = draw();

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    velMap = vec4(0.0);
}
