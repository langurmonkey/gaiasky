#ifndef GLSL_LIB_PROCGEN
#define GLSL_LIB_PROCGEN

#include <shader/lib/noise/common.glsl>
#include <shader/lib/noise/simplex.glsl>
#include <shader/lib/noise/perlin.glsl>
#include <shader/lib/noise/voronoi.glsl>
#include <shader/lib/noise/crater.glsl>
#include <shader/lib/noise/blocky.glsl>
#include <shader/lib/noise/fbm.glsl>

float noise(vec3 p,
        int type,
        float persistence,
        float frequency,
        float lacunarity,
        bool turbulence,
        bool ridge,
        vec3 scale,
        int octaves,
        float seed) {
    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(seed,
            persistence,
            frequency,
            lacunarity,
            scale,
            octaves,
            turbulence,
            ridge);

    return gln_fbm(p, opts, type);
}

// Converts spherical coordinates to a cartesian point in 3D (radius 1).
vec3 sphericalToCartesian(float phi, float theta) {
    float cosPhi = cos(phi);
    return vec3(cosPhi * cos(theta),
            cosPhi * sin(theta),
            sin(phi));
}

vec2 computeElevation(vec3 p, float baseLevel) {
    // Warping.
    vec3 warp = vec3(0.0);
    if (u_warp.x > 0.001) {
        float warpFreq = 2.0;
        warp.x = noise(p, SIMPLEX, 0.5, u_warp.y, 2.0, false, false, vec3(1.0), 3, u_seed + 0.32) * u_warp.x;
        warp.y = noise(p, SIMPLEX, 0.5, u_warp.y, 2.0, false, false, vec3(1.0), 3, u_seed + 0.121) * u_warp.x;
        warp.z = noise(p, SIMPLEX, 0.5, u_warp.y, 2.0, false, false, vec3(1.0), 3, u_seed - 0.421) * u_warp.x;
    }
    p = p + warp;

    float elevation_noise = noise(p, u_type, u_persistence, u_frequency, u_lacunarity, u_turbulence, u_ridge, u_scale, u_octaves, u_seed);
    if (u_smoothing) {
        elevation_noise = smoothstep(0.0, 1.0, elevation_noise);
    }

    float elevation;
    if (u_remap) {
        elevation = gln_map(elevation_noise, baseLevel, 1.0, 0.0, 1.0);
        // In remap mode, base level gets mapped to 0.
        baseLevel = 0.0;
    } else {
        elevation = max(baseLevel, elevation_noise);
    }

    // Plains above water
    float plainsHeight = u_plains.x;  // 0.0 = no plains, 0.5 = half the land is plains
    float plainsSlope  = u_plains.y;   // e.g. 0.1 = very gentle rise

    if (plainsHeight > 0.0) {
        // Normalize above-water elevation to [0, 1]
        float t = (elevation - baseLevel) / max(1.0 - baseLevel, 0.001);
        t = clamp(t, 0.0, 1.0);

        float plainsMaxElevation = plainsHeight * plainsSlope;
        float mountainRange = 1.0 - plainsMaxElevation;
        float mountainSlope = mountainRange / max(1.0 - plainsHeight, 0.001);

        float remapped;
        float blend = 0.05; // transition width
        if (t <= plainsHeight - blend) {
            remapped = t * plainsSlope;
        } else if (t >= plainsHeight + blend) {
            remapped = plainsMaxElevation + (t - plainsHeight) * mountainSlope;
        } else {
            // Smooth blend at the transition
            float a = (t - (plainsHeight - blend)) / (2.0 * blend);
            float plainsVal = t * plainsSlope;
            float mountainVal = plainsMaxElevation + (t - plainsHeight) * mountainSlope;
            remapped = mix(plainsVal, mountainVal, smoothstep(0.0, 1.0, a));
        }

        elevation = baseLevel + remapped * (1.0 - baseLevel);
    }

    return vec2(elevation, baseLevel);
}
#endif // GLSL_LIB_PROCGEN