#version 410 core

// UNIFORMS
uniform float u_svtId;
uniform float u_svtDepth;
uniform vec2 u_svtResolution;
uniform float u_svtTileSize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

// INPUT
in vec2 o_texCoords;

// OUTPUT
layout (location = 0) out vec4 fragColor;

// The reduction factor of the frame buffer of this pass has an impact on the determined mipmap level.
#define SVT_TILE_DETECTION_REDUCTION_FACTOR 2.0
const float svtDetectionScaleFactor = -log2(SVT_TILE_DETECTION_REDUCTION_FACTOR);

#include shader/lib_logdepthbuff.glsl
#include shader/lib_mipmap.glsl

void main() {
    // Aspect ratio of virtual texture.
    vec2 ar = vec2(u_svtResolution.x / u_svtResolution.y, 1.0);

    // u_svtDepth is also the maximum mip level, u_svtDepth = log2(svtTextureSize/u_svtTileSize)
    float mip = clamp(floor(mipmapLevel(o_texCoords * u_svtResolution, svtDetectionScaleFactor)), 0.0, u_svtDepth);
    float svtLevel = u_svtDepth - mip;
    fragColor.r = svtLevel;

    // Tile XY at the current level.
    float nTilesLevel = pow(2.0, svtLevel);
    vec2 nTilesDimension =  ar * nTilesLevel;
    fragColor.gb = floor(o_texCoords * nTilesDimension);

    // ID.
    fragColor.a = u_svtId;

    // Logarithmic depth buffer
    gl_FragDepth = getDepthValue(u_cameraNearFar.y, u_cameraK);
}

