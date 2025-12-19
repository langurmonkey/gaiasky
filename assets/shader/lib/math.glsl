#ifndef GLSL_LIB_MATH
#define GLSL_LIB_MATH

#ifndef PI
#define PI 3.14159265
#endif // PI

#ifndef saturate
#define saturate(x) clamp(x, 0.0, 1.0)
#endif // saturate

// 2D linear interpolation using smoothstep
float lint(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return y0 + (y1 - y0) * smoothstep(x0, x1, x);
}

// 2D linear interpolation using mix
float lint2(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return mix(y0, y1, (x - x0) / (x1 - x0));
}

// 3D linear interpolation using simple math
float lint3(float x, float x0, float x1, float y0, float y1) {
    if(x <= x0) return y0;
    if(x >= x1) return y1;
    return y0 + (y1 - y0) * (x - x0) / (x1 - x0);
}

// Modulus operation (%)
float mod(float x, float y) {
    return x - y * floor(x/y);
}

// Distance between segment and point
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

// Linear interpolation smoothstep-style
float linstep(float edge0, float edge1, float x) {
    float d = edge1 - edge0;
    return d != 0.0 ? clamp((x - edge0) / d, 0.0, 1.0) : 0.0;
}
#endif // GLSL_LIB_MATH