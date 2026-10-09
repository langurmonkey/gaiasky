#version 330 core

#include <shader/lib/math.glsl>
#include <shader/lib/logdepthbuff.glsl>

uniform mat4 u_viewMatrix;
uniform float u_alpha;
uniform float u_zfar;
uniform float u_k;
uniform sampler2DArray u_textures;
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
// Weighted blended OIT targets (indices 4 and 5, see PingPongBuffer.createMainFrameBuffer).
// Both are written with the same blend state, blendFuncSeparate(ONE, ONE, ZERO, ONE_MINUS_SRC_ALPHA):
// RGB is additive on both, and alpha is multiplicative, which is only meaningful on the accum target.
//   accum.rgb = sum over fragments of E * w        -> numerator of the weighted average
//   accum.a   = prod over fragments of (1 - a * w)  -> transmittance
//   weight.rgb = sum over fragments of a * w        -> denominator of the weighted average
#ifdef wboitFlag
layout (location = 4) out vec4 oitAccum;
layout (location = 5) out vec4 oitWeightAccum;
#endif// wboitFlag

#ifdef ssrFlag
#include <shader/lib/ssr.frag.glsl>
#endif// ssrFlag

#define decay 0.2

/**
 * Per-channel transmission model.
 *
 * Every fragment is described by exactly two quantities:
 *
 *   E - emission: the light this fragment contributes to the scene (linear RGB).
 *   a - coverage: how much of the background this fragment hides, in [0, 1].
 *
 * There are exactly two kinds of channel:
 *
 *   emissive (stars, HII, bulge, gas): E is the particle colour, a is the sprite falloff.
 *       These add light and also occlude a little, because their coverage enters the
 *       transmittance term (1 - R). This is mild and is deliberate: the falloff cancels in
 *       the composite, so an emitter-only frame reproduces the additive result exactly.
 *
 *   occlusive (dust): E = 0, a is the dust coverage. Dust must never add light.
 *
 * Splitting the pair is what makes dust able to occlude a full-resolution star that lives in
 * a different buffer: both channels write the same (E, a) pair into shared accumulation
 * targets, so ordering is resolved once, before resolution is split.
 */
vec4 colorTex(float alpha, float texBrightness) {
    return v_col * v_col.a * texBrightness * alpha;
}

/** Returns the emission E of an emissive fragment, after whiteout compression. */
vec3 emissionEmissive(float alpha, float texBrightness) {
    vec3 E = colorTex(alpha, texBrightness).rgb;
    // Apply non-linear intensity compression to prevent whiteout
    E = E / (E + vec3(1.2));
    return E;
}

#ifdef wboitFlag
/**
 * Weighted blended OIT weight (McGuire et al. 2013). The weight is exponential in both the
 * fragment coverage and its (window-space) depth, so that a small set of near or opaque
 * fragments dominates and the approximation of order-independent compositing becomes accurate.
 */
float oitWeight(float coverage, float depth) {
    return clamp(pow(min(1.0, coverage * 10.0) + 0.01, 3.0) * 1e8 * pow(1.0 - depth * 0.9, 3.0), 1e-2, 3e3);
}
#endif// wboitFlag

void main() {
    vec2 uv = v_uv;
    float dist = min(1.0, distance(vec2(0.5), uv) * 2.0);
    if (dist >= 1.0){
        discard;
    }
    float texBrightness = texture(u_textures, vec3(uv, v_layer)).r;

    // E: emission, a: coverage. Both are consumed by the OIT accumulation pass; until that
    // exists, they are combined here exactly as the old per-channel blending did.
    vec3 E;
    float a;
    if (v_type == T_DUST) {
        // Occlusive channel: no emission at all. The coverage is the sprite's radial falloff,
        // scaled by the dataset intensity and the global opacity, i.e. the same quantity that
        // used to be subtracted. Clamped so a sprite can never fully hide what is behind it,
        // which would also make the weight term vanish.
        E = vec3(0.0);
        a = clamp(texBrightness * v_col.a * u_alpha, 0.0, 1.0);
    } else {
        // Emissive channel: the colour contributes light, the falloff is the coverage.
        E = emissionEmissive(u_alpha, texBrightness);
        a = texBrightness;
    }

#ifdef wboitFlag
    // OIT path: E and a go to the accumulation targets, not to the scene colour buffer, so that
    // they can be resolved across resolutions before anything is composited.
    //   accum.rgb: additive under (ONE, ONE)           -> sum(E * w), the numerator
    //   accum.a:   multiplied by (ONE_MINUS_SRC_ALPHA) -> prod(1 - a), the transmittance
    //   weight.rgb: additive under (ONE, ONE)          -> sum(a * w), the denominator
    //
    // The transmittance uses the RAW coverage a, not a * w. w is a weighting factor for the
    // weighted average and reaches 3e3; multiplying alpha by it makes (1 - a*w) strongly negative
    // for any non-trivial coverage, the product collapses towards zero, and the whole galaxy is
    // composited at zero brightness, i.e. black. Only the weighted average is weighted.
    float w = oitWeight(aW, gl_FragCoord.z);
    oitAccum = vec4(E * aW * w, aOIT);
    oitWeightAccum = vec4(aW * w);
    fragColor = vec4(0.0);
#else
    // Non-OIT path: composited with ALPHA blending, i.e. dst = E + dst * (1 - a).
    fragColor = vec4(E, a);
    #endif// wboitFlag

    // Logarithmic depth buffer (not used actually).
    gl_FragDepth = getDepthValue(v_dist, u_zfar, u_k);
    layerBuffer = vec4(0.0, 0.0, 0.0, 1.0);

    // Add outline
    //if (uv.x > 0.99 || uv.x < 0.01 || uv.y > 0.99 || uv.y < 0.01) {
    //    fragColor = vec4(1.0, 1.0, 0.0, 1.0);
    //}

    #ifdef ssrFlag
    ssrBuffers();
    #endif// ssrFlag
}
