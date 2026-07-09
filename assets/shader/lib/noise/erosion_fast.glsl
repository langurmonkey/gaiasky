// #name: Fast erosion noise (fade-approach technique)
// Method described in: https://blog.runevision.com/2026/03/fast-and-gorgeous-erosion-filter.html
// Original technique by Clay John (2018) and Felix Westin/Fewes (2023),
// "fade approach" extensions (fade-towards-target, stacked fading,
// normalized gullies, straight gullies) by Rune Skovbo Johansen (2026).
// This is an original implementation of the described algorithm, not a
// copy of the author's Shadertoy source.

vec2 hash22(vec2 p) {
    uvec2 q = uvec2(ivec2(round(p)));
    q = q * 1664525u + 1013904223u;
    q.x += q.y * 1664525u;
    q.y += q.x * 1664525u;
    q ^= (q >> 16u);
    q.x += q.y * 1664525u;
    q.y += q.x * 1664525u;
    q ^= (q >> 16u);
    return vec2(q) / float(0xffffffffu);
}

// From: https://iquilezles.org/articles/gradientnoise/
vec3 perlin12d(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    vec2 du = 30.0 * f * f * (f * (f - 2.0) + 1.0);
    vec2 ga = hash22(i + vec2(0, 0)) * 2.0 - 1.0;
    vec2 gb = hash22(i + vec2(1, 0)) * 2.0 - 1.0;
    vec2 gc = hash22(i + vec2(0, 1)) * 2.0 - 1.0;
    vec2 gd = hash22(i + vec2(1, 1)) * 2.0 - 1.0;
    float va = dot(ga, f - vec2(0, 0));
    float vb = dot(gb, f - vec2(1, 0));
    float vc = dot(gc, f - vec2(0, 1));
    float vd = dot(gd, f - vec2(1, 1));
    return vec3(va + u.x * (vb - va) + u.y * (vc - va) + u.x * u.y * (va - vb - vc + vd),
            ga + u.x * (gb - ga) + u.y * (gc - ga) + u.x * u.y * (ga - gb - gc + gd)
            + du * (u.yx * (va - vb - vc + vd) + vec2(vb, vc) - va));
}

float ease_out(float t) {
    // Flip, square, flip back -- starts moderately instead of vertically,
    // which is what removes the discontinuities at peaks/valleys.
    float v = 1.0 - clamp(t, 0.0, 1.0);
    return 1.0 - v * v;
}

float pow_inv(float t, float power) {
    return 1.0 - pow(1.0 - clamp(t, 0.0, 1.0), power);
}

// One octave of rotated, per-cell "stripe" noise -- the directional part.
// Only a 3x3 neighborhood is needed (vs. your original 4x4) since the
// weight function is tighter and there's no long tail to account for.
struct GullyResult {
    float height;         // raw gully height in roughly [-1,1]
    vec2  outDeriv;        // true interpolated derivative (for output / fading to 0)
    vec2  straightDeriv;   // sign-based "straight gullies" derivative (steers next octave)
};

GullyResult gully_octave(vec2 p, vec2 slopeDir, float freq) {
    p *= freq;
    vec2 sideDir = normalize(vec2(-slopeDir.y, slopeDir.x));
    vec2 id = floor(p);
    vec2 f = p - id;

    vec2 wave = vec2(0.0); // (cos, sin) pair
    float wSum = 0.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 off = vec2(x, y);
            vec2 c = off + hash22(id + off) - f; // vector to this cell's pivot
            float w = exp(-dot(c, c) * 3.0);
            wSum += w;
            float t = dot(-c, sideDir) * 3.14159265;
            wave += vec2(cos(t), sin(t)) * w;
        }
    }
    wave /= max(wSum, 1e-5);

    // --- normalized gullies ---
    // Scale by k=2, clamp to unit length: lengths >= 0.5 become fully
    // normalized, smaller ones fade to zero -- avoids the "loopy" spikes
    // that full normalization produces.
    float len = length(wave);
    vec2 waveNorm = (len > 1e-5) ? wave / len * min(len * 2.0, 1.0) : vec2(0.0);

    GullyResult r;
    r.height = waveNorm.x;
    r.outDeriv = waveNorm.y * sideDir * freq;
    // --- straight gullies ---
    // Use only the SIGN of the sine part, faking a constant per-side slope
    // (as if stripes were triangle waves) so branches don't curl.
    r.straightDeriv = sign(waveNorm.y) * sideDir * freq;
    return r;
}

/**
 * Erosion noise, "fade approach" (Rune Skovbo Johansen, 2026).
 *
 * @param p           Point to sample.
 * @param fadeTarget  Value in [-1,1], typically altitude-based:
 *                    inverse_lerp(valleyAlt, peakAlt, h) * 2.0 - 1.0
 * @param baseSlope   Pretend constant slope for octave 1, since the real
 *                    input slope is unreliable at peaks/valleys. Tune by eye
 *                    (0.3-0.8 is a reasonable starting range).
 * @param octaves     Number of erosion octaves (4 is a good default).
 * @param lacunarity  Frequency multiplier per octave (~2.0).
 * @param detail      Stacked-fading control -- lower values restrict smaller
 *                    gullies to steeper slopes only (try 0.7-3.0).
 * @param gullyWeight Scales gully contribution vs. fade target (see "Pointy
 *                    peaks"); pair with a matching erosionStrength = 1/gullyWeight
 *                    multiplier at the call site.
 * @return vec3(height, dHeight/dx, dHeight/dy)
 */
vec3 erosion_fade(vec2 p, float fadeTarget, vec2 baseSlope, int octaves,
        float lacunarity, float detail, float gullyWeight) {
    vec2 slope = baseSlope;
    float combiMask = 0.0; // 0 = all fade target, 1 = all gully
    float target = fadeTarget;

    float height = 0.0;
    vec2 deriv = vec2(0.0);
    float freq = 1.0, amplitude = 1.0, total = 0.0;

    for (int i = 0; i < 8; i++) {
        if (i >= octaves) break;

        float slopeLen = length(slope);
        vec2 slopeDir = slopeLen > 1e-5 ? slope / slopeLen : vec2(1.0, 0.0);
        GullyResult g = gully_octave(p, slopeDir, freq);

        // Fade raw gully toward the current target, per the combi-mask.
        float faded = mix(target, g.height, combiMask);
        vec2 fadedDeriv = mix(vec2(0.0), g.outDeriv, combiMask);

        height += faded * amplitude * gullyWeight;
        deriv  += fadedDeriv * amplitude * gullyWeight;
        total  += amplitude;

        // Stacked fading: this octave's faded result becomes the next
        // fade target, and its (straight, non-curling) steepness carves
        // a new opaque region into the mask.
        target = faded;
        float steepness = ease_out(clamp(length(g.straightDeriv) / freq, 0.0, 1.0));
        combiMask = pow_inv(combiMask, detail) * steepness;

        slope += g.straightDeriv;
        freq *= lacunarity;
        amplitude *= 0.5;
    }

    return vec3(height / max(total, 1e-5), deriv);
}

// Self-contained version that derives baseSlope/fadeTarget from an
// underlying Perlin height, so it's a drop-in replacement for erosion12().
vec3 erosion12(vec2 p) {
    vec3 nd = perlin12d(p);
    float fadeTarget = clamp(nd.x * 1.5, -1.0, 1.0); // crude altitude proxy
    return erosion_fade(p, fadeTarget, nd.yz, 4, 2.0, /*detail*/1.5, /*gullyWeight*/0.6);
}

float gln_erosion(vec2 p) {
    return erosion12(p).x;
}

/**
 * Fast fBm using the fade-approach erosion noise. Same signature/usage
 * pattern as your original gln_efbm.
 */
float gln_efbm(vec2 p, gln_tFBMOpts opts) {
    float result = 0.0;
    float amplitude = opts.amplitude;
    float frequency = opts.frequency;
    float maximum = amplitude;

    for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
        if (i >= opts.octaves) break;

        vec2 pp = p * frequency * opts.scale.xy;
        float noiseVal = gln_erosion(pp);

        if (opts.turbulence && !opts.ridge) {
            result += abs(noiseVal) * amplitude;
        } else if (opts.ridge) {
            noiseVal = pow(1.0 - abs(noiseVal), 2.0);
            result += noiseVal * amplitude;
        } else {
            result += noiseVal * amplitude;
        }

        frequency *= opts.lacunarity;
        amplitude *= opts.persistence;
        maximum += amplitude;
    }

    #include <shader/lib/noise/fbm_end.glsl>
}