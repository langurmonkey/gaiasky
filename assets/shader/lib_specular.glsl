#define PI 3.14159
// https://gist.github.com/galek/53557375251e1a942dfa
// compute fresnel specular factor for given base specular and product
// product could be NdV or VdH depending on used technique
vec3 fresnel_factor(in vec3 f0, in float product)
{
    return mix(f0, vec3(1.0), pow(1.01 - product, 5.0));
}
// following functions are copies of UE4
// for computing cook-torrance specular lighting terms

float D_blinn(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float n = 2.0 / m2 - 2.0;
    return (n + 2.0) / (2.0 * PI) * pow(NdH, n);
}

float D_beckmann(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float NdH2 = NdH * NdH;
    return exp((NdH2 - 1.0) / (m2 * NdH2)) / (PI * m2 * NdH2 * NdH2);
}

float D_GGX(in float roughness, in float NdH)
{
    float m = roughness * roughness;
    float m2 = m * m;
    float d = (NdH * m2 - NdH) * NdH + 1.0;
    return m2 / (PI * d * d);
}

float G_schlick(in float roughness, in float NdV, in float NdL)
{
    float k = roughness * roughness * 0.5;
    float V = NdV * (1.0 - k) + k;
    float L = NdL * (1.0 - k) + k;
    return 0.25 / (V * L);
}
// simple phong specular calculation with normalization
vec3 phong_specular(in vec3 V, in vec3 L, in vec3 N, in vec3 specular, in float roughness) {
    vec3 R = reflect(-L, N);
    float spec = max(0.0, dot(V, R));

    float k = 1.999 / (roughness * roughness);

    return min(1.0, 3.0 * 0.0398 * k) * pow(spec, min(10000.0, k)) * specular;
}
// simple blinn specular calculation with normalization
vec3 blinn_specular(in float NdH, in vec3 specular, in float roughness)
{
    float k = 1.999 / (roughness * roughness);

    return min(1.0, 3.0 * 0.0398 * k) * pow(NdH, min(10000.0, k)) * specular;
}

// cook-torrance specular calculation
#define COOK_BLINN
vec3 cooktorrance_specular(in float NdL, in float NdV, in float NdH, in vec3 specular, in float roughness)
{
    #ifdef COOK_BLINN
    float D = D_blinn(roughness, NdH);
    #endif

    #ifdef COOK_BECKMANN
    float D = D_beckmann(roughness, NdH);
    #endif

    #ifdef COOK_GGX
    float D = D_GGX(roughness, NdH);
    #endif

    float G = G_schlick(roughness, NdV, NdL);

    float rim = mix(1.0 - roughness * 0.9, 1.0, NdV);

    return (1.0 / rim) * specular * G * D;
}