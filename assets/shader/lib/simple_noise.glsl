#ifndef GLSL_LIB_SIMPLENOISE
#define GLSL_LIB_SIMPLENOISE

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

#endif // GLSL_LIB_SIMPLENOISE