#ifndef GLSL_LIB_FBM
#define GLSL_LIB_FBM

// Noise types
#define PERLIN 0
#define SIMPLEX 1
#define VORONOI 2
#define CRATER 3
#define BLOCKY 4

float gln_fbm(vec3 p, gln_tFBMOpts opts, int type) {
    p += (opts.seed * 100.0);
    float result = 0.0, amplitude = 1.0, frequency = opts.frequency, maximum = amplitude;
    for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
        if (i >= opts.octaves) break;
        vec3 p = p * frequency * opts.scale;
        float noiseVal;
        if (type == PERLIN) noiseVal = gln_perlin(p);
        else if (type == SIMPLEX) noiseVal = gln_simplex(p);
        else if (type == VORONOI) noiseVal = gln_voronoi(p);
        else if (type == CRATER) noiseVal = gln_crater(p);
        else if (type == BLOCKY) noiseVal = gln_blocky(p, frequency, amplitude);

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
    float value = result / maximum;
    // Map to [0,1] before pow to avoid NaN from pow(negative, non-integer).
    // If turbulence/ridge are on, then the result is already in [0,1].
    if (!opts.turbulence && !opts.ridge) {
        value = gln_map(value, -1.0, 1.0, 0.0, 1.0);
    }
    return value;
}

#endif // GLSL_LIB_FBM