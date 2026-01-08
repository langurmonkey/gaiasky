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
        vec3 specular,  // This is the specular/roughness mask
        vec3 night,
        vec3 albedo,    // The clean base color (diffuse.rgb)
        float metallic, // The metallic value (0.0 - 1.0)
        float roughness,// The roughness value (0.0 - 1.0)
        float shadowMapFactor,
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
    float NdotV = max(dot(N, Vn), 0.0001);
    float NdotH = max(dot(N, H), 0.0);
    float HdotV = max(dot(H, Vn), 0.0);

    // Cloud shadow
    float cloudShadow = 1.0;
    #ifdef occlusionCloudsFlag
        float cloudMap = fetchColorAmbientOcclusion(tc + Ln.xy * 0.0015);
        cloudShadow = clamp(pow(1.0 - cloudMap, 0.7), 0.0, 1.0);
    #endif // occlusionCloudsFlag

    // Combine shadows
    float totalShadow = cloudShadow;

    // Only the first light (the star) uses the shadow map
    if (validLights == 1) {
        totalShadow *= shadowMapFactor;

        // Update global L0 and NL0 for atmosphere/fog logic
        NL0 = clamp(NdotL, 0.0, 1.0);
        L0 = Ln;
    }

    float dayFactor = 1.0 - linstep(-0.1, 0.1, -NdotL);
    float nightFactor = 1.0 - dayFactor;
    #ifndef thinSurfaceFlag
        // Rings do not have night factor.
        selfShadow *= dayFactor;
    #endif // thinSurfaceFlag
    shadowColor += night * nightFactor;

    float NdotL_clamped = clamp(NdotL, 0.0, 1.0);

    float scatterFactor;
    #ifdef thinSurfaceFlag
        // Wrap lighting: prevents the edge-on black line by allowing light to
        // "bleed" past the 90-degree mark.
        float wrap = 0.2;

        // Use abs(NdotL) for the rings so that the back side is lit
        // exactly like the front side (translucency).
        scatterFactor = max(0.0, (abs(NdotL) + wrap) / (1.0 + wrap));
    #else
        // Standard Lambertian for solid objects
        scatterFactor = NdotL_clamped;

        // Early exit for solid objects only
        if (NdotL <= 0.0) return;
    #endif // thinSurfaceFlag

    // Calculate Base Reflectivity (F0)
    // Non-metals use 0.04. Metals use the albedo color.
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // BRDF Components
    float D = distributionGGX(NdotH, roughness);
    float G = geometrySmith(NdotV, NdotL_clamped, roughness);
    vec3 F = fresnelSchlick(HdotV, F0);

    // Specular term
    vec3 numerator = D * G * F;
    float denominator = 4.0 * NdotV * NdotL_clamped;
    vec3 specularBRDF = numerator / max(denominator, 0.0001);

    // Apply the legacy specular mask (if used for specific intensity control)
    specularBRDF *= specular;

    // Energy Conservation (kD)
    float diffuseAlbedoMultiplier = 1.0 - metallic;
    vec3 kS = F;            // Specular ratio
    vec3 kD = (vec3(1.0) - kS) * diffuseAlbedoMultiplier; // Remaining energy

    // Final Accumulation
    vec3 lightIntensitySpecular = col * NdotL_clamped * totalShadow;
    vec3 lightIntensityDiffuse  = col * scatterFactor * totalShadow;

    specularColor += specularBRDF * lightIntensitySpecular;
    diffuseColor  += kD * lightIntensityDiffuse;
}
#endif