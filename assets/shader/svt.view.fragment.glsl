#version 410 core

// UNIFORMS
uniform float u_svtId;
uniform float u_svtDepth;
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

// Does not take into account GL_TEXTURE_MIN_LOD/GL_TEXTURE_MAX_LOD/GL_TEXTURE_LOD_BIAS
float mipmapLevel(in vec2 texelCoord) {
    vec2  dxVtc        = dFdx(texelCoord);
    vec2  dyVtc        = dFdy(texelCoord);
    float deltaMaxSqr  = max(dot(dxVtc, dxVtc), dot(dyVtc, dyVtc));
    float mml          = 0.5 * log2(deltaMaxSqr);
    return max(0.0, mml);
}

void main() {
    float nTiles = pow(2.0, u_svtDepth);

    // Level
    float svtTextureSize = u_svtTileSize * nTiles;
    vec2 svtDimension = vec2(svtTextureSize * 2.0, svtTextureSize);
    fragColor.x = mipmapLevel(v_data.texCoords.xy * svtDimension) / u_svtDepth;

    // Tile XY
    fragColor.yz = floor(v_data.texCoords.xy * nTiles) / nTiles;

    // ID
    fragColor.w = u_svtId;
}
