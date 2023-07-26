#version 330 core

#include shader/lib_geometry.glsl
#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in float v_textureIndex;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif // velocityBufferFlag

vec4 programmatic() {
    return v_col * v_col.a;
}

vec4 textured() {
    vec4 c = texture(u_textures, vec3(v_uv, v_textureIndex));
    return vec4(c.rgb * v_col.rgb, 1.0) * c.a * v_col.a;
}

void main() {
    if (v_textureIndex < 0.0) {
        // Use color
        fragColor = programmatic();
    } else {
        // Use texture
        fragColor = textured();
    }
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer(fragColor.a);
    #endif // velocityBufferFlag
}