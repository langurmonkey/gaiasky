#version 330 core

#define TEXTURE_LOD_BIAS 0.0

////////////////////////////////////////////////////////////////////////////////////
////////// POSITION ATTRIBUTE - FRAGMENT
////////////////////////////////////////////////////////////////////////////////////
#define nop() {}

in vec4 v_position;
#define pullPosition() { return v_position;}


////////////////////////////////////////////////////////////////////////////////////
////////// NORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_normal;
vec3 g_normal = vec3(0.0, 0.0, 1.0);
#define pullNormal() g_normal = v_normal

////////////////////////////////////////////////////////////////////////////////////
////////// BINORMAL ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_binormal;
vec3 g_binormal = vec3(0.0, 0.0, 1.0);
#define pullBinormal() g_binormal = v_binormal

////////////////////////////////////////////////////////////////////////////////////
////////// TANGENT ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
in vec3 v_tangent;
vec3 g_tangent = vec3(1.0, 0.0, 0.0);
#define pullTangent() g_tangent = v_tangent

////////////////////////////////////////////////////////////////////////////////////
////////// TEXCOORD0 ATTRIBUTE - FRAGMENT
///////////////////////////////////////////////////////////////////////////////////
#define exposure 4.0

in vec2 v_texCoord0;

// Uniforms which are always available
uniform mat4 u_projViewTrans;

uniform mat4 u_worldTrans;

uniform vec4 u_cameraPosition;

uniform mat3 u_normalMatrix;

// Varyings computed in the vertex shader
in float v_opacity;
in float v_alphaTest;

// Other uniforms
#ifdef shininessFlag
uniform float u_shininess;
#else
const float u_shininess = 20.0;
#endif

#ifdef diffuseTextureFlag
uniform sampler2D u_diffuseTexture;
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif


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
    return step(compare - bias, dot(texture2D(u_shadowTexture, uv + offset, TEXTURE_LOD_BIAS), bitShifts)); //+(1.0/255.0));
}


float texture2DShadowLerp(vec2 size, vec2 uv, float compare){
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
      // Only PCF
//    float pcf = u_shadowPCFOffset / 2.0;
//    
//    return (//getShadowness(vec2(0,0)) + 
//	getShadowness(v_shadowMapUv.xy, vec2(pcf, pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(-pcf, pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(pcf, -pcf), v_shadowMapUv.z) +
//	getShadowness(v_shadowMapUv.xy, vec2(-pcf, -pcf), v_shadowMapUv.z)) * 0.25;
    
    // Complex lookup: PCF + interpolation (see http://codeflow.org/entries/2013/feb/15/soft-shadow-mapping/)
    vec2 size = vec2(1.0 / (2.0 * u_shadowPCFOffset));
    float result = 0.0;
    for(int x=-2; x<=2; x++){
        for(int y=-2; y<=2; y++){
            vec2 offset = vec2(float(x), float(y)) / size;
            //result += texture2DShadowLerp(size, v_shadowMapUv.xy + offset, v_shadowMapUv.z);
            result += getShadowness(v_shadowMapUv.xy, offset, v_shadowMapUv.z);
        }
    }
    return result / 25.0;
    
    // Simple lookup
    //return getShadowness(v_shadowMapUv.xy, vec2(0.0), v_shadowMapUv.z);
}

#endif //shadowMapFlag


// AMBIENT LIGHT
in vec3 v_ambientLight;

// CLOUD TEXTURE
#if defined(diffuseTextureFlag) && defined(normalTextureFlag)
    // We have clouds and transparency
    vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
        vec4 cloud = texture2D(u_diffuseTexture, texCoord, TEXTURE_LOD_BIAS);
        vec4 trans = texture2D(u_normalTexture, texCoord, TEXTURE_LOD_BIAS);
        return vec4(cloud.rgb, 1.0 - (trans.r + trans.g + trans.b) / 3.0);
    } 
#elif defined(diffuseTextureFlag)
    // Only clouds, we use value as transp
    vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
        vec4 cloud = texture2D(u_diffuseTexture, texCoord, TEXTURE_LOD_BIAS);
        // Smooth towards the poles
        float smoothing = smoothstep(0.01, 0.07, texCoord.y); 
        return vec4(2.0 * cloud.rgb, smoothing * (cloud.r + cloud.g + cloud.b) / 3.0);
    }
#else
    vec4 fetchCloudColor(vec2 texCoord, vec4 defaultValue) {
        return defaultValue;
    }
#endif // diffuseTextureFlag && diffuseColorFlag

in vec3 v_lightDir;
in vec3 v_lightCol;
in vec3 v_viewDir;
#ifdef environmentCubemapFlag
in vec3 v_reflect;
#endif

#define saturate(x) clamp(x, 0.0, 1.0)

#define PI 3.1415926535

in float v_depth;
out vec4 fragColor;

void main() {
    vec2 g_texCoord0 = v_texCoord0;

    vec4 cloud = fetchCloudColor(g_texCoord0, vec4(0.0, 0.0, 0.0, 0.0));
    vec3 ambient = v_ambientLight;
    float ambient_val = (ambient.r + ambient.g + ambient.b) / 4.0;

    // Normal in pixel space
    vec3 N = vec3(0.0, 0.0, 1.0);
    vec3 L = normalize(v_lightDir);
    vec3 V = normalize(v_viewDir);
    vec3 H = normalize(L + V);
    float NL = max(0.0, dot(N, L));
    float NH = max(0.0, dot(N, H));

    float selfShadow = NL;
    
    
    #ifdef shadowMapFlag
    	float shdw = clamp(getShadow(), 0.05, 1.0);
        vec3 cloudColor = (v_lightCol * cloud.rgb) * shdw;
        fragColor = vec4(cloudColor, cloud.a * v_opacity * clamp(NL + ambient_val, 0.0, 1.0));
    #else
        vec3 cloudColor = (v_lightCol * cloud.rgb);
        fragColor = vec4(cloudColor, cloud.a *  v_opacity * clamp(NL + ambient_val, 0.0, 1.0));
    #endif // shadowMapFlag

    //gl_FragColor.rgb += selfShadow;
    
    // Prevent saturation
    fragColor = clamp(fragColor, 0.0, 1.0);
    fragColor.rgb *= 0.95;

    // Normal depth buffer
    // gl_FragDepth = gl_FragCoord.z;
    // Logarithmic depth buffer
    gl_FragDepth = v_depth;
}