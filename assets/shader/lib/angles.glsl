#ifndef GLSL_LIB_ANGLES
#define GLSL_LIB_ANGLES

#ifndef PI
#define PI 3.141592653589793238462643383
#endif // PI

#define TO_RAD PI / 180.0
#define TO_DEG 180.0 / PI

#define TO_RAD12 PI / 180.0e12
#define TO_DEG12 180.0e12 / PI

float degrees(float radians) {
    return radians * TO_DEG;
}
float radians(float degrees) {
    return degrees * TO_RAD;
}

float degrees12(float radians) {
    return radians * TO_DEG12;
}
float radians12(float degrees) {
    return degrees * TO_RAD12;
}

#endif // GLSL_LIB_ANGLES