#version 330 core

// Full resolution color buffer.
uniform sampler2D u_texture0;
// Half resolution color buffer.
uniform sampler2D u_texture1;
// Full resolution depth buffer.
uniform sampler2D u_texture2;
// Half resolution depth buffer.
uniform sampler2D u_texture3;
// Z-far and K values for depth buffer.
uniform vec2 u_zFarK;
/**
 * Debug view of the OIT buffers. 0 = off (normal composite). Anything else replaces the output
 * with a false-colour view of one intermediate quantity, so a black screen can be attributed to a
 * specific term instead of guessed at. Values are shown scaled, not clamped to 0..1, so that a
 * non-black result means "non-zero", which is the only question that matters when bisecting.
 *   1: accumFull.rgb   2: accumFull.a (transmittance)   3: weightFull.r
 *   4: accumHalf.rgb   5: accumHalf.a                  6: weightHalf.r
 *   7: numer/denom     8: total transmittance           9: denom
 */
uniform int u_debugMode;
#ifdef wboitFlag
// Weighted blended OIT accumulations (index 4) and weights (index 5), at both resolutions.
//   accum.rgb = sum(E * w), accum.a = prod(1 - a), weight.rgb = sum(a * w)
uniform sampler2D u_texture4; // full-res accum
uniform sampler2D u_texture5; // half-res accum
uniform sampler2D u_texture6; // full-res weight
uniform sampler2D u_texture7; // half-res weight
#endif// wboitFlag

in vec2 v_texCoords;
layout(location = 0) out vec4 fragColor;

#include <shader/lib/logdepthbuff.glsl>

#ifdef wboitFlag
/**
 * Resolves the weighted blended OIT accumulations of both resolutions into a single premultiplied
 * colour.
 *
 * The two buffers are combined *unresolved*, which is the whole point: emission and weight are
 * plain sums so they add, and the transmittance is a product so it multiplies. Resolving each
 * buffer separately and then adding the results is what made dust unable to occlude full-res
 * stars, since the two resolutions had already lost their relative ordering by then.
 *
 * The half-res accumulations are sampled bilinearly, which is an approximation: a true upsample of
 * a per-texel sum is not a per-pixel bilinear filter. It is the standard trade-off and the error is
 * confined to the half-res contribution, but it does soften dust silhouettes slightly.
 */
vec3 resolveOit() {
    vec4 accumFull = texture(u_texture4, v_texCoords);
    vec4 accumHalf = texture(u_texture5, v_texCoords);
    vec4 weightFull = texture(u_texture6, v_texCoords);
    vec4 weightHalf = texture(u_texture7, v_texCoords);

#ifdef wboitFlag
    if (u_debugMode > 0) {
        // Show one intermediate quantity, scaled so that "not black" means "not zero". These are
        // debug views, not colour-accurate output.
        if (u_debugMode == 1) return abs(accumFull.rgb) * 0.01;
        if (u_debugMode == 2) return vec3(accumFull.a);
        if (u_debugMode == 3) return vec3(weightFull.r * 0.01);
        if (u_debugMode == 4) return abs(accumHalf.rgb) * 0.01;
        if (u_debugMode == 5) return vec3(accumHalf.a);
        if (u_debugMode == 6) return vec3(weightHalf.r * 0.01);
        if (u_debugMode == 7) {
            float d = weightFull.r + weightHalf.r;
            return d > 1e-6 ? abs((accumFull.rgb + accumHalf.rgb) / d) : vec3(0.0);
        }
        if (u_debugMode == 8) return vec3(clamp(accumFull.a * accumHalf.a, 0.0, 1.0));
        if (u_debugMode == 9) return vec3((weightFull.r + weightHalf.r) * 0.01);
    }
#endif// wboitFlag

    // Sums add across resolutions.
    vec3 numer = accumFull.rgb + accumHalf.rgb;
    float denom = weightFull.r + weightHalf.r;

    // The transmittance is a product across resolutions, so it multiplies. This is the term that
    // makes dust in the half-res buffer occlude stars in the full-res buffer.
    float transmittance = clamp(accumFull.a * accumHalf.a, 0.0, 1.0);

    // Nothing was accumulated here: leave the pixel to the colour buffers alone.
    if (denom <= 1e-6) {
        return vec3(0.0);
    }

    // Weighted average of the emission, attenuated by the total transmittance.
    return clamp(numer / denom, 0.0, 1.0) * transmittance;
}
#endif// wboitFlag

void main() {
    vec3 fullRes = texture(u_texture0, v_texCoords).rgb;
    vec3 halfRes = texture(u_texture1, v_texCoords).rgb;

    // Blend using depth buffers.

    // Recover 'linear' depth values.
    float depthFull = 1.0 / recoverWValue(texture(u_texture2, v_texCoords).r, u_zFarK.x, u_zFarK.y);
    // float depthHalf = 1.0 / recoverWValue(texture(u_texture3, v_texCoords).r, u_zFarK.x, u_zFarK.y);

    vec3 color;
    if (depthFull < 1e5) {
        color = fullRes;
    } else {
        color = clamp(fullRes + halfRes, 0.0, 1.0);
    }

#ifdef wboitFlag
    // Billboards no longer write to the colour buffers when OIT is on, so the resolved accumulation
    // is simply added on top of whatever the other renderers produced.
    color += resolveOit();
#endif// wboitFlag

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);

    // if (depthFull < depthHalf) {
    //     // Full-res pixel is closer - use it
    //     fragColor = vec4(fullRes, 1.0);
    // } else if (depthFull > depthHalf) {
    //     // Half-res pixel is closer - use it
    //     fragColor = vec4(halfRes, 1.0);
    // } else {
    //     // Depths are approximately equal - blend
    //     fragColor = vec4(clamp(fullRes + halfRes, 0.0, 1.0), 1.0);
    // }
}
