#ifndef GLSL_LIB_DITHER8x8
#define GLSL_LIB_DITHER8x8
const int matrix[64] = int[](0,  32, 8,  40, 2,  34, 10, 42,
                             48, 16, 56, 24, 50, 18, 58, 26,
                             12, 44, 4,  36, 14, 46, 6,  38,
                             60, 28, 52, 20, 62, 30, 54, 22,
                             3,  35, 11, 43, 1,  33, 9,  41,
                             51, 19, 59, 27, 49, 17, 57, 25,
                             15, 47, 7,  39, 13, 45, 5,  37,
                             63, 31, 55, 23, 61, 29, 53, 21);

float dither(vec2 position, float alpha) {
    int x = int(mod(position.x, 8.0));
    int y = int(mod(position.y, 8.0));
    float limit = matrix[x + y * 8] / 64.0;
    return alpha < limit ? 0.0 : 1.0;
}
#endif