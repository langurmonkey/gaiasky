#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_data.normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_binormal = vec3(0.0, 0.0, 1.0);

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_tangent = vec3(1.0, 0.0, 0.0);

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef diffuseCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef specularCubemapFlag
uniform samplerCube u_specularCubemap;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef normalCubemapFlag
uniform samplerCube u_normalCubemap;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#include shader/lib_luma.glsl
#endif

#ifdef emissiveCubemapFlag
uniform samplerCube u_emissiveCubemap;
#endif

#ifdef metallicColorFlag
uniform vec4 u_metallicColor;
#endif

#ifdef metallicTextureFlag
uniform sampler2D u_metallicTexture;
#endif

#ifdef metallicCubemapFlag
uniform samplerCube u_metallicCubemap;
#endif

#ifdef roughnessTextureFlag
uniform sampler2D u_roughnessTexture;
#endif

#ifdef roughnessCubemapFlag
uniform samplerCube u_roughnessCubemap;
#endif

#ifdef heightTextureFlag
uniform sampler2D u_heightTexture;
#endif

#ifdef heightCubemapFlag
uniform samplerCube u_heightCubemap;
#endif

#ifdef reflectionCubemapFlag
uniform samplerCube u_reflectionCubemap;
#endif

#ifdef shininessFlag
uniform float u_shininess;
#endif

#if defined(heightTextureFlag) || defined(heightCubemapFlag)
#define heightFlag
#endif //heightTextureFlag

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

#ifdef cubemapFlag
    #include shader/lib_cubemap.glsl
#endif // cubemapFlag

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

#if defined(diffuseCubemapFlag)
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * texture(u_diffuseCubemap, UVtoXYZ(texCoord))
#elif defined(diffuseTextureFlag) || defined(diffuseColorFlag)
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(texCoord, defaultValue)
#else
    #define fetchColorDiffuse(baseColor, texCoord, defaultValue) baseColor
#endif // diffuse

// COLOR EMISSIVE
#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord) + u_emissiveColor
#elif defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor
#endif // emissive

#ifdef emissiveCubemapFlag
    #define fetchColorEmissive(texCoord) texture(u_emissiveCubemap, UVtoXYZ(texCoord))
#elif defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(texCoord) fetchColorEmissiveTD(u_emissiveTexture, texCoord)
#else
    #define fetchColorEmissive(texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif // emissive

// COLOR SPECULAR
#ifdef specularCubemapFlag
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
#ifdef normalCubemapFlag
    #define fetchColorNormal(texCoord) texture(u_normalCubemap, UVtoXYZ(texCoord))
#elif defined(normalTextureFlag)
    #define fetchColorNormal(texCoord) texture(u_normalTexture, texCoord)
#endif // normal

// COLOR METALLIC
#ifdef metallicCubemapFlag
    #define fetchColorMetallic(texCoord) texture(u_metallicCubemap, UVtoXYZ(texCoord))
#elif defined(metallicTextureFlag)
    #define fetchColorMetallic(texCoord) texture(u_metallicTexture, texCoord)
#elif defined(metallicColorFlag)
    #define fetchColorMetallic(texCoord) u_metallicColor
#endif // metallic

// COLOR ROUGHNESS
#ifdef roughnessCubemapFlag
    #define fetchColorRoughness(texCoord) texture(u_roughnessCubemap, UVtoXYZ(texCoord))
#elif defined(roughnessTextureFlag)
    #define fetchColorRoughness(texCoord) texture(u_roughnessTexture, texCoord)
#endif // roughness

// HEIGHT
#ifdef heightCubemapFlag
    #define fetchHeight(texCoord) texture(u_heightCubemap, UVtoXYZ(texCoord))
#elif defined(heightTextureFlag)
    #define fetchHeight(texCoord) texture(u_heightTexture, texCoord)
#endif // height

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
#endif // directionalLightsFlag

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    #ifdef directionalLightsFlag
    DirectionalLight directionalLights[numDirectionalLights];
    #endif // directionalLightsFlag
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
};
in VertexData v_data;

#ifdef atmosphereGround
in vec4 v_atmosphereColor;
in float v_fadeFactor;
#endif

// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#define saturate(x) clamp(x, 0.0, 1.0)



#ifdef heightFlag
uniform float u_heightScale;
uniform vec2 u_heightSize;
uniform float u_heightNoiseSize;

#define KM_TO_U 1.0E-6
#define HEIGHT_FACTOR 70.0

vec2 parallaxMapping(vec2 texCoords, vec3 viewDir) {
    // number of depth layers
    const float minLayers = 8;
    const float maxLayers = 32;
    float numLayers = mix(maxLayers, minLayers, abs(dot(vec3(0.0, 0.0, 1.0), viewDir)));
    // calculate the size of each layer
    float layerDepth = 1.0 / numLayers;
    // depth of current layer
    float currentLayerDepth = 0.0;

    // the amount to shift the texture coordinates per layer (from vector P)
    vec2 P = viewDir.xy / viewDir.z * u_heightScale * HEIGHT_FACTOR;
    vec2 deltaTexCoords = P / numLayers;

    // get initial values
    vec2  currentTexCoords     = texCoords;
    float currentDepthMapValue = fetchHeight(currentTexCoords).r;

    while(currentLayerDepth < currentDepthMapValue){
        // shift texture coordinates along direction of P
        currentTexCoords -= deltaTexCoords;
        // get depthmap value at current texture coordinates
        currentDepthMapValue = fetchHeight(currentTexCoords).r;
        // get depth of next layer
        currentLayerDepth += layerDepth;
    }

    // get texture coordinates before collision (reverse operations)
    vec2 prevTexCoords = currentTexCoords + deltaTexCoords;

    // get depth after and before collision for linear interpolation
    float afterDepth  = currentDepthMapValue - currentLayerDepth;
    float beforeDepth = texture(u_heightTexture, prevTexCoords).r - currentLayerDepth + layerDepth;

    // interpolation of texture coordinates
    float weight = afterDepth / (afterDepth - beforeDepth);
    vec2 finalTexCoords = prevTexCoords * weight + currentTexCoords * (1.0 - weight);

    return finalTexCoords;
}
#endif // heightFlag

#include shader/lib_logdepthbuff.glsl

// http://www.thetenthplanet.de/archives/1180
mat3 cotangentFrame(vec3 N, vec3 p, vec2 uv){
    // get edge vectors of the pixel triangle
    vec3 dp1 = dFdx( p );
    vec3 dp2 = dFdy( p );
    vec2 duv1 = dFdx( uv );
    vec2 duv2 = dFdy( uv );

    // solve the linear system
    vec3 dp2perp = cross( dp2, N );
    vec3 dp1perp = cross( N, dp1 );
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;

    // construct a scale-invariant frame
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, N );
}

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif // velocityBufferFlag

#ifdef ssrFlag
#include shader/lib_pack.glsl
#endif // ssrFlag

// MAIN
void main() {
    vec2 texCoords = v_data.texCoords;

    vec3 viewDir;
    // TBN and viewDir do not depend on light
    #ifdef heightFlag
        // Compute tangent space
        pullNormal();
        mat3 TBN = cotangentFrame(g_normal, -v_data.viewDir, texCoords);
        viewDir = normalize(v_data.viewDir * TBN);
        // Parallax occlusion mapping
        texCoords = parallaxMapping(texCoords, viewDir);
    #else // heightFlag
        viewDir = v_data.viewDir;
    #endif // heightFlag

    #ifdef directionalLightsFlag
    vec3 lightDir[numDirectionalLights], lightCol[numDirectionalLights];
    for(int i = 0; i < numDirectionalLights; i++) {
        #ifdef heightFlag
        lightDir[i] = normalize(v_data.directionalLights[i].direction * TBN);
        lightCol[i] = v_data.directionalLights[i].color;
        #else // heightFlag
        lightDir[i] = v_data.directionalLights[i].direction;
        lightCol[i] = v_data.directionalLights[i].color;
        #endif // heightFlag
    }
    #endif // directionalLightsFlag

    vec4 diffuse = fetchColorDiffuse(v_data.color, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(texCoords);
    vec3 specular = fetchColorSpecular(texCoords, vec3(0.0, 0.0, 0.0));
    vec3 ambient = v_data.ambientLight;
    #ifdef atmosphereGround
    vec3 night = emissive.rgb;
    emissive = vec4(0.0);
    #else
    vec3 night = vec3(0.0);
    #endif

    // Alpha value from textures
    float texAlpha = 1.0;
    #if defined(diffuseTextureFlag) || defined(diffuseCubemapFlag)
    texAlpha = diffuse.a;
    #elif defined(emissiveTextureFlag)
    texAlpha = luma(emissive.rgb);
    #endif

    vec4 normalVector = vec4(0.0, 0.0, 0.0, 1.0);
    #if defined(normalTextureFlag) || defined(normalCubemapFlag)
        // Normal in tangent space
        vec3 N = normalize(vec3(fetchColorNormal(texCoords) * 2.0 - 1.0));
		#ifdef metallicFlag
            // Perturb the normal to get reflect direction
            pullNormal();
            #ifndef heightFlag
            mat3 TBN = cotangentFrame(g_normal, -v_data.viewDir, texCoords);
            #endif // heighFlag
            normalVector.xyz = TBN * N;
            vec3 reflectDir = normalize(reflect(v_data.fragPosWorld, normalVector.xyz));
		#endif // metallicFlag
    #else
	    // Normal in tangent space
	    vec3 N = vec3(0.0, 0.0, 1.0);
        normalVector.xyz = v_data.normal;
		#ifdef metallicFlag
			vec3 reflectDir = normalize(v_data.reflect);
        #endif // metallicFlag
    #endif // normalTextureFlag

    // Shadow
    #ifdef shadowMapFlag
    float shdw = clamp(getShadow(v_data.shadowMapUv), 0.0, 1.0);
    #else
    float shdw = 1.0;
    #endif

    // Reflection
    vec3 reflectionColor = vec3(0.0);
    // Reflection mask
    #ifdef ssrFlag
        reflectionMask = vec4(0.0, 0.0, 0.0, 1.0);
    #endif // ssrFlag

    #ifdef metallicFlag
        float roughness = 0.0;
        #if defined(roughnessTextureFlag) || defined(roughnessCubemapFlag)
            roughness = fetchColorRoughness(texCoords).x;
        #elif defined(shininessFlag)
            roughness = 1.0 - u_shininess;
        #endif // roughness, shininessFlag

        #ifdef reflectionCubemapFlag
            reflectionColor = texture(u_reflectionCubemap, vec3(-reflectDir.x, reflectDir.y, reflectDir.z), roughness * 7.0).rgb;
        #endif // reflectionCubemapFlag

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

    vec3 shadowColor = vec3(0.0);
    vec3 diffuseColor = vec3(0.0);
    vec3 specularColor = vec3(0.0);
    float selfShadow = 1.0;

    // Loop for directional light contributitons
    #ifdef directionalLightsFlag
    vec3 V = viewDir;
    // Loop for directional light contributitons
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 col = lightCol[i];
        // Skip non-lights
        if (col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
            continue;
        }
        // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
        vec3 L = lightDir[i];
        vec3 H = normalize(L - V);
        float NL = max(0.001, dot(N, L));
        float NH = max(0.001, dot(N, H));

        selfShadow *= saturate(4.0 * NL);

        specularColor += specular * min(1.0, pow(NH, 40.0));
        shadowColor += col * night * max(0.0, 0.5 - NL) * shdw;
        diffuseColor += (col * diffuse.rgb) * NL * shdw + (ambient * diffuse.rgb) * (1.0 - NL);
    }
    #endif // directionalLightsFlag

    // Final color equation
    fragColor = vec4(diffuseColor + shadowColor + emissive.rgb + reflectionColor, texAlpha * v_data.opacity);
    fragColor.rgb += selfShadow * specularColor;

    #ifdef atmosphereGround
    #define exposure 4.0
        fragColor.rgb += (vec3(1.0) - exp(v_atmosphereColor.rgb * -exposure)) * v_atmosphereColor.a * shdw * v_fadeFactor;
    #endif

    // Prevent saturation
    fragColor.rgb = clamp(fragColor.rgb, 0.0, 0.98);
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
    #endif
}
