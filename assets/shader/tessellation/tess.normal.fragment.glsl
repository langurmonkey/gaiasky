#version 410 core

////////////////////////////////////////////////////////////////////////////////////
////////// GROUND ATMOSPHERIC SCATTERING - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
in vec4 o_atmosphereColor;

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

in vec4 v_position;
#define pullPosition() { return v_position;}

////////////////////////////////////////////////////////////////////////////////////
////////// COLOR ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec4 o_color;

////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 o_normal;
in vec3 o_normalTan;

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
#define exposure 4.0

in vec2 o_texCoords;

// Varyings computed in the vertex shader
in float o_opacity;

// Other uniforms
#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
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
#define specularFlag
#endif

#if defined(specularFlag) || defined(fogFlag)
#define cameraPositionFlag
#endif

#if	defined(ambientLightFlag) || defined(ambientCubemapFlag) || defined(sphericalHarmonicsFlag)
#define ambientFlag
#endif //ambientFlag

//////////////////////////////////////////////////////
////// SHADOW MAPPING
//////////////////////////////////////////////////////
#ifdef shadowMapFlag
#define bias 0.006
uniform sampler2D u_shadowTexture;
uniform float u_shadowPCFOffset;
in vec3 v_shadowMapUv;

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

float getShadow()
{
    vec2 size = vec2(1.0 / (2.0 * u_shadowPCFOffset));
    float result = 0.0;
    for(int x=-2; x<=2; x++){
        for(int y=-2; y<=2; y++){
            vec2 offset = vec2(float(x), float(y)) / size;
            //result += textureShadowLerp(size, v_shadowMapUv.xy + offset, v_shadowMapUv.z);
            result += getShadowness(v_shadowMapUv.xy, offset, v_shadowMapUv.z);
        }
    }
    return result / 25.0;
}

    #endif //shadowMapFlag


// AMBIENT LIGHT
in vec3 o_ambientLight;

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
#define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord) * u_emissiveColor * 2.0
#elif defined(emissiveTextureFlag)
#define fetchColorEmissiveTD(tex, texCoord) texture(tex, texCoord)
#elif defined(emissiveColorFlag)
#define fetchColorEmissiveTD(tex, texCoord) u_emissiveColor * 2.0
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

// Light direction in world space
in vec3 o_lightDir;
// View direction in world space
in vec3 o_viewDir;
// Light color
in vec3 o_lightCol;
// Logarithmic depth
in float o_depth;

#ifdef environmentCubemapFlag
in vec3 v_reflect;
#endif

#ifdef environmentCubemapFlag
uniform samplerCube u_environmentCubemap;
#endif

#ifdef reflectionColorFlag
uniform vec4 u_reflectionColor;
#endif

out vec4 fragColor;

#define saturate(x) clamp(x, 0.0, 1.0)

#define PI 3.1415926535

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

vec3 perturbNormal(sampler2D normalTexture, vec2 texCoords, mat3 TBN){
    vec3 normalMap = normalize(texture(normalTexture, texCoords).xyz * 2.0 - 1.0);
    return normalize(TBN * normalMap);
}


void main() {
    vec2 texCoords = o_texCoords;

    // Compute tangent space
    mat3 TBN = cotangentFrame(o_normal, -o_viewDir, texCoords);
    vec3 viewDirTangent = normalize(o_viewDir * TBN);
    vec3 lightDirTangent = normalize(o_lightDir * TBN);

    vec4 diffuse = fetchColorDiffuse(o_color, u_diffuseTexture, texCoords, vec4(1.0, 1.0, 1.0, 1.0));
    vec4 emissive = fetchColorEmissive(u_emissiveTexture, texCoords);
    vec3 specular = fetchColorSpecular(texCoords, vec3(0.0, 0.0, 0.0));
    vec3 ambient = o_ambientLight;

    #ifdef normalTextureFlag
    vec3 N = o_normalTan;
    #ifdef environmentCubemapFlag
    vec3 reflectDir = normalize(v_reflect + (vec3(0.0, 0.0, 1.0) - N.xyz));
    #endif // environmentCubemapFlag
    #else
    // Normal in pixel space
    vec3 N = vec3(0.0, 0.0, 1.0);
    #ifdef environmentCubemapFlag
    vec3 reflectDir = normalize(v_reflect);
    #endif // environmentCubemapFlag
    #endif // normalTextureFlag

    // see http://http.developer.nvidia.com/CgTutorial/cg_tutorial_chapter05.html
    vec3 L = lightDirTangent;
    vec3 V = viewDirTangent;
    vec3 H = normalize(L + V);
    float NL = max(0.0, dot(N, L));
    float NH = max(0.0, dot(N, H));

    specular *= min(1.0, pow(NH, 100.0));
    float selfShadow = saturate(4.0 * NL);

    #ifdef environmentCubemapFlag
    vec3 environment = texture(u_environmentCubemap, reflectDir).rgb;
    specular *= environment;
    #ifdef reflectionColorFlag
    diffuse.rgb = (vec3(1.0) - u_reflectionColor.rgb) * diffuse.rgb + environment * u_reflectionColor.rgb;
    #endif // reflectionColorFlag
    #endif // environmentCubemapFlag

    #ifdef shadowMapFlag
    float shdw = clamp(getShadow(), 0.0, 1.0);
    vec3 dayColor = (o_lightCol * diffuse.rgb) * NL * shdw + (ambient * diffuse.rgb) * (1.0 - NL);
    fragColor = vec4(dayColor + emissive.rgb, diffuse.a * o_opacity);
    #else
    vec3 dayColor = (o_lightCol * diffuse.rgb) * NL + (ambient * diffuse.rgb) * (1.0 - NL);
    fragColor = vec4(dayColor + emissive.rgb, diffuse.a * o_opacity);
    #endif // shadowMapFlag

    fragColor.rgb += selfShadow * specular;
    fragColor.rgb += (vec3(1.0) - exp(o_atmosphereColor.rgb * -exposure)) * o_atmosphereColor.a;

    //fragColor = vec4(abs(sin(texCoords * 30.0)), texCoords.x, 1.0);

    if(fragColor.a == 0.0){
        discard;
    }

    // Logarithmic depth buffer
    gl_FragDepth = o_depth;
}

