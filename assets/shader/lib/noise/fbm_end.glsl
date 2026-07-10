float value = result / maximum;
// Map to [0,1] before pow to avoid NaN from pow(negative, non-integer).
// If turbulence/ridge are on, then the result is already in [0,1].
if (!opts.turbulence && !opts.ridge) {
    value = gln_map(value, -1.0, 1.0, 0.0, 1.0);
}
return value;
