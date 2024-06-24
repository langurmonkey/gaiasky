// #name: Curl
// #deps: Simplex, Luma

vec2 _snois2(vec2 x) {
  float s1 = gln_simplex(vec2(x));
  float s2 = gln_simplex(vec2(x.y - 19.1, x.x + 33.4));
  return vec2(s1, s2);
}

vec3 _snois3(vec3 x) {
  float s = gln_simplex(vec3(x));
  float s1 = gln_simplex(vec3(x.y - 19.1, x.z + 33.4, x.x + 47.2));
  float s2 = gln_simplex(vec3(x.z + 74.2, x.x - 124.5, x.y + 99.4));
  return vec3(s, s1, s2);
}

/**
 * Generates 2D Curl Noise.
 *
 * @name gln_curl
 * @function
 * @param {vec2} p  Point to sample Curl Noise at.
 * @return {float}  Value of Curl Noise at point "p".
 *
 * @example
 * vec2 n = gln_curl(position);
 */
float gln_curl(vec2 p) {
  const float e = .1;
  vec2 dx = vec2(e, 0.0);
  vec2 dy = vec2(0.0, e);

  vec2 p_x0 = _snois2(p - dx);
  vec2 p_x1 = _snois2(p + dx);
  vec2 p_y0 = _snois2(p - dy);
  vec2 p_y1 = _snois2(p + dy);

  float x = p_x1.y - p_x0.y - p_y1.x + p_y0.x;
  float y = p_y1.x - p_y0.x - p_x1.y + p_x0.y;

  const float divisor = 1.0 / (2.0 * e);
  return luma(vec3(normalize(vec2(x, y) * divisor), 0.0));
}

/**
 * Generates 3D Curl Noise.
 *
 * @name gln_curl
 * @function
 * @param {vec3} p  Point to sample Curl Noise at.
 * @return {float}  Value of Curl Noise at point "p".
 *
 * @example
 * float n = gln_curl(position);
 */
float gln_curl(vec3 p) {
  const float e = .1;
  vec3 dx = vec3(e, 0.0, 0.0);
  vec3 dy = vec3(0.0, e, 0.0);
  vec3 dz = vec3(0.0, 0.0, e);

  vec3 p_x0 = _snois3(p - dx);
  vec3 p_x1 = _snois3(p + dx);
  vec3 p_y0 = _snois3(p - dy);
  vec3 p_y1 = _snois3(p + dy);
  vec3 p_z0 = _snois3(p - dz);
  vec3 p_z1 = _snois3(p + dz);

  float x = p_y1.z - p_y0.z - p_z1.y + p_z0.y;
  float y = p_z1.x - p_z0.x - p_x1.z + p_x0.z;
  float z = p_x1.y - p_x0.y - p_y1.x + p_y0.x;

  const float divisor = 1.0 / (2.0 * e);
  return luma(normalize(vec3(x, y, z) * divisor));
}

/**
 * Generates 2D Fractional Brownian motion (fBm) from Curl Noise.
 *
 * @name gln_cfbm
 * @function
 * @param {vec2} p               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating Perlin Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(uSeed, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * float n = gln_cfbm(position.xy, opts);
 */
float gln_cfbm(vec2 p, gln_tFBMOpts opts) {
  p += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
    break;

    vec2 p = p * frequency * opts.scale.xy;

    float noiseVal = gln_curl(p);

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
 * Generates 3D Fractional Brownian motion (fBm) from Curl Noise.
 *
 * @name gln_pfbm
 * @function
 * @param {vec3} p               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating Perlin Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(uSeed, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * float n = gln_cfbm(position.xy, opts);
 */
float gln_cfbm(vec3 p, gln_tFBMOpts opts) {
  p += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
    break;

    vec3 p = p * frequency * opts.scale;

    float noiseVal = gln_curl(p);

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
