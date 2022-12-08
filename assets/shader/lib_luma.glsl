#ifndef GLSL_LIB_LUMA
#define GLSL_LIB_LUMA
float luma(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}
#endif