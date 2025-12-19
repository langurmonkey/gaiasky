#ifndef GLSL_LIB_PBR
#define GLSL_LIB_PBR

// Need linstep() from math.glsl library
#include <shader/lib/math.glsl>

// Process a single point or directional light
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

// ----------------------------------------------------------------------------
float DistributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness*roughness;
    float a2 = a*a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH*NdotH;

    float nom   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r*r) / 8.0;

    float nom   = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}
// ----------------------------------------------------------------------------
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}
// ----------------------------------------------------------------------------
#endif