#ifndef GLSL_LIB_CUBEMAP
#define GLSL_LIB_CUBEMAP

#ifndef PI
#define PI 3.141592653589793238462643383
#endif // PI
vec3 UVtoXYZ(vec2 tc) {
    float lat = tc.y * PI;
    float lon = tc.x * 2.0 * PI;
    vec3 cubemaptc;
    cubemaptc.x = -sin(lon) * sin(lat);
    cubemaptc.y = cos(lat);
    cubemaptc.z = -cos(lon) * sin(lat);

    return cubemaptc;
}
#endif