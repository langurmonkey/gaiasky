#version 410 core

layout (triangles) in;

#define tessellationEvaluationShader

// GEOMETRY (QUATERNIONS)
#if defined(velocityBufferFlag) || defined(relativisticEffects)
#include shader/lib_geometry.glsl
#endif

#ifdef normalTextureFlag
uniform sampler2D u_normalTexture;
#endif

#ifdef normalCubemapFlag
uniform samplerCube u_normalCubemap;
#endif

#ifdef svtIndirectionNormalTextureFlag
uniform sampler2D u_svtIndirectionNormalTexture;
#endif

#ifdef heightTextureFlag
uniform sampler2D u_heightTexture;
#endif

#ifdef heightCubemapFlag
uniform samplerCube u_heightCubemap;
#endif

#ifdef svtIndirectionHeightTextureFlag
uniform sampler2D u_svtIndirectionHeightTexture;
#endif

#ifdef svtCacheTextureFlag
uniform sampler2D u_svtCacheTexture;
#endif

#ifdef cubemapFlag
    #include shader/lib_cubemap.glsl
#endif // cubemapFlag

#ifdef svtFlag
    #include shader/lib_svt.glsl
#endif // svtFlag

// COLOR NORMAL
#if defined(svtIndirectionNormalTextureFlag)
    #define fetchColorNormal(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionNormalTexture, texCoord))
#elif defined(normalCubemapFlag)
    #define fetchColorNormal(texCoord) texture(u_normalCubemap, UVtoXYZ(texCoord))
#elif defined(normalTextureFlag)
    #define fetchColorNormal(texCoord) texture(u_normalTexture, texCoord)
#endif // normal

// HEIGHT
#if defined(svtIndirectionHeightTextureFlag)
    #define fetchHeight(texCoord) texture(u_svtCacheTexture, svtTexCoords(u_svtIndirectionHeightTexture, texCoord))
#elif defined(heightCubemapFlag)
    #define fetchHeight(texCoord) texture(u_heightCubemap, UVtoXYZ(texCoord))
#elif defined(heightTextureFlag)
    #define fetchHeight(texCoord) texture(u_heightTexture, texCoord)
#else
    #define fetchHeight(texCoord) vec4(0.0)
#endif // height

////////////////////////////////////////////////////////////////////////////////////
//////////RELATIVISTIC EFFECTS - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef relativisticEffects
#include shader/lib_relativity.glsl
#endif// relativisticEffects

////////////////////////////////////////////////////////////////////////////////////
//////////GRAVITATIONAL WAVES - VERTEX
////////////////////////////////////////////////////////////////////////////////////
#ifdef gravitationalWaves
#include shader/lib_gravwaves.glsl
#endif// gravitationalWaves

uniform mat4 u_projViewTrans;

uniform float u_heightScale;
uniform float u_heightNoiseSize;
uniform vec2 u_heightSize;
uniform float u_vrScale;

#if defined(numDirectionalLights) && (numDirectionalLights > 0)
#define directionalLightsFlag
#endif // numDirectionalLights

#ifdef directionalLightsFlag
struct DirectionalLight {
    vec3 color;
    vec3 direction;
};
#endif // directionalLightsFlag

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
    #ifdef reflectionCubemapFlag
    vec3 reflect;
    #endif // reflectionCubemapFlag
};
// INPUT
in VertexData l_data[gl_MaxPatchVertices];
#ifdef atmosphereGround
in vec4 l_atmosphereColor[gl_MaxPatchVertices];
in float l_fadeFactor[gl_MaxPatchVertices];
#endif // atmosphereGround

// OUTPUT
out VertexData o_data;
#ifdef atmosphereGround
out vec4 o_atmosphereColor;
out float o_fadeFactor;
#endif
out vec3 o_normalTan;
out vec3 o_fragPosition;
out float o_fragHeight;

#ifdef velocityBufferFlag
#include shader/lib_velbuffer.vert.glsl
#endif

#if defined(normalCubemapFlag) || defined(normalTextureFlag) || defined(svtIndirectionNormalTextureFlag)
    // Use normal map
    vec3 calcNormal(vec2 p, vec2 dp) {
        return normalize(fetchColorNormal(p).rgb * 2.0 - 1.0);
    }
#elif defined(heightCubemapFlag) || defined(heightTextureFlag) || defined(svtIndirectionHeightTextureFlag)
    // maps the height scale in internal units to a normal strength
    float computeNormalStrength(float heightScale){
        // to [0,100] km
        vec2 heightSpanKm = vec2(0.0, 100.0);
        vec2 span = vec2(0.2, 1.0);
        heightScale *= u_vrScale * 1e6;
        heightScale = clamp(heightScale, heightSpanKm.x, heightSpanKm.y);
        // normalize to [0,1]
        heightScale = (heightSpanKm.y - heightScale) / (heightSpanKm.y - heightSpanKm.x);
        return span.x + (span.y - span.x) * heightScale;
    }
    // Use height texture for normals
    vec3 calcNormal(vec2 p, vec2 dp){
        vec4 h;
        vec2 size = vec2(computeNormalStrength(u_heightScale), 0.0);
        if (dp.x < 0.0){
            // Generated height using perlin noise
            dp = vec2(3e-4);
        }
        h.x = fetchHeight(vec2(p.x - dp.x, p.y)).r;
        h.y = fetchHeight(vec2(p.x + dp.x, p.y)).r;
        h.z = fetchHeight(vec2(p.x, p.y - dp.y)).r;
        h.w = fetchHeight(vec2(p.x, p.y + dp.y)).r;
        vec3 va = normalize(vec3(size.xy, -h.x + h.y));
        vec3 vb = normalize(vec3(size.yx, -h.z + h.w));
        vec3 n = cross(va, vb);
        return normalize(n);
    }
#else
    vec3 calcNormal(vec2 p, vec2 dp){
        return vec3(0.0);
    }
#endif // normalTextureFlag

void main(void){
    float u = gl_TessCoord.x;
    float v = gl_TessCoord.y;
    float w = gl_TessCoord.z;

    vec4 pos = (u * gl_in[0].gl_Position +
    v * gl_in[1].gl_Position +
    w * gl_in[2].gl_Position);

    o_data.texCoords = (u * l_data[0].texCoords + v * l_data[1].texCoords + w * l_data[2].texCoords);

    // Normal to apply height
    o_data.normal = normalize(u * l_data[0].normal + v * l_data[1].normal + w * l_data[2].normal);

    // Use height texture to move vertex along normal
    float h = fetchHeight(o_data.texCoords).r;
    o_fragHeight = h * u_heightScale;
    vec3 dh = o_data.normal * o_fragHeight;
    pos += vec4(dh, 0.0);


    #ifdef relativisticEffects
    pos.xyz = computeRelativisticAberration(pos.xyz, length(pos.xyz), u_velDir, u_vc);
    #endif// relativisticEffects

    #ifdef gravitationalWaves
    pos.xyz = computeGravitationalWaves(pos.xyz, u_gw, u_gwmat3, u_ts, u_omgw, u_hterms);
    #endif// gravitationalWaves

    vec4 gpos = u_projViewTrans * pos;
    gl_Position = gpos;

    #ifdef velocityBufferFlag
    velocityBufferCam(gpos, pos);
    #endif// velocityBufferFlag

    // Plumbing
    o_fragPosition = pos.xyz;
    o_normalTan = calcNormal(o_data.texCoords, vec2(1.0 / u_heightSize.x, 1.0 / u_heightSize.y));
    o_data.opacity = (u * l_data[0].opacity + v * l_data[1].opacity + w * l_data[2].opacity);
    o_data.color = (u * l_data[0].color + v * l_data[1].color + w * l_data[2].color);
    o_data.viewDir = (u * l_data[0].viewDir + v * l_data[1].viewDir + w * l_data[2].viewDir);
    o_data.fragPosWorld = (u * l_data[0].fragPosWorld + v * l_data[1].fragPosWorld + w * l_data[2].fragPosWorld);
    #ifdef directionalLightsFlag
    for (int i = 0; i < numDirectionalLights; i++){
        o_data.directionalLights[i].direction = (u * l_data[0].directionalLights[i].direction + v * l_data[1].directionalLights[i].direction + w * l_data[2].directionalLights[i].direction);
        o_data.directionalLights[i].color = (u * l_data[0].directionalLights[i].color + v * l_data[1].directionalLights[i].color + w * l_data[2].directionalLights[i].color);
    }
    #endif // directionalLightsFlag
    o_data.ambientLight = (u * l_data[0].ambientLight + v * l_data[1].ambientLight + w * l_data[2].ambientLight);
    #ifdef reflectionCubemapFlag
    o_data.reflect = (u * l_data[0].reflect + v * l_data[1].reflect + w * l_data[2].reflect);
    #endif // reflectionCubemapFlag

    #ifdef atmosphereGround
    o_atmosphereColor = (u * l_atmosphereColor[0] + v * l_atmosphereColor[1] + w * l_atmosphereColor[2]);
    o_fadeFactor = (u * l_fadeFactor[0] + v * l_fadeFactor[1] + w * l_fadeFactor[2]);
    #endif

    #ifdef shadowMapFlag
    o_data.shadowMapUv = (u * l_data[0].shadowMapUv + v * l_data[1].shadowMapUv + w * l_data[2].shadowMapUv);
    #endif // shadowMapFlag
}
