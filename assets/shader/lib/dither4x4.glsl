#ifndef GLSL_LIB_DITHER4x4
#define GLSL_LIB_DITHER4x4
const int matrix[16] = int[](0,  8,  2,  10,
                             12, 4,  14, 6,
                             3,  11, 1,  9,
                             15, 7,  13, 5);

float dither(vec2 position, float alpha) {
    int x = int(mod(position.x, 4.0));
    int y = int(mod(position.y, 4.0));
    float limit = matrix[x + y * 4] / 16.0;
    return alpha < limit ? 0.0 : 1.0;
}
#endif