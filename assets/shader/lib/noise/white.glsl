// #name: White

/**
 * Implimentation of 2D white noise.
 * Based on: https://www.ronja-tutorials.com/post/024-white-noise/
 *
 * @name gln_white
 * @function
 * @param {vec2} p Point to sample noise at.
 */
float gln_white(vec2 p){
  vec2 dotDir = vec2(12.9898, 78.233);
  vec2 smallValue = sin(p);
  float random = dot(smallValue, dotDir);
  random = fract(sin(random) * 143758.5453);
  return random;
}

/**
 * Implimentation of 3D white noise.
 * Based on: https://www.ronja-tutorials.com/post/024-white-noise/
 *
 * @name gln_white
 * @function
 * @param {vec3} p Point to sample noise at.
 */
float gln_white(vec3 p){
  vec3 dotDir = vec3(12.9898, 78.233, 37.719);
  vec3 smallValue = sin(p);
  float random = dot(smallValue, dotDir);
  random = fract(sin(random) * 143758.5453);
  return random;
}
/**
 * Generates Fractional Brownian motion (fBm) from 2D White noise.
 *
 * @name gln_wfbm
 * @function
 * @param {vec2} v               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating fBm Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(1.0, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * float n = gln_wfbm(position.xy, opts);
 */
float gln_wfbm(vec2 v, gln_tFBMOpts opts) {
  v += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
    break;

    vec2 p = v * frequency * opts.scale.xy;

    float noiseVal = gln_white(p);

    result += noiseVal * amplitude;

    frequency *= opts.lacunarity;
    amplitude *= opts.persistence;
    maximum += amplitude;
  }

  if (opts.turbulence && !opts.ridge) {
    result = abs(result);
  } else if (opts.ridge) {
    result = 1.0 - abs(result);
  }

  return pow(result / maximum, opts.power);
}

/**
 * Generates Fractional Brownian motion (fBm) from 3D White noise.
 *
 * @name gln_wfbm
 * @function
 * @param {vec3} v               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating fBm Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(1.0, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * float n = gln_wfbm(position.xy, opts);
 */
float gln_wfbm(vec3 v, gln_tFBMOpts opts) {
  v += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
    break;

    vec3 p = v * frequency * opts.scale;

    float noiseVal = gln_white(p);

    result += noiseVal * amplitude;

    frequency *= opts.lacunarity;
    amplitude *= opts.persistence;
    maximum += amplitude;
  }

  if (opts.turbulence && !opts.ridge) {
    result = abs(result);
  } else if (opts.ridge) {
    result = 1.0 - abs(result);
  }

  return pow(result / maximum, opts.power);
}
