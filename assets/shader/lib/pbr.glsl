#ifndef GLSL_LIB_PBR
#define GLSL_LIB_PBR

// Need linstep() from math.glsl library
#include <shader/lib/math.glsl>

// Trowbridge-Reitz GGX normal distribution function
float distributionGGX(float NdotH, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH2 = NdotH * NdotH;

    float nom = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return nom / max(denom, 0.0001);
}

// Schlick-GGX geometry function
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;

    float nom = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / max(denom, 0.0001);
}

// Smith's method combining view and light geometry
float geometrySmith(float NdotV, float NdotL, float roughness) {
    float ggx2 = geometrySchlickGGX(NdotV, roughness);
    float ggx1 = geometrySchlickGGX(NdotL, roughness);

    return ggx1 * ggx2;
}

// Fresnel-Schlick approximation
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// ============================================================================
// Process a single point or directional light with PBR BRDF
// ============================================================================

void processLight(
                vec3 col,
                vec3 V,
                vec3 N,
                vec3 L,
                int validLights,
                vec3 specular,
                vec3 night,
                vec2 tc,
                inout float NL0,
                inout vec3 L0,
                inout float selfShadow,
                inout vec3 specularColor,
                inout vec3 shadowColor,
                inout vec3 diffuseColor) {

    // Normalize vectors
    vec3 Ln = normalize(L);
    vec3 Vn = normalize(-V);
    vec3 H = normalize(Ln + Vn);

    // Calculate dot products
    float NdotL = dot(N, Ln);
    float NdotV = max(dot(N, Vn), 0.0001); // Prevent division by zero
    float NdotH = max(dot(N, H), 0.0);
    float HdotV = max(dot(H, Vn), 0.0);

    // Store first valid light direction
    if (validLights == 1) {
        NL0 = clamp(NdotL, 0.0, 1.0);
        L0 = Ln;
    }

    // Cloud shadow, if needed
    float cloudShadow = 1.0;
    #ifdef occlusionCloudsFlag
        // Use Ln (current light direction) for projection instead of L0
        float cloudMap = fetchColorAmbientOcclusion(tc + Ln.xy * 0.0015);
        cloudShadow = clamp(pow(1.0 - cloudMap, 0.7), 0.0, 1.0);
    #endif

    // Day/night transition for emissive (keep existing behavior)
    float dayFactor = 1.0 - linstep(-0.1, 0.1, -NdotL);
    float nightFactor = 1.0 - dayFactor;

    selfShadow *= dayFactor;
    shadowColor += night * nightFactor;

    // Early exit if light is behind surface
    if (NdotL <= 0.0) {
        return;
    }

    float NdotL_clamped = clamp(NdotL, 0.0, 1.0);

    // ========================================================================
    // PBR BRDF Calculation
    // ========================================================================

    // Get roughness from specular input (assume it's encoded in specular.r)
    // If you have a separate roughness texture, use that instead
    float roughness = 0.5; // Default, should be passed or computed from material
    #ifdef roughnessTextureFlag
    // This will need to be passed in or computed before processLight is called
    // For now using a default value
    #endif

    // Base reflectivity (F0)
    // For now, assume dielectric (0.04). This should come from metallic workflow
    vec3 F0 = vec3(0.04);
    #ifdef metallicFlag
    // This will be improved when we fix the metallic workflow
    // For now, mix with specular color if available
    F0 = mix(F0, specular, 0.0); // Will be fixed in item 4
    #endif

    // Cook-Torrance BRDF components
    float D = distributionGGX(NdotH, roughness);
    float G = geometrySmith(NdotV, NdotL_clamped, roughness);
    vec3 F = fresnelSchlick(HdotV, F0);

    // Specular term: DFG / (4 * NdotV * NdotL)
    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL_clamped;
    vec3 specularBRDF = numerator / max(denominator, 0.0001);

    // Apply specular mask (e.g., water vs land on planets)
    // specular parameter contains per-pixel specular intensity mask
    specularBRDF *= specular;

    // Energy conservation: kS is Fresnel (specular), kD is what's left for diffuse
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;

    // Metallic surfaces don't have diffuse (will be refined in item 4)
    // For now, assume non-metallic
    // kD *= (1.0 - metallic); // Will be added in item 4

    // Lambertian diffuse (will be multiplied by albedo later)
    vec3 diffuseBRDF = kD / PI;

    // Accumulate lighting contributions
    // diffuseColor accumulates irradiance * BRDF (without albedo yet)
    // specularColor accumulates full specular term with mask applied
    specularColor += specularBRDF * col * NdotL_clamped * cloudShadow;
    diffuseColor += diffuseBRDF * col * NdotL_clamped * cloudShadow;
}

// Process a single point or directional light
void processLightOriginal(
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

#endif