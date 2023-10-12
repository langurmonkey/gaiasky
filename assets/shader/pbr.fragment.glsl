#version 330 core

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;
uniform vec3 u_vrOffset = vec3(0.0);

// DIFFUSE
#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuseCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

// SPECULAR
#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef specularCubemapFlag
uniform samplerCube u_specularCubemap;
#endif

// NORMAL
#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef normalCubemapFlag
uniform samplerCube u_normalCubemap;
#endif

// EMISSIVE
#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#include <shader/lib/luma.glsl>
#endif

#ifdef emissiveCubemapFlag
uniform samplerCube u_emissiveCubemap;
#endif

// METALLIC
#ifdef metallicColorFlag
uniform vec4 u_metallicColor;
#endif

#ifdef metallicTextureFlag
uniform sampler2D u_metallicTexture;
#endif

#ifdef metallicCubemapFlag
uniform samplerCube u_metallicCubemap;
#endif

// ROUGHNESS
#ifdef roughnessColorFlag
uniform vec4 u_roughnessColor;
#endif

#ifdef roughnessTextureFlag
uniform sampler2D u_roughnessTexture;
#endif

#ifdef roughnessCubemapFlag
uniform samplerCube u_roughnessCubemap;
#endif

// DIFFUSE SCATTERING
#ifdef diffuseScatteringColorFlag
uniform vec4 u_diffuseScatteringColor;
#endif

// AMBIENT OCCLUSION
#ifdef AOTextureFlag
uniform sampler2D u_aoTexture;
#endif

// OCCLUSION-METALLIC-ROUGHNESS
#ifdef occlusionMetallicRoughnessTextureFlag
uniform sampler2D u_occlusionMetallicRoughnessTexture;
#endif

// HEIGHT
#ifdef heightTextureFlag
uniform sampler2D u_heightTexture;
#endif

#ifdef heightCubemapFlag
uniform samplerCube u_heightCubemap;
#endif

// REFLECTION
#ifdef reflectionCubemapFlag
uniform samplerCube u_reflectionCubemap;
#endif

// IRIDESCENCE
//#ifdef iridescenceFlag
//#include <shader/lib/iridescence.glsl>
//uniform float u_iridescenceFactor;
//uniform float u_iridescenceIOR;
//uniform float u_iridescenceThicknessMin;
//uniform float u_iridescenceThicknessMax;
//#endif
//
//#ifdef iridescenceTextureFlag
//uniform sampler2D u_iridescenceSampler;
//#endif
//
//#ifdef iridescenceThicknessTextureFlag
//uniform sampler2D u_iridescenceThicknessSampler;
//#endif

#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

#ifdef svtIndirectionDiffuseTextureFlag
uniform sampler2D u_svtIndirectionDiffuseTexture;
#endif

#ifdef svtIndirectionSpecularTextureFlag
uniform sampler2D u_svtIndirectionSpecularTexture;
#endif

#ifdef svtIndirectionNormalTextureFlag
uniform sampler2D u_svtIndirectionNormalTexture;
#endif

#ifdef svtIndirectionHeightTextureFlag
uniform sampler2D u_svtIndirectionHeightTexture;
#endif

#ifdef svtIndirectionEmissiveTextureFlag
uniform sampler2D u_svtIndirectionEmissiveTexture;
#endif

#ifdef svtIndirectionMetallicTextureFlag
uniform sampler2D u_svtIndirectionMetallicTexture;
#endif

// SHININESS
#ifdef shininessFlag
uniform float u_shininess;
#endif // shininessFlag

#if defined(heightTextureFlag) || defined(heightCubemapFlag) || defined(svtIndirectionHeightTextureFlag)
#define heightFlag
#endif// heightTextureFlag

// ECLIPSES
#include <shader/lib/eclipses.glsl>

//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
#define bias 0.030
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;

float getShadowness(vec2 uv, vec2 offset, float compare){
    const vec4 bitShifts = vec4(1.0, 1.0 / 255.0, 1.0 / 65025.0, 1.0 / 160581375.0);
    return step(compare - bias, dot(texture(u_shadowTexture, uv + offset), bitShifts)); //+(1.0/255.0));
}


float textureShadowLerp(vec2 size, vec2 uv, float compare){
    vec2 texelSize = vec2(1.0) / size;
    vec2 f = fract(uv * size + 0.5);
    vec2 centroidUV = floor(uv * size + 0.5) / size;

    float lb = getShadowness(centroidUV, texelSize * vec2(0.0, 0.0), compare);
    float lt = getShadowness(centroidUV, texelSize * vec2(0.0, 1.0), compare);
    float rb = getShadowness(centroidUV, texelSize * vec2(1.0, 0.0), compare);
    float rt = getShadowness(centroidUV, texelSize * vec2(1.0, 1.0), compare);
    float a = mix(lb, lt, f.y);
    float b = mix(rb, rt, f.y);
    float c = mix(a, b, f.x);
    return c;
}

float getShadow(vec3 shadowMapUv) {
    // Complex lookup: PCF + interpolation (see http://codeflow.org/entries/2013/feb/15/soft-shadow-mapping/)
    vec2 size = vec2(1.0 / (2.0 * u_shadowPCFOffset));
    float result = 0.0;
    for(int x=-2; x<=2; x++) {
        for(int y=-2; y<=2; y++) {
            vec2 offset = vec2(float(x), float(y)) / size;
            result += textureShadowLerp(size, shadowMapUv.xy + offset, shadowMapUv.z);
        }
    }
    return result / 25.0;

    // Simple lookup
    //return getShadowness(v_data.shadowMapUv.xy, vec2(0.0), v_data.shadowMapUv.z);
}
#endif // shadowMapFlag
//////////////////////////////////////////////////////
//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
////// CUBEMAPS
//////////////////////////////////////////////////////
#ifdef cubemapFlag
    #include <shader/lib/cubemap.glsl>
#endif // cubemapFlag
//////////////////////////////////////////////////////
//////////////////////////////////////////////////////

//////////////////////////////////////////////////////
////// SVT
//////////////////////////////////////////////////////
#ifdef svtFlag
#include <shader/lib/svt.glsl>
#endif // svtFlag
//////////////////////////////////////////////////////
//////////////////////////////////////////////////////

// COLOR DIFFUSE
#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(texCoord, defaultValue) texture(u_diffuseTexture, texCoord) * u_diffuseColor
#elif defined(diffuseTextureFlag)
    #define fetchColorDiffuseTD(texCoord, defaultValue) texture(u_diffuseTexture, texCoord)
#elif defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(texCoord, defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuseTD(texCoord, defaultValue) defaultValue
#endif // diffuse

#if defined(svtIndirectionDiffuseTextureFlag)
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionDiffuseTexture, texCoord))
#elif defined(diffuseCubemapFlag)
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * texture(u_diffuseCubemap, UVtoXYZ(texCoord))
#elif defined(diffuseTextureFlag) || defined(diffuseColorFlag)
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(texCoord, defaultValue)
#else
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor
#endif // diffuse

// COLOR EMISSIVE
#if defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor
#endif // emissive

// SVT emissive
#if defined(svtIndirectionEmissiveTextureFlag)
    #define fetchColorEmissive(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionEmissiveTexture, texCoord))
#elif defined(emissiveCubemapFlag)
    #define fetchColorEmissive(texCoord) texture(u_emissiveCubemap, UVtoXYZ(texCoord))
#elif defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(texCoord) fetchColorEmissiveTD(u_emissiveTexture, texCoord)
#else
    #define fetchColorEmissive(texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif // SVT emissive

// COLOR SPECULAR
#if defined(svtIndirectionSpecularTextureFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionSpecularTexture, texCoord))
#elif defined(specularCubemapFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularCubemap, UVtoXYZ(texCoord))
#elif defined(specularTextureFlag) && defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord).rgb * u_specularColor.rgb
#elif defined(specularTextureFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord).rgb
#elif defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) u_specularColor.rgb
#else
    #define fetchColorSpecular(texCoord, defaultValue) defaultValue
#endif // specular

// COLOR NORMAL
#if defined(svtIndirectionNormalTextureFlag)
#define fetchColorNormal(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionNormalTexture, texCoord))
#elif defined(normalCubemapFlag)
#define fetchColorNormal(texCoord) texture(u_normalCubemap, UVtoXYZ(texCoord))
#elif defined(normalTextureFlag)
#define fetchColorNormal(texCoord) texture(u_normalTexture, texCoord)
#endif// normal

// COLOR METALLIC
#if defined(svtIndirectionMetallicTextureFlag)
    #define fetchColorMetallic(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionMetallicTexture, texCoord))
#elif defined(metallicCubemapFlag)
    #define fetchColorMetallic(texCoord) texture(u_metallicCubemap, UVtoXYZ(texCoord))
#elif defined(metallicTextureFlag)
    #define fetchColorMetallic(texCoord) texture(u_metallicTexture, texCoord)
#elif defined(occlusionMetallicRoughnessTextureFlag) && defined(metallicColorFlag)
    #define fetchColorMetallic(texCoord) vec3(texture(u_occlusionMetallicRoughnessTexture, texCoord).b) * u_metallicColor.rgb
#elif defined(occlusionMetallicRoughnessTextureFlag)
    #define fetchColorMetallic(texCoord) vec3(texture(u_occlusionMetallicRoughnessTexture, texCoord).b)
#elif defined(metallicColorFlag)
    #define fetchColorMetallic(texCoord) u_metallicColor
#endif // metallic

// COLOR ROUGHNESS
#if defined(svtIndirectionRoughnessTextureFlag)
    #define fetchColorRoughness(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionRoughnessTexture, texCoord)).rgb
#elif defined(roughnessCubemapFlag)
    #define fetchColorRoughness(texCoord) texture(u_roughnessCubemap, UVtoXYZ(texCoord)).rgb
#elif defined(roughnessTextureFlag)
    #define fetchColorRoughness(texCoord) texture(u_roughnessTexture, texCoord).rgb
#elif defined(occlusionMetallicRoughnessTextureFlag) && defined(roughnessColorFlag)
    #define fetchColorRoughness(texCoord) vec3(texture(u_occlusionMetallicRoughnessTexture, texCoord).g) * u_roughnessColor.rgb
#elif defined(occlusionMetallicRoughnessTextureFlag)
    #define fetchColorRoughness(texCoord) vec3(texture(u_occlusionMetallicRoughnessTexture, texCoord).g).rgb
#elif defined(roughnessColorFlag)
    #define fetchColorRoughness(texCoord) u_roughnessColor.rgb;
#endif // roughness

// DIFFUSE SCATTERING COLOR
#if defined(diffuseScatteringColorFlag)
    #define fetchColorDiffuseScattering() u_diffuseScatteringColor.rgb
#endif // diffuse scattering

// COLOR AMBIENT OCCLUSION
#if defined(occlusionMetallicRoughnessTextureFlag)
    #define fetchColorAmbientOcclusion(texCoord) texture(u_occlusionMetallicRoughnessTexture, texCoord).r
#elif defined(AOTextureFlag)
    #define fetchColorAmbientOcclusion(texCoord) texture(u_aoTexture, texCoord).r
#else
    #define fetchColorAmbientOcclusion(texCoord) 1.0
#endif // ambient occlusion

// HEIGHT
#if defined(svtIndirectionHeightTextureFlag)
    #define fetchHeight(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionHeightTexture, texCoord))
#elif defined(heightCubemapFlag)
    #define fetchHeight(texCoord) texture(u_heightCubemap, UVtoXYZ(texCoord))
#elif defined(heightTextureFlag)
    #define fetchHeight(texCoord) texture(u_heightTexture, texCoord)
#endif// height

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
    #define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
uniform DirectionalLight u_dirLights[numDirectionalLights];
#endif // directionalLightsFlag

#if defined(numPointLights) && (numPointLights > 0)
#define pointLightsFlag
#endif// numPointLights

#ifdef pointLightsFlag
struct PointLight {
    vec3 color;
    vec3 position;
    float intensity;
};
uniform PointLight u_pointLights[numPointLights];
#endif // numPointLights

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    #ifdef shadowMapFlag
    vec3 shadowMapUv;
    #endif // shadowMapFlag
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif // metallicFlag
    mat3 tbn;
};
in VertexData v_data;

#ifdef atmosphereGround
in vec4 v_atmosphereColor;
in float v_fadeFactor;
#endif // atmosphereGround

#if !defined(normalTextureFlag) && !defined(normalCubemapFlag) && !defined(svtIndirectionNormalTextureFlag)
in vec3 v_normalTan;
#endif // !normalTextureFlag && !normalCubemapFlag && !normalSVT

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
    #include <shader/lib/ssr.frag.glsl>
#endif // ssrFlag

#define saturate(x) clamp(x, 0.0, 1.0)

#if defined(heightFlag) && defined(parallaxMappingFlag)
    uniform float u_heightScale;
    #include <shader/lib/parallaxmapping.glsl>
#endif // heightFlag && parallaxMappingFlag

#include <shader/lib/atmfog.glsl>
#include <shader/lib/logdepthbuff.glsl>

#ifdef velocityBufferFlag
    #include <shader/lib/velbuffer.frag.glsl>
#endif // velocityBufferFlag

#ifdef ssrFlag
    #include <shader/lib/pack.glsl>
#endif // ssrFlag

// MAIN
void main() {
    vec2 texCoords = v_data.texCoords;

    #if defined(heightFlag) && defined(parallaxMappingFlag)
        // Parallax occlusion mapping
        texCoords = parallaxMapping(texCoords, normalize(v_data.fragPosWorld - u_vrOffset));
    #endif // parallaxMappingFlag

    vec4 diffuse = fetchColorDiffuse(v_data.color, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(texCoords);
    vec3 specular = fetchColorSpecular(texCoords, vec3(0.0, 0.0, 0.0));
    vec3 ambient = v_data.ambientLight;

    #ifdef atmosphereGround
        vec3 night = emissive.rgb;
        emissive = vec4(0.0);
    #else
        vec3 night = vec3(0.0);
    #endif // atmosphereGround

    float ambientOcclusion = fetchColorAmbientOcclusion(texCoords);
    #if defined(occlusionMetallicRoughnessTextureFlag)
        // Sometimes ambient occlusion is not used, and it is set to 0.
        if (ambientOcclusion == 0.0) {
            ambientOcclusion = 1.0;
        }
    #endif// occlusionMetallicRoughnessTextureFlag

    // Occlusion strength is 1 by default.
    diffuse.rgb *= ambientOcclusion;
    specular.rgb *= ambientOcclusion;

    // Alpha value from textures
    float texAlpha = 1.0;
    #if defined(diffuseTextureFlag) || defined(diffuseCubemapFlag)
        texAlpha = diffuse.a;
    #elif defined(emissiveTextureFlag)
        texAlpha = luma(emissive.rgb);
    #endif // diffuseTextureFlag || diffuseCubemapFlag

    vec4 normalVector;
    vec3 N;
    #if defined(normalTextureFlag) || defined(normalCubemapFlag) || defined(svtIndirectionNormalTextureFlag)
        // Fetch from normal map.
        N = normalize(fetchColorNormal(texCoords).rgb * 2.0 - 1.0);
        // To tangent space.
        normalVector.xyz = v_data.tbn * N;
        #ifdef metallicFlag
            vec3 reflectDir = normalize(reflect(v_data.fragPosWorld, normalVector.xyz));
        #endif // metallicFlag
    #else
        // Fetch from previous stage.
        N = v_normalTan;
        normalVector.xyz = v_data.normal;
        #ifdef metallicFlag
            vec3 reflectDir = normalize(v_data.reflect);
        #endif // metallicFlag
    #endif // normalTextureFlag

    // Shadow
    #ifdef shadowMapFlag
        float transparency = 1.0 - texture(u_shadowTexture, v_data.shadowMapUv.xy).g;
        float shdw = clamp(getShadow(v_data.shadowMapUv) + transparency, 0.0, 1.0);
    #else
        float shdw = 1.0;
    #endif // shadowMapFlag

    // Eclipses
    #ifdef eclipsingBodyFlag
        vec3 lightDirection;
        if (any(notEqual(u_dirLights[0].color, vec3(0.0)))) {
            lightDirection = -u_dirLights[0].direction;
        } else {
            lightDirection = normalize(u_pointLights[0].position - v_data.fragPosWorld);
        }
        float outline;
        vec4 outlineColor = eclipseColor(v_data.fragPosWorld, lightDirection, normalVector.xyz, outline, shdw);
    #endif // eclipsingBodyFlag

    // Reflection
    vec3 reflectionColor = vec3(0.0);
    // Reflection mask
    #ifdef ssrFlag
        reflectionMask = vec4(0.0, 0.0, 0.0, 1.0);
    #endif // ssrFlag

    #ifdef metallicFlag
        // Roughness.
        float roughness = 0.0;
        #if defined(roughnessTextureFlag) || defined(roughnessCubemapFlag) || defined(svtIndirectionRoughnessTextureFlag) || defined(roughnessColorFlag) || defined(occlusionMetallicRoughnessTextureFlag)
            vec3 roughness3 = fetchColorRoughness(texCoords);
            roughness = roughness3.r;
        #elif defined(shininessFlag)
            roughness = 1.0 - u_shininess;
        #endif // roughnessTextureFlag, shininessFlag

        #ifdef reflectionCubemapFlag
            reflectionColor = texture(u_reflectionCubemap, vec3(-reflectDir.x, reflectDir.y, reflectDir.z), roughness * 6.0).rgb;
        #endif // reflectionCubemapFlag

        // Metallic.
        vec3 metallicColor = fetchColorMetallic(texCoords).rgb;
        reflectionColor = reflectionColor * metallicColor;
        #ifdef ssrFlag
            vec3 rmc = diffuse.rgb * metallicColor;
            reflectionMask = vec4(rmc.r, pack2(rmc.gb), roughness, 1.0);
            reflectionColor *= 0.0;
        #else
            reflectionColor += reflectionColor * diffuse.rgb;
        #endif // ssrFlag
    #endif // metallicFlag

    //#ifdef iorFlag
    //    vec3 f0 = vec3(pow(( u_ior - 1.0) /  (u_ior + 1.0), 2.0));
    //#else
    //    vec3 f0 = vec3(0.04); // from ior 1.5 value
    //#endif // iorFlag

    vec3 shadowColor = vec3(0.0);
    vec3 diffuseColor = vec3(0.0);
    vec3 specularColor = vec3(0.0);
    float selfShadow = 1.0;
    vec3 fog = vec3(0.0);

    float NL0;
    vec3 L0;

    int validLights = 0;

    // DIRECTIONAL LIGHTS
    #ifdef directionalLightsFlag
        // Loop for directional light contributions.
        for (int i = 0; i < numDirectionalLights; i++) {
            vec3 V = v_data.viewDir;
            vec3 col = u_dirLights[i].color;
            // Skip non-lights
            if (col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
                continue;
            } else {
                validLights++;
            }
            // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
            vec3 L = normalize(-u_dirLights[i].direction * v_data.tbn);
            vec3 H = normalize(L - V);
            float NL = max(0.00001, dot(N, L));
            float NH = max(0.00001, dot(N, H));
            if (validLights == 1) {
                NL0 = NL;
                L0 = L;
            }

            selfShadow *= saturate(4.0 * NL);

            specularColor += specular * min(1.0, pow(NH, 40.0));
            shadowColor += col * night * max(0.0, 0.5 - NL) * shdw;
            diffuseColor = saturate(diffuseColor + col * NL * shdw + ambient * (1.0 - NL));
        }
    #endif // directionalLightsFlag

    // POINT LIGHTS
    #ifdef pointLightsFlag
        // Loop for point light contributions.
        for (int i = 0; i < numPointLights; i++) {
            vec3 V = v_data.viewDir;
            vec3 col = u_pointLights[i].color * u_pointLights[i].intensity;
            // Skip non-lights
            if (all(equal(col, vec3(0.0)))) {
                continue;
            } else {
                validLights++;
            }
            // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
            vec3 L = normalize((u_pointLights[i].position - v_data.fragPosWorld) * v_data.tbn);
            vec3 H = normalize(L - V);
            float NL = max(0.00001, dot(N, L));
            float NH = max(0.00001, dot(N, H));
            if (validLights == 1){
                NL0 = NL;
                L0 = L;
            }

            selfShadow *= saturate(4.0 * NL);

            specularColor += specular * min(1.0, pow(NH, 40.0));
            shadowColor += col * night * max(0.0, 0.5 - NL) * shdw;
            diffuseColor = saturate(diffuseColor + col * NL * shdw + ambient * (1.0 - NL));
        }
    #endif // pointLightsFlag

    // Diffuse texture contribution.
    if (validLights == 0) {
        // Only ambient contribution, we have no illuminating directional lights.
        diffuseColor = saturate(diffuse.rgb * ambient);
    } else {
        // Regular shading.
        diffuseColor *= diffuse.rgb;
    }

    // Diffuse scattering
    #ifdef diffuseScatteringColorFlag
        vec3 diffuseScattering = fetchColorDiffuseScattering();
        diffuseScattering = diffuse.rgb * diffuseScattering * ambientOcclusion * shdw;
    #else
        vec3 diffuseScattering = vec3(0.0);
    #endif // diffuseScatteringColorFlag

    // Final color equation
    fragColor = vec4(diffuseColor + diffuseScattering + shadowColor + emissive.rgb + reflectionColor, texAlpha * v_data.opacity);
    fragColor.rgb += selfShadow * specularColor;

    #ifdef atmosphereGround
        #define exposure 1.0
        fragColor.rgb = clamp(fragColor.rgb + (vec3(1.0) - exp(v_atmosphereColor.rgb * -exposure)) * v_atmosphereColor.a * shdw * v_fadeFactor, 0.0, 1.0);
        #if defined(heightFlag) && !defined(parallaxMappingFlag)
            fragColor.rgb = applyFog(fragColor.rgb, v_data.viewDir, L0 * -1.0, NL0);
        #endif // heightFlag && !parallaxMappingFlag
    #endif // atmosphereGround

    #if defined(eclipsingBodyFlag) && defined(eclipseOutlines)
        if (outline > 0.0) {
            fragColor = outlineColor;
        }
    #endif // eclipsingBodyFlag && eclipseOutlines

    if (fragColor.a <= 0.0) {
        discard;
    }

    #ifdef ssrFlag
        normalBuffer = vec4(normalVector.xyz, 1.0);
    #endif // ssrFlag

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef velocityBufferFlag
        velocityBuffer();
    #endif // velocityBufferFlag
}