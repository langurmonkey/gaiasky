#ifndef GLSL_LIB_GOLDNOISE
#define GLSL_LIB_GOLDNOISE
// A very basic noise function used in particle and star twinkling.
float PHI = 1.61803398874989484820459; // Golden Ratio
float gold_noise(in vec2 xy, in float seed) {
    return fract(tan(distance(xy * PHI, xy) * seed) * xy.x);
}
#endif
