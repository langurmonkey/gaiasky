#version 330 core

#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_falloff;
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in float v_textureIndex;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#define PI 3.1415927

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

vec4 programmatic(float dist) {
    float profile =  1.0 - pow(abs(sin(PI * dist / 2.0)), u_falloff);
    return vec4(v_col.rgb * profile, 1.0) * v_col.a;
}

vec4 textured() {
    vec4 c = texture(u_textures, vec3(v_uv, v_textureIndex));
    return vec4(c.rgb * v_col.rgb, 1.0) * c.a * v_col.a;
}

void main() {
    if (v_textureIndex < 0.0) {
        float dist = distance(vec2(0.5), v_uv) * 2.0;
        if (dist > 1.0) {
            discard;
        }
        fragColor = programmatic(dist);
    } else {
        fragColor = textured();
    }
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    // Add outline
    //if(v_uv.x > 0.99 || v_uv.x < 0.01 || v_uv.y > 0.99 || v_uv.y < 0.01) {
    //    fragColor = vec4(1.0, 0.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer(profile);
    #endif
}
