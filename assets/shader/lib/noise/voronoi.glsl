// #name: Voronoi
// #deps: Luma

/**
 * Generates 3D Voronoi Noise.
 *
 * @name gln_voronoi
 * @function
 * @param {vec3}  x                  Point to sample Voronoi Noise at.
 * @return {float}                   Value of Voronoi Noise at point "p".
 *
 * float n = gln_voronoi(position.xyz);
 */
float gln_voronoi(in vec3 point) {
  vec3 p = floor(point);
  vec3 f = fract(point);

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

  return pow(luma(clamp(vec3(sqrt(res), abs(id)), 0.0, 1.0)), 2.0);
}
