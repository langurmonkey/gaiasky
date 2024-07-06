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
