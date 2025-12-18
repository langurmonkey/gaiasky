#ifndef GLSL_LIB_LIGHT
#define GLSL_LIB_LIGHT
float linstep(float edge0, float edge1, float x) {
    float d = edge1 - edge0;
    return d != 0.0 ? clamp((x - edge0) / d, 0.0, 1.0) : 0.0;
}

void processLight(
        vec3 col,
        vec3 V,
        vec3 N,
        vec3 L,
        int validLights,
        vec3 specular,
        vec3 night,
        inout float NL0,
        inout vec3 L0,
        inout float selfShadow,
        inout vec3 specularColor,
        inout vec3 shadowColor,
        inout vec3 diffuseColor) {

    vec3 H = normalize(L - V);
    float NL = dot(N, L);
    float NH = clamp(dot(N, H), 0.0, 1.0);
    if (validLights == 1) {
        NL0 = clamp(NL, 0.0, 1.0);
        L0 = L;
    }

    float dayFactor = 1.0 - linstep(-0.1, 0.1, -NL);
    float nightFactor = 1.0 - dayFactor;

    selfShadow *= dayFactor;

    specularColor += specular * min(1.0, pow(NH, 40.0));
    shadowColor += night * nightFactor;
    diffuseColor += col * dayFactor;
}
#endif // LIGHT