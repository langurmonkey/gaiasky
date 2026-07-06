#version 330 core

#include <shader/lib/geometry.glsl>
#include <shader/lib/logdepthbuff.glsl>

// UNIFORMS
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;
// 0-fragColor, 1-layerBuffer
uniform int u_renderTarget;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in float v_textureIndex;

// OUTPUT
layout (location = 0) out vec4 fragColor;
layout (location = 1) out vec4 layerBuffer;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

vec4 programmatic() {
    return v_col * v_col.a;
}

vec4 textured() {
    vec4 c = texture(u_textures, vec3(v_uv, v_textureIndex));
    return vec4(c.rgb * v_col.rgb, 1.0) * c.a * v_col.a;
}

void main() {
    vec4 finalColor;
    if (v_textureIndex < 0.0) {
        // Use color
        finalColor = programmatic();
    } else {
        // Use texture
        finalColor = textured();
    }
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    if (u_renderTarget == 0) {
        fragColor = finalColor;
    } else {
        layerBuffer = finalColor;
    }

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
