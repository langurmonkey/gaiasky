#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// GROUND ATMOSPHERIC SCATTERING - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#ifdef atmosphereGround
in vec4 v_atmosphereColor;
#endif

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_data.normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() g_binormal = v_data.binormal

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() g_tangent = v_data.tangent

// Uniforms which are always available
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

#ifdef timeFlag
uniform float u_time;
#else
const float u_time = 0.0;
#endif

#ifdef diffuseColorFlag
uniform vec4 u_diffuseColor;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef specularColorFlag
uniform vec4 u_specularColor;
#endif

#ifdef specularTextureFlag
uniform sampler2D u_specularTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef emissiveColorFlag
uniform vec4 u_emissiveColor;
#endif

#ifdef emissiveTextureFlag
uniform sampler2D u_emissiveTexture;
#endif

#ifdef reflectionTextureFlag
uniform sampler2D u_reflectionTexture;
#endif

#if defined(diffuseTextureFlag) || defined(specularTextureFlag)
#define textureFlag
#endif

#if defined(specularTextureFlag) || defined(specularColorFlag)
#define specularFlag
#endif

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
#define emissiveFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

#if defined(heightTextureFlag)
#define heightFlag
#endif //heightFlag


//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
#define bias 0.006
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
#endif //shadowMapFlag

// COLOR DIFFUSE
#if defined(diffuseTextureFlag) && defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) texture(tex, texCoord) * u_diffuseColor
#elif defined(diffuseTextureFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) texture(tex, texCoord)
#elif defined(diffuseColorFlag)
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuseTD(tex, texCoord, defaultValue) defaultValue
#endif // diffuseTextureFlag && diffuseColorFlag

#if defined(diffuseTextureFlag) || defined(diffuseColorFlag)
    #define fetchColorDiffuse(baseColor, tex, texCoord, defaultValue) baseColor * fetchColorDiffuseTD(tex, texCoord, defaultValue)
#else
    #define fetchColorDiffuse(baseColor, tex, texCoord, defaultValue) baseColor
#endif // diffuseTextureFlag || diffuseColorFlag

// COLOR EMISSIVE
#if defined(emissiveTextureFlag) && defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord) + u_emissiveColor
#elif defined(emissiveTextureFlag)
    #define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord)
#elif defined(emissiveColorFlag)
    #define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor
#endif // emissiveTextureFlag && emissiveColorFlag

#if defined(emissiveTextureFlag) || defined(emissiveColorFlag)
    #define fetchColorEmissive(emissiveTex, texCoord) fetchColorEmissiveTD(emissiveTex, texCoord)
#else
    #define fetchColorEmissive(emissiveTex, texCoord) vec4(0.0, 0.0, 0.0, 0.0)
#endif // emissiveTextureFlag || emissiveColorFlag

// COLOR SPECULAR
#if defined(specularTextureFlag) && defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord).rgb * u_specularColor.rgb
#elif defined(specularTextureFlag)
    #define fetchColorSpecular(texCoord, defaultValue) texture(u_specularTexture, texCoord).rgb
#elif defined(specularColorFlag)
    #define fetchColorSpecular(texCoord, defaultValue) u_specularColor.rgb
#else
    #define fetchColorSpecular(texCoord, defaultValue) defaultValue
#endif // specular

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
#endif // directionalLightsFlag

#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

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
    #ifdef binormalFlag
    vec3 binormal;
    #endif // binormalFlag
    #ifdef tangentFlag
    vec3 tangent;
    #endif // tangentFlag
    #ifdef environmentCubemapFlag
    vec3 reflect;
    #endif // environmentCubemapFlag
};
in VertexData v_data;

// OUTPUT
layout (location = 0) out vec4 fragColor;

#define saturate(x) clamp(x, 0.0, 1.0)
#define PI 3.1415926535

#ifdef heightFlag
uniform sampler2D u_heightTexture;
uniform float u_heightScale;
uniform vec2 u_heightSize;
uniform float u_heightNoiseSize;

#define KM_TO_U 1.0E-6
#define HEIGHT_FACTOR 70.0

#include shader/lib_sampleheight.glsl

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
    float currentDepthMapValue = sampleHeight(u_heightTexture, currentTexCoords).r;

    while(currentLayerDepth < currentDepthMapValue){
        // shift texture coordinates along direction of P
        currentTexCoords -= deltaTexCoords;
        // get depthmap value at current texture coordinates
        currentDepthMapValue = sampleHeight(u_heightTexture, currentTexCoords).r;
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

#include shader/lib_logdepthbuff.glsl

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.frag.glsl
#endif

float luma(vec3 color){
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

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

    vec4 diffuse = fetchColorDiffuse(v_data.color, u_diffuseTexture, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(u_emissiveTexture, texCoords);
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
    #if defined(diffuseTextureFlag)
    texAlpha = diffuse.a;
    #elif defined(emissiveTextureFlag)
    texAlpha = luma(emissive.rgb);
    #endif

    #ifdef normalTextureFlag
        // Normal in tangent space
        vec3 N = normalize(vec3(texture(u_normalTexture, texCoords).xyz * 2.0 - 1.0));
		#ifdef environmentCubemapFlag
            // Perturb the normal to get reflect direction
            pullNormal();
            mat3 TBN = cotangentFrame(g_normal, -v_data.viewDir, texCoords);
			vec3 reflectDir = normalize(reflect(v_data.fragPosWorld, normalize(TBN * N)));
		#endif // environmentCubemapFlag
    #else
	    // Normal in tangent space
	    vec3 N = vec3(0.0, 0.0, 1.0);
		#ifdef environmentCubemapFlag
			vec3 reflectDir = normalize(v_data.reflect);
		#endif // environmentCubemapFlag
    #endif // normalTextureFlag

    // Shadow
    #ifdef shadowMapFlag
    float shdw = clamp(getShadow(v_data.shadowMapUv), 0.0, 1.0);
    #else
    float shdw = 1.0;
    #endif
    // Cubemap
    vec3 reflectionColor = vec3(0.0);
    #ifdef environmentCubemapFlag
        reflectionColor = texture(u_environmentCubemap, reflectDir).rgb;
        #ifdef reflectionTextureFlag
            reflectionColor = reflectionColor * texture(u_reflectionTexture, texCoords).rgb;
        #elif defined(reflectionColorFlag)
            reflectionColor = reflectionColor * u_reflectionColor.rgb;
        #endif // reflectionColorFlag
        reflectionColor += reflectionColor * diffuse.rgb;
    #endif // environmentCubemapFlag

    vec3 shadowColor = vec3(0.0);
    vec3 diffuseColor = vec3(0.0);
    vec3 specularColor = vec3(0.0);
    float selfShadow = 1.0;

    // Loop for directional light contributitons
    #ifdef directionalLightsFlag
    // Loop for directional light contributitons
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 col = lightCol[i];
        // Skip non-lights
        if (i >= 0 && col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
            continue;
        }
        // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
        vec3 L = lightDir[i];
        vec3 V = viewDir;
        vec3 H = normalize(L + V);
        float NL = max(0.0, dot(N, L));
        float NH = max(0.0, dot(N, H));

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
    fragColor.rgb += (vec3(1.0) - exp(v_atmosphereColor.rgb * -exposure)) * v_atmosphereColor.a;
    #endif

    // Prevent saturation
    fragColor.rgb = clamp(fragColor.rgb, 0.0, 0.98);

    if (fragColor.a <= 0.0) {
        discard;
    }
    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif
}
