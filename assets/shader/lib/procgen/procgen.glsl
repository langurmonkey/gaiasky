#ifndef GLSL_LIB_PROCGEN
#define GLSL_LIB_PROCGEN

// Noise types
#define PERLIN 0
#define SIMPLEX 1
#define VORONOI 2

float noise(vec3 p,
        int type,
        float frequency,
        bool turbulence,
        bool ridge,
        vec3 scale,
        int octaves,
        float seed) {
    // Fill up opts.
    gln_tFBMOpts opts = gln_tFBMOpts(seed,
            u_persistence,
            u_frequency,
            u_lacunarity,
            scale,
            octaves,
            turbulence,
            ridge);

    float value = 0.0;
    if (type == PERLIN) {
        value = gln_pfbm(p, opts);
    } else if (type == SIMPLEX) {
        value = gln_sfbm(p, opts);
    } else if (type == VORONOI) {
        value = gln_vfbm(p, opts);
    }

    return value;
}

// Converts spherical coordinates to a cartesian point in 3D (radius 1).
vec3 sphericalToCartesian(float phi, float theta) {
    float cosPhi = cos(phi);
    return vec3(cosPhi * cos(theta),
            cosPhi * sin(theta),
            sin(phi));
}

vec2 computeElevation(vec3 p, float baseLevel) {
    float elevation_noise = noise(p, u_type, u_frequency, u_turbulence, u_ridge, u_scale, u_octaves, u_seed);
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