// #name: Worley
// #deps: Luma

/**
 * @typedef {struct} gln_tVornoiOpts    Options for Voronoi Noise generators.
 * @property {float} seed               Seed for PRNG generation.
 * @property {float} distance           Size of each generated cell
 * @property {vec3} scale               "Zoom level" of generated noise.
 * @property {boolean} invert           Invert generated noise.
 */
struct gln_tVoronoiOpts {
  float seed;
  float distance;
  vec3 scale;
  bool invert;
};

/**
 * Generates 2D Voronoi Noise.
 *
 * @name gln_voronoi
 * @function
 * @param {vec2}  x                  Point to sample Voronoi Noise at.
 * @param {gln_tWorleyOpts} opts     Options for generating Voronoi Noise.
 * @return {float}                   Value of Voronoi Noise at point "p".
 *
 * @example
 * gln_tWorleyOpts opts = gln_tWorleyOpts(uSeed, 0.0, 0.5, false);
 *
 * float n = gln_voronoi(position.xy, opts);
 */
float gln_voronoi(vec2 point, gln_tVoronoiOpts opts) {
  vec2 p = floor(point * opts.scale.xy);
  vec2 f = fract(point * opts.scale.xy);
  float res = 0.0;
  for (int j = -1; j <= 1; j++) {
    for (int i = -1; i <= 1; i++) {
      vec2 b = vec2(i, j);
      vec2 r = vec2(b) - f + gln_rand(p + b);
      res += 1. / pow(dot(r, r), 8.);
    }
  }

  float result = pow(1. / res, 0.0625);
  if (opts.invert)
    result = 1.0 - result;
  return result;
}

/**
 * Generates 3D Voronoi Noise.
 *
 * @name gln_voronoi
 * @function
 * @param {vec3}  x                  Point to sample Voronoi Noise at.
 * @param {gln_tWorleyOpts} opts     Options for generating Voronoi Noise.
 * @return {float}                   Value of Voronoi Noise at point "p".
 *
 * @example
 * gln_tWorleyOpts opts = gln_tWorleyOpts(uSeed, 0.0, 0.5, false);
 *
 * float n = gln_voronoi(position.xyz, opts);
 */
float gln_voronoi(vec3 point, gln_tVoronoiOpts opts) {
  vec3 p = floor(point * opts.scale);
  vec3 f = fract(point * opts.scale);

  float id = 0.0;
  vec2 res = vec2(100.0);
  for (int k = -1; k <= 1; k++) {
    for (int j = -1; j <= 1; j++) {
      for (int i = -1; i <= 1; i++) {
        vec3 b = vec3(float(i), float(j), float(k));
        vec3 r = vec3(b) - f + gln_rand(p + b);
        float d = dot(r, r);

        float cond = max(sign(res.x - d), 0.0);
        float nCond = 1.0 - cond;

        float cond2 = nCond * max(sign(res.y - d), 0.0);
        float nCond2 = 1.0 - cond2;

        id = (dot(p + b, vec3(1.0, 57.0, 113.0)) * cond) + (id * nCond);
        res = vec2(d, res.x) * cond + res * nCond;

        res.y = cond2 * d + nCond2 * res.y;
      }
    }
  }

  float result = luma(vec3(sqrt(res), abs(id)));
  if (opts.invert)
    result = 1.0 - result;
  return result;
}

/**
 * Generates Fractional Brownian motion (fBm) from 2D Voronoi noise.
 *
 * @name gln_wfbm
 * @function
 * @param {vec2} v               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating Simplex Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(1.0, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * gln_tVoronoiOpts voronoiOpts =
 *     gln_tVoronoiOpts(1.0, 1.0, 3.0, false);
 *
 * float n = gln_vfbm(position.xy, voronoiOpts, opts);
 */
float gln_vfbm(vec2 v, gln_tFBMOpts opts, gln_tVoronoiOpts vopts) {
  v += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
      break;

    vec2 p = v * frequency * opts.scale.xy;

    float noiseVal = gln_voronoi(p, vopts);

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

  float redistributed = pow(result, opts.power);
  return redistributed / maximum;
}

/**
 * Generates Fractional Brownian motion (fBm) from 3D Voronoi noise.
 *
 * @name gln_wfbm
 * @function
 * @param {vec3} v               Point to sample fBm at.
 * @param {gln_tFBMOpts} opts    Options for generating Simplex Noise.
 * @return {float}               Value of fBm at point "p".
 *
 * @example
 * gln_tFBMOpts opts =
 *      gln_tFBMOpts(1.0, 0.3, 2.0, 0.5, 1.0, 5, false, false);
 *
 * gln_tVoronoiOpts voronoiOpts =
 *     gln_tVoronoiOpts(1.0, 1.0, 3.0, false);
 *
 * float n = gln_vfbm(position.xy, voronoiOpts, opts);
 */
float gln_vfbm(vec3 v, gln_tFBMOpts opts, gln_tVoronoiOpts vopts) {
  v += (opts.seed * 100.0);
  float result = 0.0;
  float amplitude = 1.0;
  float frequency = opts.frequency;
  float maximum = amplitude;

  for (int i = 0; i < MAX_FBM_ITERATIONS; i++) {
    if (i >= opts.octaves)
    break;

    vec3 p = v * frequency * opts.scale;

    float noiseVal = gln_voronoi(p, vopts);

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

  float redistributed = pow(result, opts.power);
  return redistributed / maximum;
}
