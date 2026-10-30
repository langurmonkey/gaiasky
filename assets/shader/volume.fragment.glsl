#version 330 core

// Generic volume renderer.
//
// Up to four independent 3D volumes are raymarched and summed with additive blending.
// Nothing here is bound to a particular data set: the box, the density range and the colour
// all arrive through the per-slot uniforms below, so the same shader draws any stack of
// additive volumes. Slot i is enabled by the volume<i>Flag define, which the Java side
// appends when a material declares volume<i>; a slot whose flag is absent costs nothing at
// runtime, only a shader permutation at compile time.

// Uniforms which are always available
uniform mat4 u_worldTrans;
uniform vec3 u_cameraPos;
uniform float u_bodySize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;
// u_time is bound only for the colour pass' march jitter (see the note on the jitter in the
// raymarch); the depth pre-pass must keep a fixed phase so its mask does not boil.
uniform float u_time;
uniform int u_iterations = 90;

#ifdef volume0Flag
// Volume texture 0
uniform sampler3D u_volume0;
uniform vec3 u_volume0BoundsMin;
uniform vec3 u_volume0BoundsMax;
uniform int u_volume0ColorMode;
// Minimum density for volume 0 to write depth. 0 or less = this volume never writes depth.
uniform float u_volume0DepthThreshold;
// Brick occupancy grid of volume 0: one texel per brick of voxels, holding the maximum density
// of the brick. See emptyBrickSteps().
uniform sampler3D u_volume0Occupancy;
#endif //volume0Flag

#ifdef volume1Flag
// Volume texture 1
uniform sampler3D u_volume1;
uniform vec3 u_volume1BoundsMin;
uniform vec3 u_volume1BoundsMax;
uniform int u_volume1ColorMode;
// Minimum density for volume 1 to write depth. 0 or less = this volume never writes depth.
uniform float u_volume1DepthThreshold;
// Brick occupancy grid of volume 1. See emptyBrickSteps().
uniform sampler3D u_volume1Occupancy;
#endif //volume1Flag

#ifdef volume2Flag
// Volume texture 2
uniform sampler3D u_volume2;
uniform vec3 u_volume2BoundsMin;
uniform vec3 u_volume2BoundsMax;
uniform int u_volume2ColorMode;
// Minimum density for volume 2 to write depth. 0 or less = this volume never writes depth.
uniform float u_volume2DepthThreshold;
// Brick occupancy grid of volume 2. See emptyBrickSteps().
uniform sampler3D u_volume2Occupancy;
#endif //volume2Flag

#ifdef volume3Flag
// Volume texture 3
uniform sampler3D u_volume3;
uniform vec3 u_volume3BoundsMin;
uniform vec3 u_volume3BoundsMax;
uniform int u_volume3ColorMode;
// Minimum density for volume 3 to write depth. 0 or less = this volume never writes depth.
uniform float u_volume3DepthThreshold;
// Brick occupancy grid of volume 3. See emptyBrickSteps().
uniform sampler3D u_volume3Occupancy;
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

#ifdef volumeDepthTextureFlag
// This volume's own front: depth of its first sample dense enough to occlude, written by the
// depth-only pre-pass.
uniform sampler2D u_volumeDepthTexture;
// The scene depth buffer as of this pass: geometry plus every volume's composed front.
uniform sampler2D u_volumeSceneDepth;
// Mask texels per fragment of this pass (2 in the half-resolution buffer, 1 otherwise).
uniform vec2 u_volumeDepthMaskScale;

// Distance along the ray at which something other than this volume occludes it, or 0 if nothing does.
// Both depths are stored by the pre-pass, so they compare exactly; a fixed-function depth test would
// compare the volume with its own front instead.
float volumeOcclusionDistance() {
    // One texel for both textures, so where this volume is nearest they hold the same number.
    ivec2 texel = ivec2(gl_FragCoord.xy * u_volumeDepthMaskScale);
    float mine = texelFetch(u_volumeDepthTexture, texel, 0).r;
    float scene = texelFetch(u_volumeSceneDepth, texel, 0).r;
    // Stored log depths: nearer is smaller. 1e-5 is above the 24-bit quantum.
    if (!(scene < mine - 1.0e-5)) {
        return 0.0;
    }
    // View depth of the occluder, converted to a distance along this fragment's ray.
    float sceneDist = 1.0 / recoverWValue(scene, u_cameraNearFar.y, u_cameraK);
    return sceneDist * length(v_data.fragPosWorld) * gl_FragCoord.w;
}
#endif // volumeDepthTextureFlag

#define PI 3.14159265359
#define EXPOSURE 20.0

// Volume colour modes, passed in per slot as u_volumeNColorMode. They describe both
// where the colour comes from and where the density lives, because the two travel
// together: a colour+density (RGBA) volume has its colour in .rgb and its density in
// .a, whereas a density-only (R8) volume keeps its density in .r and stores no colour.
// These values must match VolumeType.COLOR_MODE_* on the Java side.
#define COLOR_MODE_RAMP 0         // density in .r, colour from the ramp
#define COLOR_MODE_TEXTURE 1      // density in .a, colour from the texture (.rgb)
#define COLOR_MODE_TEXTURE_RAMP 2 // density in .a, colour = texture colour * ramp

// Depth writing, per slot, via u_volumeNDepthThreshold. A volume writes depth only where its
// own density exceeds its threshold, and a threshold of 0 (the default) disables the slot for
// the depth pass entirely: the slot still renders, but nothing can be hidden behind it. A
// slot holding a transparent medium usually wants a threshold, so that only its dense parts
// occlude what lies behind the object and its faint outskirts do not cut a hole in the sky.

// ---- Transfer function colour ramp --------------------------------------
// Density -> colour, used by the ramp colour modes (COLOR_MODE_RAMP, and the modulation
// applied in COLOR_MODE_TEXTURE_RAMP). In COLOR_MODE_TEXTURE the per-voxel colour baked into
// the RGBA volume is used directly and the ramp is ignored.
//
// A colour baked into a texture is quantised, and a sparse channel may peak at a few percent
// of full scale, so its density-weighted mean can land at a couple of 8-bit levels per
// channel. The generator therefore applies a single scalar, hue-preserving gain before
// storing it (see --color-normalize in particles-to-3dtex.py). The shader consumes the stored
// colour as-is: the gain is baked into the bytes, and is not undone here.
//
// Stop 0 = thinnest material, stop 1 = mid, stop 2 = densest core.
const vec3 DEFAULT_RAMP[3] = vec3[3](
        vec3(0.0, 0.2, 0.8), vec3(0.1, 0.5, 0.9), vec3(0.8, 0.2, 0.1)
    );

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash12blue(vec2 p) {
    // Blue noise approximation via interleaved gradient noise + bit manipulation
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);

    // XOR-shift mixing for better spectral properties
    uint n = floatBitsToUint(fract((p3.x + p3.y) * p3.z) * 4294967295.0);
    n = (n << 13U) ^ n;
    n = n * (n * n * 15731U + 789221U) + 1376312589U;

    return float(n & 0x7fffffffU) / float(0x7fffffffU);
}

// Map a position in the box's own frame to normalised [0,1] texture coordinates across the
// box. The caller passes the ray's own sample position, so no further transform belongs here.
vec3 worldToVolumeUVW(vec3 pos, vec3 boundsMin, vec3 boundsMax) {
    vec3 volumeSpace = (pos - boundsMin) / (boundsMax - boundsMin);
    return volumeSpace;
}

// How many whole sample steps the ray may jump from a sample that lies in an empty brick.
//
// `occupancy` covers the same box as the volume, one texel per brick of voxels, each holding the
// maximum density of its brick (VolumeTextureLoader.brickOccupancy). A zero texel therefore says
// that every voxel in that brick is zero - and, because the loader dilates the brick occupancy by
// one voxel, that no trilinear fetch anywhere in the brick can read a non-zero voxel either. Every
// sample the march would have taken inside that brick would have fetched a zero, and a zero
// contributes nothing to the sum, so jumping the whole brick cannot change the image.
//
// What it buys: one nearest R8 fetch in place of every full fetch the brick would have cost, and -
// because the caller advances the sample index rather than shrinking the step - the whole set of
// steps the brick spans is never executed.
//
// `span` is the slot's own [entry, exit] along the ray. Outside it the sample reads zero whatever
// the texture holds, because the box clips it, so nothing is constrained there and the function
// says how far the ray has to go to get back in. Slots have independent boxes, so a sample can be
// inside one and outside another.
//
// Returns the number of steps to skip, or 0 when the brick has content in it and the sample has to
// be taken. The result is always >= 1 when it is not 0, so a caller that acts on it always makes
// progress.
float emptyBrickSteps(vec3 uvw, vec3 rayDir, vec3 boundsMin, vec3 boundsMax, sampler3D occupancy,
                      vec2 span, float t, float stepSize) {
    if (t < span.x) {
        // One step short of the entry: the samples before the box are all zero, and stopping short
        // of it means a sample that does land inside can never be jumped.
        return max(1.0, ceil((span.x - t) / stepSize) - 1.0);
    }
    if (t > span.y) {
        return 1.0e9; // the ray has left the box: this slot contributes nothing from here on
    }

    vec3 uvwClamped = clamp(uvw, 0.0, 1.0);
    // Nearest filtering, so this is the brick the sample is in. A slot with no occupancy grid is
    // bound a 1x1x1 volume of ones, which reads 1 here and never skips.
    if (texture(occupancy, uvwClamped).r > 0.0) {
        return 0.0;
    }

    // Distance to the brick's exit plane, i.e. how far the ray can go before the brick changes.
    // Standard DDA: step to the next brick boundary on each axis and keep the nearest.
    vec3 brickGrid = vec3(textureSize(occupancy, 0));
    vec3 g = rayDir * brickGrid / (boundsMax - boundsMin); // brick units per unit world distance
    vec3 b = uvwClamped * brickGrid;
    // floor(b) + 1 on an axis the ray moves along positively, floor(b) where it moves negatively.
    vec3 nextBoundary = floor(b) + max(sign(g), vec3(0.0));
    // An axis the ray is parallel to cannot bound the exit, and 0/0 is a NaN that would poison the
    // min(): give it a denominator it cannot divide by and a distance that cannot win.
    vec3 safeG = sign(g) * max(abs(g), vec3(1.0e-20));
    vec3 distanceToBoundary = (nextBoundary - b) / safeG + step(abs(g), vec3(1.0e-12)) * 1.0e30;
    float exitDistance = min(min(distanceToBoundary.x, distanceToBoundary.y), distanceToBoundary.z);

    // Round up, so the caller never lands a sample inside the brick it is leaving.
    return max(1.0, ceil(exitDistance / stepSize));
}

// Fetch one texel per sample, normalised to (rgb = colour, a = density).
//
// Unpacking both here, from a single fetch, is what keeps this to one texture read per
// step. The channel the density lives in is a property of the storage layout, and the
// colour mode already tells us which layout is bound: .r for density-only (R8), .a for
// colour+density (RGBA). Note that an RGB volume has no density at all - sampled as a
// 3D texture its alpha reads 1.0 - so it cannot be raymarched and must not be bound to
// a volume slot.
vec4 sampleVolumeRGBA(vec3 uvw, sampler3D volume, int colorMode) {
    // Branchless bounds test. This runs once per fetch, and the loop takes one fetch per step per
    // slot for up to u_iterations steps, so with four slots it is the most executed piece of code in
    // the shader - and the `if` it replaces diverges at every silhouette edge, which is exactly
    // where the ray spends its steps. Clamping the coordinate into the volume and scaling the
    // sample by a 0/1 mask gives the same answer for a fraction of the cost, and keeps neighbouring
    // pixels in the same warp on the same path.
    vec3 inVolume = step(vec3(0.0), uvw) * step(uvw, vec3(1.0));
    float mask = inVolume.x * inVolume.y * inVolume.z;
    vec4 texel = texture(volume, clamp(uvw, 0.0, 1.0));
    // Density-only: density in R, no colour stored.
    vec4 sample_d = colorMode == COLOR_MODE_RAMP
    ? vec4(0.0, 0.0, 0.0, texel.r) : texel; // colour+density: colour in RGB, density in A
    return sample_d * mask;
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

// Map a peak-normalised, sparse density to a ramp parameter in [0,1].
// log2 spreads 8-bit values evenly: 1/255 -> 0.125, 16/255 -> 0.54, 1.0 -> 1.0.
// Without this the whole volume lands in stop 0 and the ramp does nothing.
float rampT(float density) {
    return clamp(log2(1.0 + density * 255.0) / 8.0, 0.0, 1.0);
}

vec3 rampColor(in vec3 ramp[3], float t) {
    return t < 0.5
    ? mix(ramp[0], ramp[1], t * 2.0) : mix(ramp[1], ramp[2], (t - 0.5) * 2.0);
}

// Colour of an emitting sample. `texColor` is the RGB of the same fetch the density came
// from, and is ignored in ramp mode. In texture-ramp mode the ramp modulates the baked
// colour, which is worth having when the texture's colour is flat across a channel - as
// it is for volumes converted from particles with no colour column of their own.
vec3 volumeColor(in vec3 ramp[3], float density, vec3 texColor, int colorMode) {
    if (colorMode == COLOR_MODE_TEXTURE) {
        return texColor;
    }
    vec3 rampCol = rampColor(ramp, rampT(density));
    return colorMode == COLOR_MODE_TEXTURE_RAMP ? texColor * rampCol : rampCol;
}

// Transfer function for additive blending - allows saturation to white.
vec4 transferFunction(float density, vec3 baseColor, float stepUVW) {
    // Emission: integrate density*baseColor over the normalized step.
    vec3 color = baseColor * density * stepUVW;
    // Opacity: dimensionless per-step coverage, accumulated only to bound the march.
    float alpha = clamp(density * stepUVW, 0.0, 1.0);
    return vec4(color, alpha);
}

// March one volume over its own box and return the light it contributes along the ray.
// `firstDepth` carries the distance to the nearest depth-writing sample across all slots;
// `intersects` reports whether the ray met this box at all, which is what tells the caller
// whether the fragment is worth shading.
vec4 raymarchVolume(in sampler3D volume, in sampler3D occupancy, in vec3 boundsMin, in vec3 boundsMax, in int colorMode,
    in float depthThreshold, in vec3[3] ramp, inout float firstDepth, out bool intersects, in float clipDist) {
    intersects = false;

    // Rotation part of u_worldTrans (= S·R; strip the uniform scale).
    mat3 W = mat3(u_worldTrans);
    mat3 R = mat3(W[0] / length(W[0]), W[1] / length(W[1]), W[2] / length(W[2]));

    // Frames. The box lives in the object's frame, which is what R maps to the world.
    //  - u_cameraPos is `camera - object` (ModelEntityRenderSystem.renderVolume), so it is already
    //    the camera's position relative to the box origin: rotate it into the box frame and we are
    //    done.
    //  - v_data.fragPosWorld, however, is in the floating camera frame, whose origin IS the camera:
    //    u_worldTrans carries `object - camera` as its translation. So it is the view vector directly
    //    and must NOT have u_cameraPos subtracted from it again - that applies the object-camera
    //    offset twice, which only vanishes when the object sits at the origin (u_cameraPos ~ 0),
    //    as it did in the first test scene.
    vec3 rayOrigin = transpose(R) * u_cameraPos;
    vec3 rayDir = normalize(transpose(R) * v_data.fragPosWorld);
    // Scale to size. The box is normalized - the generator writes it with a side of 1, in
    // units of the object's size, and it is the object's *diameter* - so this is the whole
    // of the mapping from the box to the world: the largest axis spans exactly u_bodySize,
    // and half of it is the volume's radius. The 0.5 that used to be here belonged to the
    // other convention, in which the metadata bounds were in raw data units; keep it in
    // mind if you ever feed this a volume written by a generator before NORMALIZED_SIDE,
    // and note that VolumeTextureLoader.getDefaultBounds() - the unit cube - is only
    // consistent with the box as it is now.
    boundsMin = boundsMin * u_bodySize;
    boundsMax = boundsMax * u_bodySize;

    vec2 intersection = boxIntersection(rayOrigin, rayDir, boundsMin, boundsMax);
    if (intersection.x < 0.0) {
        // This slot's box is not on the ray. Return rather than discard: the slots have
        // independent boxes, so a miss here says nothing about the others, and the caller
        // decides once every slot has been tried whether anything was hit at all.
        return vec4(0.0);
    }
    intersects = true;

    vec4 result = vec4(0.0);
    float entryDist = intersection.x;
    float exitDist = intersection.y;
    // Stop the march at whatever occludes this volume.
    if (clipDist > 0.0) {
        exitDist = min(exitDist, clipDist);
    }
    if (exitDist <= entryDist) {
        return vec4(0.0);
    }
    float rayLength = exitDist - entryDist;

    vec3 extent = boundsMax - boundsMin;
    float uvwScale = length(rayDir / extent); // UVW length per unit world distance
    float pathUVW = rayLength * uvwScale; // total arc length in UVW space
    const float TARGET_STEP_UVW = 0.01;

    int nSteps = clamp(int(ceil(pathUVW / TARGET_STEP_UVW)), 16, u_iterations);
    float stepSize = rayLength / float(nSteps);
    float stepUVW = stepSize * uvwScale; // consistent with the loop, by construction

    // The jitter decorrelates neighbouring pixels so that a step size in UVW does not show as
    // banding across a smooth volume. In the pre-pass there is no banding to hide - it writes a
    // depth, not a colour - and a phase that re-rolls every frame is a mask that boils at its
    // edges and, at a coarse step, flickers over whole dust lanes. So the pre-pass hashes position
    // only: still per pixel, no longer per frame, and the same camera gives the same mask twice.
    // This matters more here than in a single-volume shader, because the slots share one
    // firstDepth: the earliest depth-writing sample of any slot is the one that survives, so an
    // unstable phase in one slot moves the depth written for all of them.
    // Fixed phase in the pre-pass, animated in the colour pass: a static phase turns the march
    // noise into a stipple pattern that survives in a still frame.
    #ifdef DEPTH_PASS
    float jitter = hash12(gl_FragCoord.xy);
    #else
    float jitter = hash12(gl_FragCoord.xy * u_time * 0.1);
    #endif
    for (int i = 0; i < nSteps; i++) {
        float currentDist = entryDist + (float(i) + jitter) * stepSize;
        vec3 samplePos = rayOrigin + rayDir * currentDist;
        vec3 uvw = worldToVolumeUVW(samplePos, boundsMin, boundsMax);

        // Jump the empty bricks instead of stepping through them. The index advances, not the step,
        // so the samples that are still taken sit on exactly the lattice this loop would have used
        // without the skip, and the ones dropped are all inside a brick with no density in it: see
        // emptyBrickSteps().
        float skip = emptyBrickSteps(uvw, rayDir, boundsMin, boundsMax, occupancy,
                                     vec2(entryDist, exitDist), currentDist, stepSize);
        if (skip >= 1.0) {
            i += int(min(skip, float(nSteps))) - 1;
            continue;
        }

        vec4 texel = sampleVolumeRGBA(uvw, volume, colorMode);
        float density = texel.a;

        // Does this sample write depth? Only if the slot is configured as an occluder and its
        // density clears that threshold. Both passes run this same test, so they agree on the
        // depth they write and there is no z-fighting between them.
        bool occludes = depthThreshold > 0.0 && density > depthThreshold;
        if (firstDepth < 0.0 && occludes) {
            firstDepth = currentDist;
        }

        #ifdef DEPTH_PASS
        // Depth-only pre-pass (RenderGroup.VOLUME_DEPTH): the distance to the first depth-writing
        // sample is all we need, so stop right there - one step instead of the whole integration.
        // The colour pass runs the same threshold test over the same samples and computes the same
        // front, which is what makes the depth the two passes write identical - the colour pass
        // tests against it.
        if (occludes) {
            break;
        }
        #else
        // Emission with enhanced transfer function
        if (density > 0.0) {
            vec3 baseColor = volumeColor(ramp, density, texel.rgb, colorMode);
            vec4 sampl = transferFunction(density, baseColor, stepUVW);

            result.rgb += sampl.rgb;
            result.a += sampl.a * (1.0 - result.a); // Alpha still uses blending
        }

        // Stop once the accumulated coverage has saturated: further samples could only add to
        // a value that is already full.
        if (result.a > 0.99) {
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
    bool hit = false;
    bool anyHit = false;

    // Only the colour pass clips: the pre-pass is writing the very depth this would be read from.
    #if defined(volumeDepthTextureFlag) && !defined(DEPTH_PASS)
    float clipDist = volumeOcclusionDistance();
    #else
    float clipDist = 0.0;
    #endif

    // Every slot is raymarched over its own box and the results are summed: additive blending
    // of the incoming volumes. No ordering is modelled between them, and no slot attenuates
    // another.
    #ifdef volume0Flag
    result += raymarchVolume(u_volume0, u_volume0Occupancy, u_volume0BoundsMin, u_volume0BoundsMax, u_volume0ColorMode, u_volume0DepthThreshold, DEFAULT_RAMP, firstDepth, hit, clipDist);
    anyHit = anyHit || hit;
    #endif //volume0Flag
    #ifdef volume1Flag
    result += raymarchVolume(u_volume1, u_volume1Occupancy, u_volume1BoundsMin, u_volume1BoundsMax, u_volume1ColorMode, u_volume1DepthThreshold, DEFAULT_RAMP, firstDepth, hit, clipDist);
    anyHit = anyHit || hit;
    #endif //volume1Flag
    #ifdef volume2Flag
    result += raymarchVolume(u_volume2, u_volume2Occupancy, u_volume2BoundsMin, u_volume2BoundsMax, u_volume2ColorMode, u_volume2DepthThreshold, DEFAULT_RAMP, firstDepth, hit, clipDist);
    anyHit = anyHit || hit;
    #endif //volume2Flag
    #ifdef volume3Flag
    result += raymarchVolume(u_volume3, u_volume3Occupancy, u_volume3BoundsMin, u_volume3BoundsMax, u_volume3ColorMode, u_volume3DepthThreshold, DEFAULT_RAMP, firstDepth, hit, clipDist);
    anyHit = anyHit || hit;
    #endif //volume3Flag

    // A fragment whose ray misses every box has nothing to draw and nothing to write depth for.
    // Discarding it is what keeps the proxy mesh - which encloses the boxes and is far larger
    // than them - from shading the whole of its silhouette.
    if (!anyHit) {
        discard;
    }

    // Never discard a fragment that did hit a box: a discarded fragment writes no depth, and a
    // slot configured to write depth has to be given the chance to. A ray that crossed its box
    // without summing anything writes an all-zero colour, which contributes nothing under
    // additive blending.
    if (result.a > 0.001) {
        result.rgb *= EXPOSURE;
        fragColor = vec4(result.rgb, result.a) * v_data.opacity;
    } else {
        fragColor = vec4(0.0);
    }

    // Depth of the nearest sample dense enough to write depth, i.e. of the first sample of a
    // slot whose u_volumeNDepthThreshold it clears. With every threshold at 0 (the default) no
    // slot ever sets firstDepth, and the fragment writes the far plane: it still draws, but
    // nothing can be hidden behind it.
    //
    // Only the pre-pass writes this; the colour pass runs with the depth test off.
    float depthDist = firstDepth * 0.9999;
    gl_FragDepth = firstDepth > 0.0 ? getDepthValue(depthDist, u_cameraNearFar.y, u_cameraK) : 1.0;
    #endif // volumeFlag
}
