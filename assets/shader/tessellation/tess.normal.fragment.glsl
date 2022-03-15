#version 330 core

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = o_data.normal

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

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

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
    #ifdef environmentCubemapFlag
    vec3 reflect;
    #endif // environmentCubemapFlag
};
in VertexData o_data;

#ifdef atmosphereGround
in vec4 o_atmosphereColor;
in float o_fadeFactor;
#endif
in vec3 o_normalTan;

#ifdef environmentCubemapFlag
uniform samplerCube u_diffuseCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif


// OUTPUT
layout (location = 0) out vec4 fragColor;

#ifdef ssrFlag
#include shader/lib_ssr.frag.glsl
#endif // ssrFlag

#define saturate(x) clamp(x, 0.0, 1.0)

#include shader/lib_atmfog.glsl
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

// MAIN
void main() {
    vec2 texCoords = o_data.texCoords;

    vec4 diffuse = fetchColorDiffuse(o_data.color, u_diffuseTexture, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(u_emissiveTexture, texCoords);
    vec3 specular = fetchColorSpecular(texCoords, vec3(0.0, 0.0, 0.0));
    vec3 ambient = o_data.ambientLight;
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

    vec3 N = o_normalTan;
    #ifdef normalTextureFlag
        #ifdef environmentCubemapFlag
            // Perturb the normal to get reflect direction
            pullNormal();
            mat3 TBN = cotangentFrame(g_normal, -o_data.viewDir, texCoords);
            vec3 reflectDir = normalize(reflect(o_data.fragPosWorld, normalize(TBN * N)));
        #endif // environmentCubemapFlag
    #else
        #ifdef environmentCubemapFlag
            vec3 reflectDir = normalize(o_data.reflect);
        #endif // environmentCubemapFlag
    #endif // normalTextureFlag

    // Shadow
    #ifdef shadowMapFlag
    float shdw = clamp(getShadow(o_data.shadowMapUv), 0.0, 1.0);
    #else
    float shdw = 1.0;
    #endif
    // Cubemap
    vec3 reflectionColor = vec3(0.0);
    #ifdef environmentCubemapFlag
        reflectionColor = texture(u_diffuseCubemap, vec3(-reflectDir.x, reflectDir.y, reflectDir.z)).rgb;
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
    vec3 fog = vec3(0.0);

    float NL0;
    vec3 L0;

    // Loop for directional light contributitons
    #ifdef directionalLightsFlag
    vec3 V = o_data.viewDir;
    // Loop for directional light contributitons
    for (int i = 0; i < numDirectionalLights; i++) {
        vec3 col = o_data.directionalLights[i].color;
        // Skip non-lights
        if (col.r == 0.0 && col.g == 0.0 && col.b == 0.0) {
            continue;
        }
        // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
        vec3 L = o_data.directionalLights[i].direction;
        vec3 H = normalize(L + V);
        float NL = max(0.0, dot(N, L));
        float NH = max(0.0, dot(N, H));
        if (i == 0){
            NL0 = NL;
            L0 = L;
        }

        selfShadow *= saturate(4.0 * NL);

        specularColor += specular * min(1.0, pow(NH, 40.0));
        shadowColor += col * night * max(0.0, 0.5 - NL) * shdw;
        diffuseColor += (col * diffuse.rgb) * NL * shdw + (ambient * diffuse.rgb) * (1.0 - NL);
    }
    #endif // directionalLightsFlag

    // Final color equation
    fragColor = vec4(diffuseColor + shadowColor + emissive.rgb + reflectionColor, texAlpha * o_data.opacity);
    fragColor.rgb += selfShadow * specularColor;

    #ifdef atmosphereGround
    #define exposure 4.0
    fragColor.rgb += (vec3(1.0) - exp(o_atmosphereColor.rgb * -exposure)) * o_atmosphereColor.a * shdw * o_fadeFactor;
    fragColor.rgb = applyFog(fragColor.rgb, o_data.viewDir, L0 * -1.0, NL0);
    #endif

    // Prevent saturation
    fragColor.rgb = clamp(fragColor.rgb, 0.0, 0.98);

    if (fragColor.a <= 0.0) {
        discard;
    }
    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);

    #ifdef ssrFlag
    ssrBuffers();
    #endif // ssrFlag

    #ifdef velocityBufferFlag
    velocityBuffer();
    #endif // velocityBufferFlag
}
