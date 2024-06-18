// #name: Common

#define MAX_FBM_ITERATIONS 30
#define gln_PI 3.1415926538
vec4 _permute(vec4 x) { return mod(((x * 34.0) + 1.0) * x, 289.0); }
vec4 _taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

/**
 * @typedef {struct} gln_tFBMOpts   Options for fBm generators.
 * @property {float} seed           Seed for PRNG generation.
 * @property {float} frequency      Initial frequency.
 * @property {float} persistence    Factor by which successive layers of noise
 *                                  decrease in amplitude.
 * @property {float} lacunarity     Factor by which successive layers of noise
 *                                  increase in frequency.
 * @property {vec3} scale           Noise scale in (x, y[, z]).
 * @property {float} power          Exponent to apply to the generated noise in a power function.
 * @property {int} octaves          Number of layers of noise to stack.
 * @property {boolean} turbulence   Enable absolute value.
 * @property {boolean} ridge        Convert the fBm to Ridge Noise.
 */
struct gln_tFBMOpts {
  float seed;
  float frequency;
  float persistence;
  float lacunarity;
  vec3 scale;
  float power;
  int octaves;
  bool turbulence;
  bool ridge;
};

/**
 * Converts a number from one range to another.
 *
 * @name gln_map
 * @function
 * @param {} value      Value to map
 * @param {float} min1  Minimum for current range
 * @param {float} max1  Maximum for current range
 * @param {float} min2  Minimum for wanted range
 * @param {float} max2  Maximum for wanted range
 * @return {float} Mapped Value
 *
 * @example
 * float n = gln_map(-0.2, -1.0, 1.0, 0.0, 1.0);
 * // n = 0.4
 */
float gln_map(float value, float min1, float max1, float min2, float max2) {
  return min2 + (value - min1) * (max2 - min2) / (max1 - min1);
}

/**
 * Normalized a value from the range [-1, 1] to the range [0,1].
 *
 * @name gln_normalize
 * @function
 * @param {float} v Value to normalize
 * @return {float} Normalized Value
 *
 * @example
 * float n = gln_normalize(-0.2);
 * // n = 0.4
 */
float gln_normalize(float v) { return gln_map(v, -1.0, 1.0, 0.0, 1.0); }

/**
 * Generates a random 2D Vector.
 *
 * @name gln_rand2
 * @function
 * @param {vec2} p Vector to hash to generate the random numbers from.
 * @return {vec2} Random vector.
 *
 * @example
 * vec2 n = gln_rand2(vec2(1.0, -4.2));
 */
vec2 gln_rand2(vec2 p) {
  return fract(
      sin(vec2(dot(p, vec2(127.1, 311.7)), dot(p, vec2(269.5, 183.3)))) *
      43758.5453);
}

/**
 * Generates a random 3D Vector.
 *
 * @name gln_rand3
 * @function
 * @param {vec3} p Vector to hash to generate the random numbers from.
 * @return {vec3} Random vector.
 *
 * @example
 * vec3 n = gln_rand3(vec3(1.0, -4.2, 0.2));
 */
vec3 gln_rand3(vec3 p) { return mod(((p * 34.0) + 1.0) * p, 289.0); }

/**
 * Generates a random 4D Vector.
 *
 * @name gln_rand4
 * @function
 * @param {vec4} p Vector to hash to generate the random numbers from.
 * @return {vec4} Random vector.
 *
 * @example
 * vec4 n = gln_rand4(vec4(1.0, -4.2, 0.2, 2.2));
 */
vec4 gln_rand4(vec4 p) { return mod(((p * 34.0) + 1.0) * p, 289.0); }

/**
 * Generates a random number.
 *
 * @name gln_rand
 * @function
 * @param {float} n Value to hash to generate the number from.
 * @return {float} Random number.
 *
 * @example
 * float n = gln_rand(2.5);
 */
float gln_rand(float n) { return fract(sin(n) * 1e4); }

/**
 * Generates a random number.
 *
 * @name gln_rand
 * @function
 * @param {vec2} p Value to hash to generate the number from.
 * @return {float} Random number.
 *
 * @example
 * float n = gln_rand(vec2(2.5, -1.8));
 */
float gln_rand(vec2 p) {
  return fract(1e4 * sin(17.0 * p.x + p.y * 0.1) *
               (0.1 + abs(sin(p.y * 13.0 + p.x))));
}
/**
 * Generates a random number.
 *
 * @name gln_rand
 * @function
 * @param {vec3} p Value to hash to generate the number from.
 * @return {float} Random number.
 *
 * @example
 * float n = gln_rand(vec3(2.5, -1.8, 7.2));
 */
vec3 gln_rand(vec3 p) {
    return fract(
        sin(vec3(dot(p, vec3(1.0, 57.0, 113.0)), dot(p, vec3(57.0, 113.0, 1.0)),
                 dot(p, vec3(113.0, 1.0, 57.0)))) *
        43758.5453);
}
