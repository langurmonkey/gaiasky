#version 410 core

// UNIFORMS
uniform float u_svtId;
uniform float u_svtDepth;
uniform vec2 u_svtResolution;
uniform float u_svtTileSize;

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

#include shader/lib_cubemap.glsl

// The reduction factor of the frame buffer of this pass has an impact on the determined mipmap level.
#define SVT_TILE_DETECTION_REDUCTION_FACTOR 4.0
const float svtDetectionScaleFactor = -log2(SVT_TILE_DETECTION_REDUCTION_FACTOR);

#include shader/lib_mipmap.glsl

void main() {
    // Aspect ratio of virtual texture.
    vec2 ar = vec2(u_svtResolution.x / u_svtResolution.y, 1.0);

    // u_svtDepth is also the maximum mip level, u_svtDepth = log2(svtTextureSize/u_svtTileSize)
    float mip = clamp(floor(mipmapLevel(v_data.texCoords * u_svtResolution, svtDetectionScaleFactor)), 0.0, u_svtDepth);
    float svtLevel = u_svtDepth - mip;
    fragColor.x = svtLevel;

    // Tile XY at the current level.
    float nTilesLevel = pow(2.0, svtLevel);
    vec2 nTilesDimension =  ar * nTilesLevel;
    fragColor.yz = floor(v_data.texCoords * nTilesDimension);

    // ID.
    fragColor.w = u_svtId;
}
