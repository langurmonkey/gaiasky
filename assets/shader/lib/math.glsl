#ifndef GLSL_LIB_MATH
#define GLSL_LIB_MATH
float lint(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
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
float dist_segment_point(in vec3 v, in vec3 w, in vec3 p) {
    vec3 aux3 = p - v;
    vec3 aux4 = w - v;
    vec3 vw = v - w;
    float l2 = dot(vw, vw);
    if (l2 == 0.0) {
        return distance(p, v);
    }
    float t = dot(aux3, aux4) / l2;
    if (t < 0.0) {
        return distance(p, v);
    } else if (t > 1.0) {
        return distance(p, w);
    }
    vec3 projection = v + aux4 * t;
    return distance(p, projection);
}
#endif // GLSL_LIB_MATH