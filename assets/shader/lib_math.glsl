#ifndef GLSL_LIB_MATH
#define GLSL_LIB_MATH
float lint(float x, float x0, float x1, float y0, float y1) {
    return y0 + (y1 - y0) * smoothstep(x0, x1, x);
}
float lint2(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return mix(y0, y1, (x - x0) / (x1 - x0));
}
float lint3(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
}
float mod(float x, float y) {
    return x - y * floor(x/y);
}
#endif