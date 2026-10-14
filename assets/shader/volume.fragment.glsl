#version 330 core

// Uniforms which are always available
uniform mat4 u_worldTrans;
uniform vec3 u_cameraPos;
uniform float u_bodySize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;

#ifdef volume0Flag
// Volume texture 0
uniform sampler3D u_volume0;
uniform vec3 u_volume0BoundsMin;
uniform vec3 u_volume0BoundsMax;
uniform int u_volume0ColorMode;
// Minimum density for volume 0 to write depth. 0 or less = this volume never occludes.
uniform float u_volume0DepthThreshold;
#endif //volume0Flag

#ifdef volume1Flag
// Volume texture 1
uniform sampler3D u_volume1;
uniform vec3 u_volume1BoundsMin;
uniform vec3 u_volume1BoundsMax;
uniform int u_volume1ColorMode;
// Minimum density for volume 1 to write depth. 0 or less = this volume never occludes.
uniform float u_volume1DepthThreshold;
#endif //volume1Flag

#ifdef volume2Flag
// Volume texture 2
uniform sampler3D u_volume2;
uniform vec3 u_volume2BoundsMin;
uniform vec3 u_volume2BoundsMax;
uniform int u_volume2ColorMode;
// Minimum density for volume 2 to write depth. 0 or less = this volume never occludes.
uniform float u_volume2DepthThreshold;
#endif //volume2Flag

#ifdef volume3Flag
// Volume texture 3
uniform sampler3D u_volume3;
uniform vec3 u_volume3BoundsMin;
uniform vec3 u_volume3BoundsMax;
uniform int u_volume3ColorMode;
// Minimum density for volume 3 to write depth. 0 or less = this volume never occludes.
uniform float u_volume3DepthThreshold;
#endif //volume3Flag

#if defined(volume0Flag) || defined(volume1Flag) || defined(volume2Flag) || defined(volume3Flag)
#define volumeFlag
#endif // volume[0|1|2|3]Flag

// We use the diffuse channel for volume0.
#ifdef diffuseColorFlag
    uniform vec4 u_diffuseColor;
    #define fetchColorDiffuse(defaultValue) u_diffuseColor
#else
    #define fetchColorDiffuse(defaultValue) defaultValue
#endif // diffuseColorFlag

// INPUT
struct VertexData {
    vec2 texCoords;
    vec3 normal;
    vec3 viewDir;
    vec3 ambientLight;
    float opacity;
    vec4 color;
    vec3 fragPosWorld;
    mat3 tbn;
};
in VertexData v_data;

// OUTPUT
layout(location = 0) out vec4 fragColor;
layout(location = 1) out vec4 layerBuffer;

#include <shader/lib/logdepthbuff.glsl>

#define PI 3.14159265359
#define ITERATIONS 150
#define EXPOSURE 10.0

// Volume colour modes, passed in per slot as u_volumeNColorMode. They describe both where
// the colour comes from and where the density lives, because the two travel together: a
// colour+density (RGBA) volume has its colour in .rgb and its density in .a, whereas a
// density-only (R8) volume keeps its density in .r and stores no colour. These values must
// match VolumeType.COLOR_MODE_* in gaiasky.render.gdx.graphics.VolumeType.
#define COLOR_MODE_RAMP 0         // density in .r, colour from the slot's fixed colour
#define COLOR_MODE_TEXTURE 1      // density in .a, colour from the texture (.rgb)
#define COLOR_MODE_TEXTURE_RAMP 2 // density in .a, colour = texture colour * fixed colour

// Occlusion, per slot, via u_volumeNDepthThreshold. A volume writes depth only where its own
// density exceeds its threshold, and a threshold of 0 (the default) disables the slot for the
// depth pass entirely: it still renders, but nothing can be hidden behind it. An emissive
// volume (gas) should be left at 0 so that its faint outer haze does not cut a hard hole in the
// star field, while an absorbing one (dust) carries a threshold so that only its dense lanes
// occlude and its blurred tail does not.

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

// Convert world position to volume texture coordinates
vec3 worldToVolumeUVW(vec3 worldPos, vec3 boundsMin, vec3 boundsMax) {
    // Transform world position to model space using inverse of u_worldTrans
    vec4 modelSpacePos = vec4(worldPos, 1.0);
    // Normalize model space position to [0,1] within volume bounds
    vec3 volumeSpace = (modelSpacePos.xyz - boundsMin) / (boundsMax - boundsMin);
    return volumeSpace;
}

// Fetch one texel per sample, normalised to (rgb = colour, a = density).
//
// Unpacking both here, from a single fetch, keeps this to one texture read per step. The
// channel the density lives in is a property of the storage layout, and the colour mode
// already tells us which layout is bound: .r for density-only (R8), .a for colour+density
// (RGBA). Note that an RGB volume has no density at all - sampled as a 3D texture its alpha
// reads 1.0 - so it cannot be raymarched and must not be bound to a volume slot.
vec4 sampleVolumeRGBA(vec3 uvw, sampler3D volume, int colorMode) {
    if (any(lessThan(uvw, vec3(0.0))) || any(greaterThan(uvw, vec3(1.0)))) {
        return vec4(0.0);
    }
    vec4 texel = texture(volume, uvw);
    // Density-only: density in R, no colour stored.
    return colorMode == COLOR_MODE_RAMP
    ? vec4(0.0, 0.0, 0.0, texel.r)
    : texel; // colour+density: colour in RGB, density in A
}

// Colour of an emitting sample. `fallback` is this shader's fixed per-slot colour, used when
// the volume carries no colour of its own (mode 0). In texture-ramp mode the fixed colour
// modulates the baked one, which is worth having when the texture's colour is flat across a
// channel - as it is for volumes converted from particles with no colour column of their own.
vec3 volumeColor(vec3 fallback, vec3 texColor, int colorMode) {
    if (colorMode == COLOR_MODE_TEXTURE) {
        return texColor;
    }
    return colorMode == COLOR_MODE_TEXTURE_RAMP ? texColor * fallback : fallback;
}

// Ray-box intersection - returns (entryDistance, exitDistance)
vec2 boxIntersection(vec3 rayOrigin, vec3 rayDir, vec3 boundsMin, vec3 boundsMax) {
    vec3 invDir = 1.0 / rayDir;
    vec3 t0 = (boundsMin - rayOrigin) * invDir;
    vec3 t1 = (boundsMax - rayOrigin) * invDir;

    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);

    float entry = max(max(tmin.x, tmin.y), tmin.z);
    float exit = min(min(tmax.x, tmax.y), tmax.z);

    // Check if intersection is valid
    if (entry > exit || exit < 0.0) {
        return vec2(-1.0);
    }

    // If entry is negative, camera is inside the box
    entry = max(entry, 0.0);

    return vec2(entry, exit);
}

// Transfer function for additive blending - allows saturation to white
vec4 transferFunction(float density, vec3 baseColor, float stepUVW, float transmittance) {
    // Emission: integrate density*baseColor over the normalized step.
    vec3 color = baseColor * density * stepUVW;
    // Opacity: dimensionless per-step coverage. Beer-Lambert or the linear form both work.
    float alpha = clamp(density * stepUVW, 0.0, 1.0);
    return vec4(color, alpha);
}

vec4 raymarchVolume(in sampler3D volume, in vec3 boundsMin, in vec3 boundsMax, in int colorMode, in float depthThreshold, in vec3 color, inout float firstDepth) {
    vec3 rayOrigin = u_cameraPos;
    vec3 rayDir = normalize(v_data.fragPosWorld - u_cameraPos);
    // Scale to size.
    boundsMin = boundsMin * u_bodySize * 0.5;
    boundsMax = boundsMax * u_bodySize * 0.5;

    vec2 intersection = boxIntersection(rayOrigin, rayDir, boundsMin, boundsMax);
    if (intersection.x < 0.0) {
        discard;
    }

    vec4 result = vec4(0.0);
    float entryDist = intersection.x;
    float exitDist = intersection.y;
    float rayLength = exitDist - entryDist;

    vec3 entryPoint = rayOrigin + rayDir * entryDist;
    float stepSize = rayLength / float(ITERATIONS);
    vec3  extent    = boundsMax - boundsMin;
    vec3  uvwDir    = rayDir / extent;              // world dir -> UVW dir
    float stepUVW   = stepSize * length(uvwDir);    // per-sample step in UVW space
    int nSteps = min(ITERATIONS, int(ceil(stepUVW * 2000.0)));

    float transmittance = 1.0;

    float jitter = hash12(gl_FragCoord.xy);
    for (int i = 0; i < nSteps; i++) {
        float currentDist = entryDist + (float(i) + jitter) * stepSize;
        vec3 samplePos = rayOrigin + rayDir * currentDist;
        vec3 uvw = worldToVolumeUVW(samplePos, boundsMin, boundsMax);

        vec4 texel = sampleVolumeRGBA(uvw, volume, colorMode);
        float density = texel.a;

        // Does this sample occlude? Only if the slot is configured as an occluder and its density
        // clears that threshold. Both passes run this same test, so they agree on the depth they
        // write and there is no z-fighting between them.
        bool occludes = depthThreshold > 0.0 && density > depthThreshold;
        if (firstDepth < 0.0 && occludes) {
            firstDepth = currentDist;
        }

    #ifdef DEPTH_PASS
        // Depth-only pre-pass (RenderGroup.VOLUME_DEPTH): the distance to the first occluding
        // sample is all we need, so stop right there - one step instead of the whole
        // integration. The colour pass marches the same ray again, with the same jitter and so
        // the same first sample, and writes the same depth.
        if (occludes) {
            break;
        }
    #else
        // Emission with enhanced transfer function
        if (density > 0.0 && transmittance > 0.001) {
            vec3 baseColor = volumeColor(color, texel.rgb, colorMode);
            vec4 sampl = transferFunction(density, baseColor, stepUVW, transmittance);

            vec3 emittedLight = sampl.rgb * transmittance;

            result.rgb += emittedLight;
            result.a += sampl.a * (1.0 - result.a); // Alpha still uses blending
        }

        // Stop if completely opaque or no light transmission
        if (transmittance < 0.01 || result.a > 0.99) {
            break;
        }
    #endif
    }
    return result;
}

void main() {
    #ifdef volumeFlag
        vec4 result = vec4(0.0);
        float firstDepth = -1.0;

        #ifdef volume0Flag
            result += raymarchVolume(u_volume0, u_volume0BoundsMin, u_volume0BoundsMax, u_volume0ColorMode, u_volume0DepthThreshold, fetchColorDiffuse(vec4(0.3, 0.6, 1.0, 1.0)).rgb, firstDepth);
        #endif //volume0Flag
        #ifdef volume1Flag
            result += raymarchVolume(u_volume1, u_volume1BoundsMin, u_volume1BoundsMax, u_volume1ColorMode, u_volume1DepthThreshold, vec3(0.1, 0.8, 0.2), firstDepth);
        #endif //volume1Flag
        #ifdef volume2Flag
            result += raymarchVolume(u_volume2, u_volume2BoundsMin, u_volume2BoundsMax, u_volume2ColorMode, u_volume2DepthThreshold, vec3(0.0, 0.2, 0.8), firstDepth);
        #endif //volume2Flag
        #ifdef volume3Flag
            result += raymarchVolume(u_volume3, u_volume3BoundsMin, u_volume3BoundsMax, u_volume3ColorMode, u_volume3DepthThreshold, vec3(0.8, 0.2, 0.1), firstDepth);
        #endif //volume3Flag

        // Never discard this fragment: a discarded fragment writes no depth, so a ray that
        // crosses the volume without accumulating enough opacity to be visible would lose its
        // depth write too, and whatever is behind it would show through. An invisible fragment
        // writes an all-zero colour instead, which contributes nothing under additive blending.
        if (result.a > 0.001) {
            // Simple reinhard tone mapping to control brightness
            result.rgb *= EXPOSURE;
            result.rgb = result.rgb / (1.0 + result.rgb);
            fragColor = vec4(result.rgb, result.a) * v_data.opacity;
        } else {
            fragColor = vec4(0.0);
        }

        // Depth of the first sample dense enough to occlude, i.e. of the first sample of a slot
        // whose u_volumeNDepthThreshold it clears. A ray that crossed the boxes without ever
        // meeting such a sample - a purely emissive volume, or material thinner than the
        // threshold - writes the far plane, which cannot occlude anything behind it.
        gl_FragDepth = firstDepth > 0.0 ? getDepthValue(firstDepth, u_cameraNearFar.y, u_cameraK) : 1.0;
    #endif // volumeFlag
}
