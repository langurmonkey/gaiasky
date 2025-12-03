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

float calculateWeight(float alpha, float z_lin) {
    // --- New Key Constant ---
    // Use a normalization factor that brings the world depth (z_lin) into a
    // manageable 0-1 range. Use a constant proportional to your z_far.
    float DistanceNormalizer = 1.0 / u_uToKpc; // some Kpc?

    // --- Tuning Constants ---
    // WeightScale now controls the maximum final opacity (C_alpha * WeightScale)
    const float WeightScale = 100.0; // Start with a large tuning number (e.g., 1000.0)
    const float AlphaPower = 1.0;
    const float min_weight = 0.0001;
    const float max_weight = 3000.0;

    // 1. Opacity Term (Same)
    float alpha_term = pow(min(1.0, alpha * 10.0) + 0.01, AlphaPower);

    // 2. Depth Term: Normalize z_lin first.
    float z_norm = z_lin / DistanceNormalizer;

    // Now apply the reciprocal weight formula on the normalized depth
    // The result is large when z_norm is small (close) and small when z_norm is large (far).
    // The epsilon prevents division by zero.
    const float DepthEpsilon = 0.000001;
    float depth_term = 1.0 / (DepthEpsilon + pow(z_norm, 2.0));

    // Combine terms and scale
    float w = alpha_term * WeightScale * depth_term;

    return clamp(w, min_weight, max_weight);
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
    if (v_type == T_BULGE) {
        col.rgb = vec3(0.9, 0.8, 0.95);
    }

    float C_alpha = col.a * texBrightness * u_alpha;
    float C_alpha_eff = C_alpha; // Default to true opacity

    // C_rgb will be the final color to accumulate
    vec3 C_rgb = col.rgb * C_alpha; // Default to pre-multiplied color

    float weightMultiplier = 1.0;
    if (v_type == T_BULGE || v_type == T_GAS) {
        // EMISSIVE: Use the full color, but minimize the effective opacity (alpha_eff)
        // to prevent strong occlusion.
        C_rgb = col.rgb; // Accumulate full color (additive contribution)
        C_alpha_eff = 0.01 * texBrightness * u_alpha; // Low effective alpha for minimal occlusion
    } else if (v_type == T_DUST) {
        // OCCLUSIVE: Use standard WBOIT
        // C_rgb and C_alpha_eff remain as col.rgb * C_alpha and C_alpha
        weightMultiplier = 50.0;
    }

    // Bypass log-depth linearization and use 1/W directly for linear depth (z_lin)
    float z_lin = 1.0 / gl_FragCoord.w;
    // w is the depth-dependent weight (calculated from fragment depth z)
    float w = calculateWeight(C_alpha, z_lin) * weightMultiplier;

    // 1. Accumulation Output (Stores C * w)
    // Use a large constant near the GL_RGBA16F max (e.g., 50000.0)
    const float MAX_ACCUM_VAL = 50000.0;

    // Apply clamping to the final output before writing to the RT
    accumColor = vec4(C_rgb * w, C_alpha_eff * w);
    accumColor = clamp(accumColor, vec4(0.0), vec4(MAX_ACCUM_VAL));

    // 2. Revealage Output (Stores C_alpha_eff * w)
    // IMPORTANT: The Revealage subtraction (D - S) only works if the source (S)
    // is also clamped, or the subtraction will result in a negative number, breaking R.
    float clamped_revealage = min(C_alpha_eff * w, MAX_ACCUM_VAL);
    revealage = vec4(clamped_revealage, 0.0, 0.0, 0.0);

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
