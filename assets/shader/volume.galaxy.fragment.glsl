#version 330 core

// Uniforms which are always available
uniform mat4 u_worldTrans;
uniform vec3 u_cameraPos;
uniform float u_bodySize;
uniform vec2 u_cameraNearFar;
uniform float u_cameraK;
uniform float u_time;
uniform int u_iterations = 90;

#ifdef volume0Flag
// Volume texture 0
uniform sampler3D u_volume0;
uniform vec3 u_volume0BoundsMin;
uniform vec3 u_volume0BoundsMax;
uniform int u_volume0ColorMode;
// Minimum density for volume 0 to write depth. 0 or less = this volume never occludes.
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
// Minimum density for volume 1 to write depth. 0 or less = this volume never occludes.
uniform float u_volume1DepthThreshold;
// Brick occupancy grid of volume 1. See emptyBrickSteps().
uniform sampler3D u_volume1Occupancy;
#endif //volume1Flag

#if defined(volume0Flag) || defined(volume1Flag)
#define volumeFlag
#endif // volume[0|1]Flag

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
#define EXPOSURE 20.0

// Volume colour modes, passed in per slot as u_volumeNColorMode. They describe both
// where the colour comes from and where the density lives, because the two travel
// together: a colour+density (RGBA) volume has its colour in .rgb and its density in
// .a, whereas a density-only (R8) volume keeps its density in .r and stores no colour.
// These values must match VolumeType.COLOR_MODE_* on the Java side.
#define COLOR_MODE_RAMP 0         // density in .r, colour from the ramp
#define COLOR_MODE_TEXTURE 1      // density in .a, colour from the texture (.rgb)
#define COLOR_MODE_TEXTURE_RAMP 2 // density in .a, colour = texture colour * ramp

// Occlusion, per slot, via u_volumeNDepthThreshold. A volume writes depth only where its own
// density exceeds its threshold, and a threshold of 0 (the default) disables the slot for the
// depth pass entirely: it still renders, but nothing can be hidden behind it. Gas is emissive
// and has to stay that way - its faint outer haze is not opaque, so hiding the star field
// behind it would cut a hard hole in the sky - while the absorbing volume (dust) carries a
// threshold, so that only its dense lanes occlude and its blurred tail does not.

// Dust extinction coefficient, per unit (peak-normalised dust density) x (UVW length).
// Gas and dust textures are normalised independently, so this is the only parameter
// that fixes their relative scale. Start at 100, sweep 30-300.
#define DUST_SIGMA 1000.0
// Optional: light *emitted* by the dust (scattering). 0.0 = pure extinction.
#define DUST_EMISSION 0.0

// ---- Transfer function colour ramps --------------------------------------
// Two colour sources, selected per slot by u_volumeNColorMode: these ramps, which map
// density -> colour (mode 0, and the modulation applied in mode 2), and the per-voxel
// colour baked into the RGBA volumes (modes 1 and 2). The ramps remain the default for
// density-only (R8) volumes.
//
// The raw particle colour cannot be quantised as-is - the gas palette peaks at 0.12, so
// its density-weighted mean lands at ~4/255, i.e. black - which is why the generator
// applies a single scalar, hue-preserving gain before storing it (see --color-normalize
// in particles-to-3dtex.py). The shader consumes the stored colour as-is: the gain is
// baked into the bytes, and is not undone here.
//
// Stop 0 = thinnest material, stop 1 = mid, stop 2 = densest core.
const vec3 GAS_RAMP[3] = vec3[3](
        vec3(0.25, 0.45, 1.00), // thin ionised gas -> cool
        vec3(0.70, 0.85, 1.00), // mid
        vec3(1.00, 0.95, 0.85) // dense core -> warm
    );
const vec3 DUST_RAMP[3] = vec3[3](
        vec3(0.35, 0.20, 0.12), // thin dust -> dim brown
        vec3(0.80, 0.60, 0.40), // == what galaxy_ArmDust_color.raw.gz holds
        vec3(1.00, 0.85, 0.60) // dense -> warm tan
    );
const vec3 DEFAULT_RAMP[3] = vec3[3](
        vec3(0.0, 0.2, 0.8), vec3(0.1, 0.5, 0.9), vec3(0.8, 0.2, 0.1)
    );

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

// How many whole sample steps the ray may jump from a sample that lies in an empty brick.
//
// `occupancy` covers the same box as the volume, one texel per brick of voxels, each holding the
// maximum density of its brick (VolumeTextureLoader.brickOccupancy). A zero texel therefore says
// that every voxel in that brick is zero - and, because the loader dilates the brick occupancy by
// one voxel, that no trilinear fetch anywhere in the brick can read a non-zero voxel either. Every
// sample the march would have taken inside that brick would have fetched a zero, and a zero
// contributes no emission and no extinction, so jumping the whole brick cannot change the image.
//
// What it buys: one nearest R8 fetch in place of every full fetch the brick would have cost, and -
// because the caller advances the sample index rather than shrinking the step - the whole set of
// steps the brick spans is never executed. On a galaxy volume that is most of the march.
//
// `span` is the slot's own [entry, exit] along the ray. Outside it the sample reads zero whatever
// the texture holds, because the box clips it, so nothing is constrained there and the function
// says how far the ray has to go to get back in. This matters when one box sits inside another -
// in the gas+dust coupling the dust covers about half of the gas's span - because without it the
// dust would veto every skip it is not itself part of.
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
    // Branchless bounds test. This runs once per fetch, and the loop takes two fetches per step for
    // up to u_iterations steps, so it is the most executed piece of code in the shader - and the `if`
    // it replaces diverges at every silhouette edge, which is exactly where the ray spends its
    // steps. Clamping the coordinate into the volume and scaling the sample by a 0/1 mask gives the
    // same answer for a fraction of the cost, and keeps neighbouring pixels in the same warp on the
    // same path.
    vec3 inVolume = step(vec3(0.0), uvw) * step(uvw, vec3(1.0));
    float mask = inVolume.x * inVolume.y * inVolume.z;
    vec4 texel = texture(volume, clamp(uvw, 0.0, 1.0));
    // Density-only: density in R, no colour stored.
    vec4 sample_d = colorMode == COLOR_MODE_RAMP
    ? vec4(0.0, 0.0, 0.0, texel.r): texel; // colour+density: colour in RGB, density in A
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

// Fraction of light surviving one step through the absorbing volume (Beer-Lambert).
float extinction(float dustDensity, float dustStepUVW) {
    return exp(-DUST_SIGMA * dustDensity * dustStepUVW);
}

vec4 raymarchVolume(in sampler3D volume, in sampler3D occupancy, in vec3 boundsMin, in vec3 boundsMax, in int colorMode, in float depthThreshold, in vec3[3] ramp, inout float firstDepth) {
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
        discard;
    }

    #ifdef DEPTH_PASS
    // This pass writes the distance of the first sample that clears depthThreshold, and a slot whose
    // threshold is 0 or less never produces one: there is nothing to search for in it, so there is
    // nothing to march. Returning leaves firstDepth at -1, which the caller turns into the far
    // plane. Both calls in a given draw carry the same thresholds - they are uniforms - so the
    // branch is coherent and costs the whole draw nothing.
    if (depthThreshold <= 0.0) {
        return vec4(0.0);
    }
    #endif

    vec4 result = vec4(0.0);
    float entryDist = intersection.x;
    float exitDist = intersection.y;
    float rayLength = exitDist - entryDist;

    vec3 extent = boundsMax - boundsMin;
    float uvwScale = length(rayDir / extent); // UVW length per unit world distance
    float pathUVW = rayLength * uvwScale; // total arc length in UVW space
    // The pre-pass has to *find* the first occluding sample, it does not integrate the ray, so it
    // can step further over the same span. What decides whether it finds the dust is the step
    // measured in voxels, and that depends on the box: VolumeTextureLoader crops a volume to the
    // box its density actually fills, which shortens this walk by ~2x on the wide axes, so 0.02
    // here is a finer step per voxel than 0.01 was over the uncropped box. Removing the crop means
    // putting this back to 0.01, or the mask starts stepping over thin dust lanes.
    #ifdef DEPTH_PASS
    const float TARGET_STEP_UVW = 0.02;
    #else
    const float TARGET_STEP_UVW = 0.01;
    #endif

    int nSteps = clamp(int(ceil(pathUVW / TARGET_STEP_UVW)), 16, u_iterations);
    float stepSize = rayLength / float(nSteps);
    float stepUVW = stepSize * uvwScale; // consistent with the loop, by construction

    float transmittance = 1.0;

    // The jitter decorrelates neighbouring pixels so that a step size in UVW does not show as
    // banding across a smooth volume. In the pre-pass there is no banding to hide - it writes a
    // depth, not a colour - and a phase that re-rolls every frame is a mask that boils at its
    // edges and, at a coarse step, flickers over whole dust lanes. So the pre-pass hashes position
    // only: still per pixel, no longer per frame, and the same camera gives the same mask twice.
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

        // Does this sample occlude? Only if the slot is configured as an occluder and its density
        // clears that threshold. The colour pass runs this same test to decide what the dust hides,
        // so the two passes agree on which sample is the front; they no longer agree on the sample
        // *points* (see the note on the jitter below), which they do not have to. Where a pre-pass
        // runs, it is the depth buffer's only writer for this object.
        bool occludes = depthThreshold > 0.0 && density > depthThreshold;
        if (firstDepth < 0.0 && occludes) {
            firstDepth = currentDist;
        }

        #ifdef DEPTH_PASS
        // Depth-only pre-pass (RenderGroup.VOLUME_DEPTH): the distance to the first occluding
        // sample is all we need, so stop right there - one step instead of the whole integration.
        // The colour pass marches the same ray again and finds the same front, and is drawn with
        // the depth test off, so this is the distance that ends up in the buffer.
        if (occludes) {
            break;
        }
        #else
        // Emission with enhanced transfer function
        if (density > 0.0 && transmittance > 0.001) {
            vec3 baseColor = volumeColor(ramp, density, texel.rgb, colorMode);
            // Use accumulated density for saturation effects
            vec4 sampl = transferFunction(density, baseColor, stepUVW);

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

// Coupled raymarch. `emissiveVolume` (gas) is integrated as emission; `opaqueVolume`
// (dust) attenuates whatever lies behind it. Both are sampled at the same position
// along the ray, in the same loop - that is what makes the ordering correct, and the
// distinction between a dust lane and a dust glow.
vec4 raymarchVolumeOccluded(in sampler3D emissiveVolume, in sampler3D emissiveOccupancy,
    in vec3 emissiveBoundsMin, in vec3 emissiveBoundsMax,
    in int emissiveColorMode, in float emissiveDepthThreshold,
    in sampler3D opaqueVolume, in sampler3D opaqueOccupancy,
    in vec3 opaqueBoundsMin, in vec3 opaqueBoundsMax,
    in int opaqueColorMode, in float opaqueDepthThreshold,
    in vec3[3] ramp, inout float firstDepth) {

    // Rotation part of u_worldTrans (= S·R; strip the uniform scale). See the frame note in
    // raymarchVolume(): the camera's position in the box frame is the rotated u_cameraPos, and the
    // fragment position is already camera-relative.
    mat3 W = mat3(u_worldTrans);
    mat3 R = mat3(W[0] / length(W[0]), W[1] / length(W[1]), W[2] / length(W[2]));
    vec3 rayOrigin = transpose(R) * u_cameraPos;
    vec3 rayDir = normalize(transpose(R) * v_data.fragPosWorld);

    // Scale to size. The boxes are normalized (see raymarchVolume): the metadata bounds are
    // in units of the object's size, so no further factor belongs here.
    emissiveBoundsMin *= u_bodySize;
    emissiveBoundsMax *= u_bodySize;
    opaqueBoundsMin *= u_bodySize;
    opaqueBoundsMax *= u_bodySize;

    vec2 iEmissive = boxIntersection(rayOrigin, rayDir, emissiveBoundsMin, emissiveBoundsMax);
    vec2 iOpaque = boxIntersection(rayOrigin, rayDir, opaqueBoundsMin, opaqueBoundsMax);

    #ifdef DEPTH_PASS
    // The pre-pass looks for the first sample that clears a threshold, so a slot without one cannot
    // contribute to it: not a sample, and not a span either. Dropping its box here is what keeps the
    // search inside the occluding volume - in the gas+dust coupling, the dust alone, already cropped
    // by the loader to the box its density fills. A ray that misses the dust then costs nothing at
    // all, instead of marching the whole gas box looking for a front that cannot be in it.
    if (emissiveDepthThreshold <= 0.0) {
        iEmissive = vec2(-1.0);
    }
    if (opaqueDepthThreshold <= 0.0) {
        iOpaque = vec2(-1.0);
    }
    #endif

    // Discard only if *both* boxes are missed. A ray that clips one volume and not
    // the other must still render the one it hits.
    if (iEmissive.x < 0.0 && iOpaque.x < 0.0) {
        #ifdef DEPTH_PASS
        // The pre-pass wants the far plane written, not the fragment dropped. It is drawing a mask,
        // and "the buffer was left alone" only means "nothing occludes here" if what was already in
        // it was the far plane - true of a cleared buffer, and the depth clear is itself subject to
        // the depth write mask. Returning zero colour leaves firstDepth at -1, which main() turns
        // into the far plane. (The colour pass keeps the discard: a fragment whose ray meets no
        // volume has no light to add and no depth of its own to write.)
        return vec4(0.0);
        #else
        discard;
        #endif
    }

    // March the union of the two spans: from the nearest entry to the farthest exit.
    // Outside its own box a volume samples as zero, so a span covered by only one of
    // them costs samples but contributes nothing.
    float entryDist = 1.0e30;
    float exitDist = -1.0e30;
    if (iEmissive.x >= 0.0) {
        entryDist = min(entryDist, iEmissive.x);
        exitDist = max(exitDist, iEmissive.y);
    }
    if (iOpaque.x >= 0.0) {
        entryDist = min(entryDist, iOpaque.x);
        exitDist = max(exitDist, iOpaque.y);
    }
    float rayLength = exitDist - entryDist;

    // Each volume lives in its own UVW space, so the per-sample step has to be
    // expressed in the space of the volume being sampled.
    vec3 emissiveExtent = emissiveBoundsMax - emissiveBoundsMin;
    vec3 opaqueExtent = opaqueBoundsMax - opaqueBoundsMin;
    float uvwScaleEmissive = length(rayDir / emissiveExtent);
    float uvwScaleOpaque = length(rayDir / opaqueExtent);

    #ifdef DEPTH_PASS
    // Only a box that is actually being marched may set the step. A slot with no threshold was
    // dropped from the span above by the very same test, and its scale has to go with it: left in,
    // it would only make the pre-pass walk the occluder in finer steps than the occluder needs.
    if (emissiveDepthThreshold <= 0.0) {
        uvwScaleEmissive = 0.0;
    }
    if (opaqueDepthThreshold <= 0.0) {
        uvwScaleOpaque = 0.0;
    }
    #endif

    // Size the step from the finer of the two, so neither volume is undersampled. That `max` is also
    // what keeps the pre-pass honest: it takes the finer of the boxes actually present, and the
    // occluding volume is the cropped one - see the TARGET_STEP_UVW note in raymarchVolume().
    float pathUVW = rayLength * max(uvwScaleEmissive, uvwScaleOpaque);
    #ifdef DEPTH_PASS
    const float TARGET_STEP_UVW = 0.02;
    #else
    const float TARGET_STEP_UVW = 0.01;
    #endif
    int nSteps = clamp(int(ceil(pathUVW / TARGET_STEP_UVW)), 16, u_iterations);
    float stepSize = rayLength / float(nSteps);
    float stepUVWEmissive = stepSize * uvwScaleEmissive;
    float stepUVWOpaque = stepSize * uvwScaleOpaque;

    vec3 emitted = vec3(0.0);
    float transmittance = 1.0;
    float alpha = 0.0;

    // See the note on the jitter in raymarchVolume(): the pre-pass phase must not change with time.
    #ifdef DEPTH_PASS
    float jitter = hash12(gl_FragCoord.xy);
    #else
    float jitter = hash12(gl_FragCoord.xy * u_time * 0.01);
    #endif
    for (int i = 0; i < nSteps; i++) {
        float currentDist = entryDist + (float(i) + jitter) * stepSize;
        vec3 samplePos = rayOrigin + rayDir * currentDist;
        vec3 uvwEmissive = worldToVolumeUVW(samplePos, emissiveBoundsMin, emissiveBoundsMax);
        vec3 uvwOpaque = worldToVolumeUVW(samplePos, opaqueBoundsMin, opaqueBoundsMax);

        // Jump the bricks that are empty in every volume this pass reads. One that is empty in only
        // one of them still gets sampled on its own axis - the other may well have light in it - so
        // the skip is only taken when all of them agree there is nothing here. See
        // emptyBrickSteps(), including what a slot whose span does not cover this sample says.
        float skip = min(
                emptyBrickSteps(uvwEmissive, rayDir, emissiveBoundsMin, emissiveBoundsMax, emissiveOccupancy, iEmissive, currentDist, stepSize),
                emptyBrickSteps(uvwOpaque, rayDir, opaqueBoundsMin, opaqueBoundsMax, opaqueOccupancy, iOpaque, currentDist, stepSize));
        if (skip >= 1.0) {
            i += int(min(skip, float(nSteps))) - 1;
            continue;
        }

        #ifdef DEPTH_PASS
        // Only the slots that can occlude are read. `occludes` below tests a threshold, and a slot
        // whose threshold is 0 or less fails that test whatever its texel holds, so reading it would
        // be a texture fetch per step thrown away. The threshold is a uniform, so the branch is the
        // same for the whole draw and the fetch is skipped outright. The colour pass needs both
        // textures wherever they emit, and takes them in the #else.
        float densityEmissive = 0.0;
        float densityOpaque = 0.0;
        if (emissiveDepthThreshold > 0.0) {
            densityEmissive = sampleVolumeRGBA(uvwEmissive, emissiveVolume, emissiveColorMode).a;
        }
        if (opaqueDepthThreshold > 0.0) {
            densityOpaque = sampleVolumeRGBA(uvwOpaque, opaqueVolume, opaqueColorMode).a;
        }
        #else
        vec4 texelEmissive = sampleVolumeRGBA(uvwEmissive, emissiveVolume, emissiveColorMode);
        vec4 texelOpaque = sampleVolumeRGBA(uvwOpaque, opaqueVolume, opaqueColorMode);
        float densityEmissive = texelEmissive.a;
        float densityOpaque = texelOpaque.a;
        #endif

        // Either volume may be the occluder, and each carries its own threshold; a slot whose
        // threshold is 0 or less is skipped. In the gas+dust coupling this is what makes the dust
        // lanes hide what is behind them while the gas keeps emitting straight through them.
        bool occludes = (emissiveDepthThreshold > 0.0 && densityEmissive > emissiveDepthThreshold)
                || (opaqueDepthThreshold > 0.0 && densityOpaque > opaqueDepthThreshold);
        if (firstDepth < 0.0 && occludes) {
            firstDepth = currentDist;
        }

        #ifdef DEPTH_PASS
        // Depth-only pre-pass: the distance to the first occluding sample is all we need.
        if (occludes) {
            break;
        }
        #else
        // Emission first: light emitted within this step lies *in front of* the dust
        // in this step, so it is not attenuated by it.
        if (densityEmissive > 0.0) {
            vec3 baseColor = volumeColor(ramp, densityEmissive, texelEmissive.rgb, emissiveColorMode);
            vec4 sampl = transferFunction(densityEmissive, baseColor, stepUVWEmissive);
            emitted += sampl.rgb * transmittance;
            alpha += sampl.a * (1.0 - alpha);
        }

        // Optional dust glow (scattering). Disabled while DUST_EMISSION == 0.
        if (densityOpaque > 0.0 && DUST_EMISSION > 0.0) {
            emitted += volumeColor(DUST_RAMP, densityOpaque, texelOpaque.rgb, opaqueColorMode) * densityOpaque * stepUVWOpaque * DUST_EMISSION;
        }

        // Then the dust absorbs only what is behind it.
        transmittance *= extinction(densityOpaque, stepUVWOpaque);

        if (transmittance < 0.001) {
            break;
        }
        #endif
    }

    return vec4(emitted, alpha);
}

void main() {
    #ifdef volumeFlag
    vec4 result = vec4(0.0);
    float firstDepth = -1.0;

    #if defined(volume0Flag) && defined(volume1Flag)
    // Gas (volume0) emissive, ArmDust (volume1) absorbing. The scene gives the dust a depth
    // threshold and leaves the gas at 0, so only the dust lanes occlude the passes that follow.
    result += raymarchVolumeOccluded(u_volume0, u_volume0Occupancy, u_volume0BoundsMin, u_volume0BoundsMax, u_volume0ColorMode, u_volume0DepthThreshold,
            u_volume1, u_volume1Occupancy, u_volume1BoundsMin, u_volume1BoundsMax, u_volume1ColorMode, u_volume1DepthThreshold,
            GAS_RAMP, firstDepth);
    #elif defined(volume0Flag)
    result += raymarchVolume(u_volume0, u_volume0Occupancy, u_volume0BoundsMin, u_volume0BoundsMax, u_volume0ColorMode, u_volume0DepthThreshold, GAS_RAMP, firstDepth);
    #elif defined(volume1Flag)
    // Dust alone: pure extinction with no emissive volume behind it. Nothing to
    // occlude, so it only contributes its own (currently disabled) glow.
    result += raymarchVolume(u_volume1, u_volume1Occupancy, u_volume1BoundsMin, u_volume1BoundsMax, u_volume1ColorMode, u_volume1DepthThreshold, DUST_RAMP, firstDepth);
    #endif //volume0Flag && volume1Flag

    // Never discard this fragment: a discarded fragment writes no depth, and the dust has to
    // occlude. With dust modelled as extinction, a ray that crosses dust but no gas emits
    // nothing at all (alpha == 0 here), so under a discard it wrote no depth either and
    // everything behind the dust showed through it. An invisible fragment writes an all-zero
    // colour instead, which contributes nothing under additive blending.
    if (result.a > 0.001) {
        result.rgb *= EXPOSURE;
        // Simple reinhard tone mapping to control brightness
        // result.rgb = result.rgb / (1.0 + result.rgb);
        fragColor = vec4(result.rgb, result.a) * v_data.opacity;
    } else {
        fragColor = vec4(0.0);
    }

    // Depth of the first sample dense enough to occlude, i.e. of the first sample of a slot
    // whose u_volumeNDepthThreshold it clears. A ray that crossed the boxes without ever meeting
    // such a sample - pure gas, or dust thinner than its threshold - writes the far plane, which
    // cannot occlude anything behind it. That is what keeps the gas from cutting a hard hole in
    // the star field while the dust lanes do hide it.
    // firstDepth is along the ray; the buffer wants the view depth of the same point.
    // The fragment and the sample lie on the same ray, so cos of the angle to the view
    // axis is (view depth of the fragment) / (distance to the fragment).
    float viewDepth = (1.0 / gl_FragCoord.w) * (firstDepth / length(v_data.fragPosWorld));
    gl_FragDepth = firstDepth > 0.0 ? getDepthValue(viewDepth, u_cameraNearFar.y, u_cameraK) : 1.0;
    #endif // volumeFlag
}
