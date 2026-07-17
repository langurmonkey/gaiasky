// #name: White

/*
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
