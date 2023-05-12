#version 330 core

#include shader/lib_logdepthbuff.glsl

// UNIFORMS
uniform float u_ar;
uniform float u_falloff;
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;

// INPUT
in vec4 v_col;
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

vec4 textured(vec2 uv) {
    vec4 c = texture(u_textures, vec3(uv, v_textureIndex));
    return vec4(c.rgb * v_col.rgb, 1.0) * c.a * v_col.a;
}

void main() {
    vec2 uv = vec2(gl_PointCoord.x, gl_PointCoord.y);
    uv.y = uv.y * u_ar;

    if (v_textureIndex < 0.0) {
        float dist = distance(vec2(0.5, 0.5 * u_ar), uv) * 2.0;
        if (dist > 1.0) {
            discard;
        }
        fragColor = programmatic(dist);
    } else {
        fragColor = textured(uv);
    }

    gl_FragDepth = getDepthValue(u_zfar, u_k);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer(profile);
    #endif
}
