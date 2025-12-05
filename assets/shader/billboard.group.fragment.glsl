#version 330 core

#include <shader/lib/math.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform mat4 u_viewMatrix;
uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform float u_uToKpc;
uniform sampler2DArray u_textures;

// INPUT
in vec4 v_col;
in vec2 v_uv;
in float v_dist;
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
layout (location = 1) out vec4 layerBuffer;
// Fragment Shader Output Declarations
layout(location = 2) out vec4 accumColor;// Accumulation RT
layout(location = 3) out vec4 revealage;// Revealage RT


#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif// ssrFlag

#define decay 0.2
#define PI 3.1415927

vec4 colorTex(float alpha, float texBrightness) {
    return v_col * v_col.a * texBrightness * alpha;
}

void main() {
    vec2 uv = v_uv;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }
    float texBrightness = texture(u_textures, vec3(uv, v_layer)).r;
    // Apply non-linear intensity compression to prevent whiteout
    //fragColor.rgb = fragColor.rgb / (fragColor.rgb + vec3(1.2));
    //fragColor.rgb = pow(fragColor.rgb, vec3(1.1));

    vec4 col = v_col;
    float w_m = 1.0;
    if (v_type == T_BULGE) {
        col.rgb = vec3(1.0, 1.0, 1.0);
        col.a = 1.0;
    } else if (v_type == T_DUST) {
        w_m = 1.6;
    }

    float C_alpha = col.a * texBrightness * u_alpha;
    vec3 C_rgb = col.rgb;

    float norm_units = 100.0 / u_uToKpc;
    float z = v_dist;
    float z_norm = z / norm_units;
    // w is the depth-dependent weight (calculated from fragment depth z)
    //float w = calculateWeight(C_alpha, z_lin) * weightMultiplier;
    //float w = max(min(1.0, max(max(C_rgb.r, C_rgb.g), C_rgb.b) * C_alpha), C_alpha)
    //                * clamp(1.0 / (1.0e-5 + pow(z_norm, 0.5)), 1.0e-2, 3.0e8)
    //                * 1.0e6;
    float w = clamp(1.0 / pow(z_norm, 2.0), 1.0e-4, 1.0e10) * w_m * texBrightness;

    // 1. Accumulation color
    accumColor = vec4(C_rgb * C_alpha, C_alpha) * w;

    // 2. Revealage Output
    revealage = vec4(C_alpha, 0.0, 0.0, 0.0);

    fragColor.rgb = accumColor.rgb;
    fragColor.a = 1.0;

    // Logarithmic depth buffer (not used actually).
    //gl_FragDepth = getDepthValue(v_dist, u_zfar, u_k);
    //layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Add outline
    //if (uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif// ssrFlag
}
