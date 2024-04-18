#version 330 core

#include <shader/lib/math.glsl>
#include <shader/lib/dither8x8.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;

// INPUT
in vec4 v_col;
in vec2 v_uv;
flat in int v_type;
flat in int v_layer;

// Types
#define T_DUST 0
#define T_STAR 1
#define T_BULGE 2
#define T_GAS 3
#define T_HII 4
#define T_GALAXY 5
#define T_POINT 6
#define T_OTHER 7

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#define decay 0.2
#define PI 3.1415927

float programmatic(float dist) {
    return 1.0 - pow(abs(sin(PI * dist / 2.0)), decay);
}

vec4 colorDust(float alpha, float dist) {
    return v_col * (1.0 - pow(max(0.0, abs(dist) * 1.2 - 0.2), 2.0)) * alpha;
}

vec4 colorDustTex(float alpha, vec2 uv) {
    return v_col * texture(u_textures, vec3(uv, v_layer)).r * alpha;
}

vec4 colorStar(float alpha, float dist) {
    return v_col * v_col.a * programmatic(dist) * alpha;
}

vec4 colorTex(float alpha, vec2 uv) {
    return v_col * v_col.a * texture(u_textures, vec3(uv, v_layer)).r * alpha;
}

void main() {
    vec2 uv = v_uv;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }

    if (v_type == T_DUST){
        fragColor = colorDustTex(u_alpha, uv);
        if (fragColor.a < 1.0) {
            if (dither(gl_FragCoord.xy, fragColor.a) < 0.5) {
                discard;
            }
        }
    } else {
        fragColor = colorTex(u_alpha, uv);
    }

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_zfar, u_k);

    // Add outline
    //if (uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag
}
