#version 410 core

// UNIFORMS
uniform float u_svtId;
uniform float u_svtDepth;
uniform float u_svtDetectionFactor;
uniform vec2 u_svtResolution;
uniform float u_svtTileSize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;


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
    #ifdef shadowMapGlobalFlag
    vec3 shadowMapUvGlobal;
    #endif // shadowMapGlobalFlag
    #ifdef numCSM
    vec3 csmLightSpacePos[numCSM];
    #endif // numCSM
    #endif// shadowMapFlag
    vec3 fragPosWorld;
    #ifdef metallicFlag
    vec3 reflect;
    #endif// metallicFlag
    mat3 tbn;
};
in VertexData v_data;

#ifdef atmosphereGround
in vec4 v_atmosphereColor;
in float v_fadeFactor;
#endif

// OUTPUT
layout (location = 0) out vec4 fragColor;

#include <shader/lib/cubemap.glsl>

// The reduction factor of the frame buffer of this pass has an impact on the determined mipmap level.
float svtDetectionScaleFactor = -log2(u_svtDetectionFactor);

#include <shader/lib/logdepthbuff.glsl>
#include <shader/lib/mipmap.glsl>

void main() {
    // Aspect ratio of virtual texture.
    vec2 ar = vec2(u_svtResolution.x / u_svtResolution.y, 1.0);

    // u_svtDepth is also the maximum mip level, u_svtDepth = log2(svtTextureSize/u_svtTileSize)
    float mip = clamp(floor(mipmapLevel(v_data.texCoords * u_svtResolution, svtDetectionScaleFactor)), 0.0, u_svtDepth);
    float svtLevel = u_svtDepth - mip;
    fragColor.r = svtLevel;

    // Tile XY at the current level.
    float nTilesLevel = pow(2.0, svtLevel);
    vec2 nTilesDimension =  ar * nTilesLevel;
    fragColor.gb = floor(v_data.texCoords * nTilesDimension);

    // ID.
    fragColor.a = u_svtId;

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}
