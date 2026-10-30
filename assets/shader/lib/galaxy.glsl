#ifndef GLSL_LIB_GALAXY
#define GLSL_LIB_GALAXY

// ---- Procedural galaxy density field ----
//
// This is an analytic field, not a sampler: every value is a continuous function of the
// position, which is what lets it be voxelised directly into a 3D texture with one write per
// voxel and no atomics, no blur kernel and no particle intermediate.
//
// Positions are in "galaxy space", the same space the billboard particle generators work in:
// x and z span the disk plane, y is vertical, and the galaxy extends out to baseRadius.
//
// galaxyDensity() returns vec4(colour, density):
//   .rgb  chromaticity in [0,1], a function of radius only (core -> outskirts)
//   .a    density in source units, not normalised; the caller peak-normalises it
//
// The colour and the density are stored in the channels the volume raymarcher already reads
// in COLOR_MODE_TEXTURE: colour in .rgb, density in .a, one fetch per sample.
//
// Nothing here calls rand(): there is no seed, no particle count and no discretisation left,
// so the field is resolution-independent and changing a parameter never needs a reseed. The one
// exception is the lane noise, whose octaves are clamped to what the voxel size can hold
// (gxNoiseFbm): that is a coarser lane, not a different pattern.

#include <shader/lib/distributions.glsl>

// Distribution ids. These must match the BillboardDataset.Distribution ordinals.
#define GX_SPHERE 0
#define GX_DISK 1
#define GX_SPIRAL 2
#define GX_SPIRAL_LOG 3
#define GX_BAR 4
#define GX_ELLIPSOID 5
#define GX_DISK_GAUSS 6
#define GX_SPHERE_GAUSS 7
#define GX_CONE 8
#define GX_IRREGULAR 9

// Height profile ids. These must match the BillboardDataset.HeightProfile ordinals.
#define GX_HEIGHT_CONSTANT 0
#define GX_HEIGHT_LINEAR_INC 1
#define GX_HEIGHT_LINEAR_DEC 2
#define GX_HEIGHT_SMOOTH_INC 3
#define GX_HEIGHT_SMOOTH_DEC 4

// Width of the arm in units of baseRadius, as in the particle generator (armWidth = 0.2 * baseRadius).
#define GX_ARM_WIDTH 0.2
// Scale radius of the exponential disk, in units of baseRadius.
#define GX_DISK_SCALE 0.33
// Vertical slab is cut at this many scale heights, where its density is below 1/255.
#define GX_VERTICAL_TAIL 3.0

// Shape parameters of one distribution. Mirrors the BillboardDataset.ChannelType fields one
// to one, so a descriptor needs no new keys: the numbers that drove the particle sampler are
// the numbers the field is built from.
struct GalaxyShape {
    // Distribution id, one of the GX_* constants.
    int distribution;
    // Outer radius of the distribution. The galaxy is inside this sphere/cylinder.
    float baseRadius;
    // Inner radius: spirals start their arms here, spheres start their volume here.
    float minRadius;
    // Number of spiral arms.
    int numArms;
    // Standard deviation across an arm, in units of GX_ARM_WIDTH * baseRadius.
    float armSigma;
    // Spiral pitch, in tenths of a degree (Cone: aperture, also in tenths of a degree).
    float baseAngle;
    // Ellipsoid semi-axis reduction: x = 1 - eccentricity.x and z = 1 - eccentricity.y.
    // Ignored by the spiral branches, which use `baseAngle` for their shape.
    vec2 eccentricity;
    // Density-wave displacement. No equivalent in the field model: declared no-op.
    vec2 spiralDeltaPos;
    // Bar elongation along x; also the bar's inverse radial compression.
    float aspect;
    // Vertical warp amplitude in the outer disk, in units of baseRadius.
    float warpStrength;
    // Vertical scale height of the slab.
    float heightScale;
    // Height profile id, one of the GX_HEIGHT_* constants.
    int heightProfile;
    // Colour stops, core first, outskirts last.
    vec3 baseColors[4];
    // Amplitude of the spatial colour modulation, in [0,1].
    float colorNoise;
    // Weight of this field in the sum. Not used by galaxyDensity(): the caller applies it when it
    // combines fields (the dataset's intensity, and the per-slot gain).
    float weight;
    // Channel of the field, one of the GX_CHANNEL_* constants. Only dust is treated differently:
    // it gets its own concentration (see gxDustDensity).
    int channel;
    // Amplitude of the azimuthal warp of the arms, in fractions of the arm period. 0 leaves the
    // lanes on their analytic spiral; see gxLaneWarp.
    float laneWarp;
    // Amplitude of the 3D noise mask, in [0,1]. Applies to every field, whatever its channel and
    // its distribution; see gxNoiseMask.
    float noiseMask;
    // Base frequency of the mask, in cycles per baseRadius.
    float noiseMaskFreq;
    // Number of octaves summed into the mask.
    int noiseMaskOctaves;
    // Frequency multiplier of the lane perturbations (warp and rough). 0 or less means 1.
    float laneNoiseScale;
    // Amplitude of the ragged edge of the dust lane, in units of the arm sigma. Only dust uses it;
    // see gxDustRoughness.
    float laneRough;
};

// Channel ids. These must match the BillboardDataset.ChannelType ordinals.
#define GX_CHANNEL_DUST 0
#define GX_CHANNEL_BULGE 1
#define GX_CHANNEL_STAR 2
#define GX_CHANNEL_GAS 3
#define GX_CHANNEL_HII 4
#define GX_CHANNEL_POINT 5

// ---- Packed layout of a GalaxyShape in a float[] uniform ----
//
// A shape travels from Java as a flat float array, because a compute program here has no uniform
// buffer and struct members cannot be set by index. The order below is the order of the struct
// above; GX_FIELD_STRIDE is its length in floats. Mirror any change here in the Java packer
// (GalaxyAttributes.packFields) or every field after the change shifts by one.
//
// GX_MAX_FIELDS is the number of fields summed into ONE slot, and each slot gets its own packed
// array, so a galaxy can carry up to 2 * GX_MAX_FIELDS fields. Fields are baked into the slot's
// texture, so this costs nothing per frame: it only multiplies the voxeliser's work at generation
// time. 8 * 36 = 288 floats is a quarter of the 1024 components a GL 4.3 compute shader is
// guaranteed to get, so the limit is the authoring model, not the hardware.
#define GX_MAX_FIELDS 8
#define GX_FIELD_STRIDE 36
#define GX_FIELD_ARRAY (GX_MAX_FIELDS * GX_FIELD_STRIDE)
#define GX_F_DISTRIBUTION 0
#define GX_F_BASE_RADIUS 1
#define GX_F_MIN_RADIUS 2
#define GX_F_NUM_ARMS 3
#define GX_F_ARM_SIGMA 4
#define GX_F_BASE_ANGLE 5
#define GX_F_ECCENTRICITY_X 6
#define GX_F_ECCENTRICITY_Y 7
#define GX_F_SPIRAL_DELTA_X 8
#define GX_F_SPIRAL_DELTA_Y 9
#define GX_F_ASPECT 10
#define GX_F_WARP_STRENGTH 11
#define GX_F_HEIGHT_SCALE 12
#define GX_F_HEIGHT_PROFILE 13
#define GX_F_COLOR_NOISE 14
#define GX_F_WEIGHT 15
#define GX_F_BASE_COLORS 16 // 4 x vec3
#define GX_F_CHANNEL 28
#define GX_F_LANE_WARP 29
#define GX_F_NOISE_MASK 30
#define GX_F_LANE_NOISE_SCALE 31
#define GX_F_LANE_ROUGH 32
#define GX_F_NOISE_MASK_FREQ 33
#define GX_F_NOISE_MASK_OCTAVES 34

// Rebuilds one shape from `d` starting at float `o`.
GalaxyShape gxUnpack(in float d[GX_FIELD_ARRAY], int o) {
    GalaxyShape s;
    s.distribution = int(d[o + GX_F_DISTRIBUTION] + 0.5);
    s.baseRadius = d[o + GX_F_BASE_RADIUS];
    s.minRadius = d[o + GX_F_MIN_RADIUS];
    s.numArms = int(d[o + GX_F_NUM_ARMS] + 0.5);
    s.armSigma = d[o + GX_F_ARM_SIGMA];
    s.baseAngle = d[o + GX_F_BASE_ANGLE];
    s.eccentricity = vec2(d[o + GX_F_ECCENTRICITY_X], d[o + GX_F_ECCENTRICITY_Y]);
    s.spiralDeltaPos = vec2(d[o + GX_F_SPIRAL_DELTA_X], d[o + GX_F_SPIRAL_DELTA_Y]);
    s.aspect = d[o + GX_F_ASPECT];
    s.warpStrength = d[o + GX_F_WARP_STRENGTH];
    s.heightScale = d[o + GX_F_HEIGHT_SCALE];
    s.heightProfile = int(d[o + GX_F_HEIGHT_PROFILE] + 0.5);
    s.colorNoise = d[o + GX_F_COLOR_NOISE];
    s.weight = d[o + GX_F_WEIGHT];
    s.channel = int(d[o + GX_F_CHANNEL] + 0.5);
    s.laneWarp = d[o + GX_F_LANE_WARP];
    s.noiseMask = d[o + GX_F_NOISE_MASK];
    s.laneNoiseScale = d[o + GX_F_LANE_NOISE_SCALE];
    s.laneRough = d[o + GX_F_LANE_ROUGH];
    s.noiseMaskFreq = d[o + GX_F_NOISE_MASK_FREQ];
    s.noiseMaskOctaves = int(d[o + GX_F_NOISE_MASK_OCTAVES] + 0.5);
    for (int i = 0; i < 4; i++) {
        int b = o + GX_F_BASE_COLORS + 3 * i;
        s.baseColors[i] = vec3(d[b], d[b + 1], d[b + 2]);
    }
    return s;
}

bool gxIsFlat(in GalaxyShape s) {
    return s.distribution == GX_DISK || s.distribution == GX_DISK_GAUSS
    || s.distribution == GX_SPIRAL || s.distribution == GX_SPIRAL_LOG
    || s.distribution == GX_BAR;
}

// Height scale factor with radius, as in the particle generator's computeY().
float gxHeightFactor(in GalaxyShape s, float r) {
    float rn = clamp(r / max(s.baseRadius, 1e-6), 0.0, 1.0);
    if (s.heightProfile == GX_HEIGHT_SMOOTH_INC) {
        return smoothstep(0.0, 1.0, rn);
    } else if (s.heightProfile == GX_HEIGHT_SMOOTH_DEC) {
        return smoothstep(1.0, 0.0, rn);
    } else if (s.heightProfile == GX_HEIGHT_LINEAR_INC) {
        return rn;
    } else if (s.heightProfile == GX_HEIGHT_LINEAR_DEC) {
        return 1.0 - rn;
    }
    return 1.0;
}

// Vertical warp of the disk plane, as in computeY(): zero inside 0.7 radii, growing linearly
// outwards and modulated by the azimuth.
float gxWarp(in GalaxyShape s, float r, float z) {
    if (s.warpStrength == 0.0) {
        return 0.0;
    }
    float rn = r / max(s.baseRadius, 1e-6);
    const float warpStart = 0.7;
    if (rn <= warpStart) {
        return 0.0;
    }
    float intensity = (rn - warpStart) / (1.0 - warpStart);
    float angular = z / max(r, 1e-4);
    return angular * intensity * s.warpStrength;
}

// Log-spiral tightness. `baseAngle` is in tenths of a degree in both spiral distributions, so a
// descriptor keeps its meaning; 85 deg is the limit where the arm becomes tangent.
float gxPitchB(float baseAngle) {
    float deg = clamp(baseAngle * 0.1, 0.0, 85.0);
    return tan(radians(deg));
}

// Angular distance to the nearest arm centreline, wrapped to the arm period. `warp` displaces the
// centreline by a fraction of the period; see gxLaneWarp.
float gxArmDelta(in GalaxyShape s, float r, float phi, float warp) {
    float b = gxPitchB(s.baseAngle);
    float arms = float(max(s.numArms, 1));
    // Arm centreline: theta = b * ln(r / r0), the same relation the particle sampler uses.
    float theta = b * log(max(r / (0.1 * max(s.baseRadius, 1e-6)), 1e-4));
    float period = 6.2831853 / arms;
    return mod(phi - theta - warp * period + 0.5 * period, period) - 0.5 * period;
}

// Value of the arm modulation at a point, in [0,1]: 1 on the arm centreline, 0 far from it. `rough`
// displaces the centreline across itself, in units of the arm sigma, which is what makes a lane
// ragged; 0 leaves the analytic arm.
float gxArmFactor(in GalaxyShape s, float r, float phi, float warp, float rough) {
    float base = max(s.baseRadius, 1e-6);
    float b = gxPitchB(s.baseAngle);
    float d = gxArmDelta(s, r, phi, warp);
    float sigma = max(s.armSigma * GX_ARM_WIDTH * base, 1e-4);
    // Turn the angular distance into a linear one: a log spiral of pitch b has perpendicular
    // spacing r/sqrt(1+b^2) per unit of angle.
    float perpendicular = abs(d) * r / sqrt(1.0 + b * b) + rough * sigma;
    return exp(-0.5 * perpendicular * perpendicular / (sigma * sigma));
}

// Two-octave value noise in [0,1], for the substructure that stops the field from looking
// like a smooth mathematical surface.
float gxNoise3(vec3 q) {
    return 0.65 * noise(q) + 0.35 * noise(q * 2.03 + vec3(17.1, 3.7, 11.3));
}

// Clumping of the disk, in roughly [0.55, 1.45] with a mean of 1. Sampled in cartesian space:
// polar coordinates alias into spokes near the axis, where a pixel spans a large range of
// azimuth.
float gxClumps(vec3 p, float base) {
    return 0.55 + 0.9 * gxNoise3(p * (5.0 / base));
}

// ---- Lane noise ----
//
// Perturbations of the lanes, all off at zero amplitude, all sampled at a frequency relative to
// baseRadius, so none of them depends on the grid:
//
//   laneWarp   bends the arm centreline with a low-frequency phase displacement. An analytic log
//              spiral has no variation along itself, which is what makes the lanes read as drawn
//              arcs.
//   laneRough  displaces the dust lane edge across itself, over four octaves. A single octave is a
//              smooth meander - a wave - and that is what makes a lane read as deformed liquid.
//
// Only the dust uses laneRough: the gas keeps the analytic arms. The 3D noise mask is a separate
// thing - it is not a property of a lane, since a field needs it with or without arms and in any
// channel - and it lives below, next to the density it modifies.

// Frequency of the warp, in cycles per baseRadius.
#define GX_LANE_FREQ 2.4
// Lowest frequency of the ragged edge, in cycles per baseRadius across the lane, and the number of
// octaves summed on top of it.
#define GX_LANE_ROUGH_FREQ 4.0
#define GX_LANE_ROUGH_OCTAVES 4
// Highest frequency the grid can carry, in cycles per voxel. Past this the noise aliases, so
// octaves above it are clamped down to it rather than dropped. Shared by every noise field here.
#define GX_NOISE_MAX_LOD 0.35
// Displacement of the lane edge at laneRough = 1, in units of the arm sigma. The fbm is summed with
// weights 1, 1/2, 1/4..., so its range is well inside [0,1] and this is the peak, not the typical.
#define GX_LANE_ROUGH_SIGMA 3.0
// How much longer a feature is along the lane than across it, and how much flatter it is
// vertically.
#define GX_LANE_STRETCH 2.5
#define GX_LANE_FLATTEN 0.35

// Multiplier of the noise frequency: 0 or less means 1.
float gxLaneScale(in GalaxyShape s) {
    return s.laneNoiseScale > 0.0 ? s.laneNoiseScale : 1.0;
}

// Value-noise fbm in [0,1]: `octaves` octaves, the first at the frequency already folded into `q`.
// `lod` is that first frequency in cycles per voxel, and it is what the grid is checked against:
// an octave that would alias is clamped to the highest the grid can hold, and the octaves after it
// are dropped as duplicates. Clamping rather than fading matters - a fade with a hard cutoff makes
// a coarse grid drop the term entirely, so the parameter silently stops doing anything instead of
// getting smoother. Renormalised by the weights, so the range does not depend on the octave count.
float gxNoiseFbm(vec3 q, float lod, int octaves) {
    float maxMul = lod > 1.0e-9 ? GX_NOISE_MAX_LOD / lod : 1.0e9;
    float sum = 0.0;
    float weight = 0.0;
    float amp = 1.0;
    float mul = 1.0;
    for (int i = 0; i < 8; i++) {
        if (i >= octaves) {
            break;
        }
        sum += amp * noise(q * min(mul, maxMul) + float(i) * vec3(17.1, 9.4, 31.7));
        weight += amp;
        if (mul >= maxMul) {
            break;
        }
        amp *= 0.5;
        mul *= 2.07;
    }
    return sum / weight;
}

// Lane noise in [0,1], sampled in the spiral's own frame: along the arm (the arc length, which for a
// log spiral is linear in r), across it (the same perpendicular distance gxArmFactor uses, wrapped
// at the arm period so that every lane gets its own pattern) and vertically. The along-arm
// coordinate is compressed, and y too, so features come out stretched along the lane.
//
// Warping the cartesian position instead does not work: the along-arm direction rotates with the
// position, and once the lane curves that rotation dominates the displacement and the field comes
// back isotropic.
float gxLaneNoise(in GalaxyShape s, vec3 p, float r, float warp, float freq, int octaves, float cell) {
    float base = max(s.baseRadius, 1e-6);
    float b = gxPitchB(s.baseAngle);
    float q1 = sqrt(1.0 + b * b);
    float along = r * q1;
    float across = gxArmDelta(s, r, atan(p.z, p.x), warp) * r / q1;
    float f = freq * gxLaneScale(s) / base;
    return gxNoiseFbm(vec3(along / GX_LANE_STRETCH, across, p.y * GX_LANE_FLATTEN) * f, f * cell, octaves);
}

// Displacement of the arm phase at this point, in fractions of the arm period. Two octaves of value
// noise: the warp is the coarsest structure in the field, far above the voxel size at any quality,
// so it needs neither the extra octaves nor the grid check.
float gxLaneWarp(in GalaxyShape s, vec3 p) {
    if (s.laneWarp == 0.0) {
        return 0.0;
    }
    float freq = GX_LANE_FREQ * gxLaneScale(s) / max(s.baseRadius, 1e-6);
    return s.laneWarp * (2.0 * gxNoise3(p * freq) - 1.0);
}

// Displacement of the dust lane edge across itself, in units of the arm sigma. Positive and
// negative, over four octaves from 4 cycles per baseRadius up, so the silhouette varies at every
// scale the grid can carry instead of undulating with one.
float gxDustRoughness(in GalaxyShape s, vec3 p, float r, float warp, float cell) {
    if (s.laneRough <= 0.0) {
        return 0.0;
    }
    float n = gxLaneNoise(s, p, r, warp, GX_LANE_ROUGH_FREQ, GX_LANE_ROUGH_OCTAVES, cell);
    return GX_LANE_ROUGH_SIGMA * s.laneRough * (2.0 * n - 1.0);
}

// ---- 3D noise mask ----
//
// A 3D noise field evaluated at the point itself, in [0,1], multiplied into the density of any field
// regardless of its channel: a bulge and a gas disk want clumps as much as a dust lane does. It is
// isotropic and in cartesian space: a clump is a lump of material at some place in the box, so the
// field that carves it has to be a function of that place and of nothing else - no spiral frame, no
// stretching along an arm, and no dependence on the distribution, which is why this is not part of
// the lane noise above.
//
// It reaches zero rather than keeping a floor. Where the field is drawn as extinction (dust) the
// optical depth is tens of times what saturates, so the visible boundary is one level set of the
// density with flat opaque inside it: a multiplier with a floor can thicken or thin a lane but never
// break that level set, which is what makes a smooth field read as deformed liquid. Reaching zero
// opens it into separate clumps. On an emissive field the same remap gives the mottling instead.
float gxNoiseMask(in GalaxyShape s, vec3 p, float cell) {
    if (s.noiseMask <= 0.0) {
        return 1.0;
    }
    // Default frequency and octaves when unset, so the amplitude alone is enough to get a result.
    float freq = (s.noiseMaskFreq > 0.0 ? s.noiseMaskFreq : 10.0) / max(s.baseRadius, 1e-6);
    int octaves = s.noiseMaskOctaves > 0 ? min(s.noiseMaskOctaves, 8) : 3;
    float n = gxNoiseFbm(p * freq, freq * cell, octaves);
    // Remap so the low end is a plateau: the fbm sits mostly near 0.5, and a linear remap would
    // thin the whole field evenly instead of leaving clumps and emptying the space between them.
    float clump = smoothstep(0.32, 0.68, n);
    return clamp(1.0 - s.noiseMask * (1.0 - clump), 0.0, 1.0);
}

// Soft outer rim, so the distribution ends at baseRadius instead of being clipped by it.
float gxTaper(float rn, float start) {
    return 1.0 - smoothstep(start, 1.0, rn);
}

// ---- Dust ----
//
// Dust is not just "gas with smaller numbers": it is a thin ring of narrow lanes inside the arms,
// and it is the only channel whose look is set by its contrast rather than by its brightness. Three
// departures from the gas profile, all of them resolution-independent (they are smooth functions of
// position, so the field never depends on the grid it is sampled on):
//
//   - no inter-arm floor: the 0.22 the gas keeps between arms is exactly the uniform veil dust must
//     not have;
//   - a radial ring, whose inner edge is the field's own minRadius and whose outer one is fixed at
//     the rim of the disk: the lanes start where the field says they start, and minRadius = 0 lets
//     them reach the core;
//   - a contrast power on the product, which thins the lanes without making them sub-voxel: the peak
//     is untouched (it is 1^q) while the tails fall away.
//
// The Milky Way's dust texture measures a mean-to-peak ratio of 3.3e-4 (1.933e-5 of luma against a
// peak of 0.059), and this profile is tuned to land there: 3.5e-4 at NORMAL, 3.3e-4 at ULTRA, so the
// lane contrast is the Milky Way's and not just its integrated absorption. That the ratio holds
// across resolutions is the point of using a smooth function rather than noise: a thresholded noise
// field concentrates harder but its peak is set by which voxel happens to sample the noise, which
// makes both the ratio and the look depend on the grid. Note that those two numbers were measured
// with the ring's inner edge pinned at 0.25; a field with a smaller minRadius puts more dust in the
// texture and raises the ratio, so re-measure before trusting the comparison again.

// Outer edge of the dust ring: the lanes fade out at the rim of the disk.
#define GX_DUST_RING_OUT 0.95
// Width of the radial fade at both edges of the ring.
#define GX_DUST_RING_FADE 0.15
// Contrast power applied to the whole dust field. 1 is the gas profile. 4 measured out of the
// field itself: it lands the mean-to-peak ratio at 3.5e-4, next to the Milky Way's 3.3e-4, and
// leaves the stored peak just under 1.0, so nothing clips - a higher power concentrates further but
// pushes the peak over 1 and the calibrated extinction loses its brightest voxels to the clamp.
#define GX_DUST_CONTRAST 4.0
// Dust is thinner than the gas slab it lives in.
#define GX_DUST_HEIGHT 0.5

// Scale height of the slab: dust sits in a thinner layer than the gas.
float gxHeightScale(in GalaxyShape s) {
    return s.channel == GX_CHANNEL_DUST ? GX_DUST_HEIGHT * s.heightScale : s.heightScale;
}

// Dust density inside the flat branch. `disk` and `arm` are the gas terms, already evaluated.
// `s.minRadius` is the field's own inner radius, normalised by baseRadius like rnCyl: it is where
// the lanes start, and 0 means they reach the core. It used to be a constant (0.25) here, which is
// why setting minRadius on a dust field did nothing beyond moving the arm modulation: the ring cut
// the dust out of the middle regardless.
float gxDustDensity(in GalaxyShape s, float rnCyl, float disk, float arm, vec3 p, float cell) {
    float ringIn = clamp(s.minRadius, 0.0, GX_DUST_RING_OUT - GX_DUST_RING_FADE);
    float ring = smoothstep(ringIn, ringIn + GX_DUST_RING_FADE, rnCyl)
            * (1.0 - smoothstep(GX_DUST_RING_OUT - GX_DUST_RING_FADE, GX_DUST_RING_OUT, rnCyl));
    float d = disk * ring * arm * arm * gxClumps(p, s.baseRadius);
    // Contrast, so the tails fall away without thinning the peak.
    return pow(max(d, 0.0), GX_DUST_CONTRAST);
}

// Radial density profile, normalised radius in, relative density out.
float gxRadial(in GalaxyShape s, float rn) {
    if (s.distribution == GX_SPHERE || s.distribution == GX_ELLIPSOID) {
        // Nearly uniform sphere with a soft rim and a mild core concentration, which is what
        // the billboard sphere looks like once splatted.
        return (1.0 + 1.5 * exp(-rn * rn / 0.0625)) * gxTaper(rn, 0.75);
    } else if (s.distribution == GX_SPHERE_GAUSS || s.distribution == GX_DISK_GAUSS) {
        return exp(-0.5 * rn * rn / 0.1225);
    } else if (s.distribution == GX_DISK) {
        return gxTaper(rn, 0.6);
    } else if (s.distribution == GX_BAR) {
        return exp(-2.0 * rn * rn);
    }
    return gxTaper(rn, 0.75);
}

// Colour as a function of radius only: the core stops first, the outskirts last. This is the
// gradient the billboard sampler could not produce, since its colour was random per particle.
vec3 gxPalette(in GalaxyShape s, float rn) {
    float t = clamp(rn, 0.0, 1.0) * 3.0;
    if (t < 1.0) {
        return mix(s.baseColors[0], s.baseColors[1], t);
    } else if (t < 2.0) {
        return mix(s.baseColors[1], s.baseColors[2], t - 1.0);
    }
    return mix(s.baseColors[2], s.baseColors[3], t - 2.0);
}

// Effective radius, normalised by baseRadius. Spheroids use the ellipsoid metric, the bar
// compresses x, and everything else uses the ordinary distance.
float gxRadius(in GalaxyShape s, vec3 p) {
    float base = max(s.baseRadius, 1e-6);
    if (s.distribution == GX_ELLIPSOID) {
        float a = 1.0;
        float b = max(1.0 - s.eccentricity.y, 0.05);
        float c = max(1.0 - s.eccentricity.x, 0.05);
        return length(vec3(p.x / a, p.y / c, p.z / b)) / base;
    } else if (s.distribution == GX_BAR) {
        return length(vec2(p.x / max(s.aspect, 0.05), p.z)) / base;
    }
    return length(p) / base;
}

// The density field. Returns vec4(colour, density); see the note at the top of the file. `cell` is
// the galaxy-space size of one voxel, used only to fade noise the grid cannot resolve.
vec4 galaxyDensity(vec3 p, in GalaxyShape s, float cell) {
    float base = max(s.baseRadius, 1e-6);
    float rn = gxRadius(s, p);
    if (rn > 1.2) {
        return vec4(0.0);
    }

    float density;
    float r = length(p.xz);
    if (gxIsFlat(s)) {
        // Flat distributions are radially symmetric about y, so the profile follows the
        // cylindrical radius and not the distance to the centre.
        float rnCyl = r / base;
        if (s.distribution == GX_SPIRAL || s.distribution == GX_SPIRAL_LOG) {
            float phi = atan(p.z, p.x);
            float disk = exp(-rnCyl / GX_DISK_SCALE) * gxTaper(rnCyl, 0.8);
            // No azimuthal structure inside the arm start radius: a log spiral's angular
            // frequency grows without bound as r -> 0, which would alias into radial spokes.
            float armWeight = smoothstep(0.0, 2.0 * max(s.minRadius, 0.02), rnCyl);
            float warp = gxLaneWarp(s, p);
            // Only the dust gets the ragged edge: the gas keeps the analytic arms.
            float rough = s.channel == GX_CHANNEL_DUST ? gxDustRoughness(s, p, r, warp, cell) : 0.0;
            float arm = mix(1.0, gxArmFactor(s, r, phi, warp, rough), armWeight);
            if (s.channel == GX_CHANNEL_DUST) {
                density = gxDustDensity(s, rnCyl, disk, arm, p, cell);
            } else {
                density = disk * (0.22 + 0.78 * arm) * gxClumps(p, base);
            }
        } else {
            density = gxRadial(s, rnCyl);
        }

        // Vertical structure: sech^2, which has the exponential tail of a real disk and is
        // cut at GX_VERTICAL_TAIL scale heights where it drops below 1/255.
        float h = max(gxHeightFactor(s, r) * gxHeightScale(s), 1e-5);
        float t = (p.y - gxWarp(s, r, p.z)) / h;
        if (abs(t) > GX_VERTICAL_TAIL) {
            return vec4(0.0);
        }
        float e = exp(-2.0 * abs(t));
        density *= 4.0 * e / ((1.0 + e) * (1.0 + e));
    } else if (s.distribution == GX_CONE) {
        // Truncated cone along +y, apex at -0.5 radii, as in the particle sampler.
        float cosA = cos(radians(clamp(s.baseAngle * 0.1, 1.0, 60.0)));
        vec3 d = p - vec3(0.0, -0.5 * base, 0.0);
        if (normalize(d).y < cosA) {
            return vec4(0.0);
        }
        density = gxTaper(rn, 0.6);
    } else {
        density = gxRadial(s, rn);
        if (s.distribution == GX_IRREGULAR) {
            density *= 0.4 + 1.2 * gxNoise3(p * (4.0 / base));
        }
    }

    if (density <= 0.0) {
        return vec4(0.0);
    }

    // The mask is a property of the volume, not of the channel or of the distribution, and it goes
    // after the vertical profile so a clump scales the whole column rather than one slice of it.
    density *= gxNoiseMask(s, p, cell);

    // Colour: radius gradient, modulated in luminance so the hue survives the noise.
    vec3 color = gxPalette(s, clamp(rn, 0.0, 1.0));
    float modulation = 1.0 + s.colorNoise * (2.0 * gxNoise3(p * (8.0 / base)) - 1.0);
    color = clamp(color * modulation, 0.0, 1.0);

    return vec4(color, density);
}

// Half-extent of the box that contains the whole field, in galaxy space, laterally. Covers the
// arm spread beyond baseRadius - only flat distributions have arms, so only they pay for it.
float gxLateralHalfExtent(in GalaxyShape s) {
    float spread = gxIsFlat(s) ? clamp(GX_VERTICAL_TAIL * s.armSigma * GX_ARM_WIDTH, 0.0, 0.2) : 0.0;
    return s.baseRadius * (1.0 + spread);
}

// Half-extent of the box vertically. Spheroids fill the lateral box, flat distributions only
// reach the top of their slab plus the warp.
float gxVerticalHalfExtent(in GalaxyShape s) {
    if (!gxIsFlat(s)) {
        return gxLateralHalfExtent(s);
    }
    return GX_VERTICAL_TAIL * gxHeightScale(s) + abs(s.warpStrength);
}

// Vertical to lateral aspect ratio of the containing box. The volume grid is sized from this,
// so that a flat galaxy gets a thin texture and a spheroid a cubic one.
float gxBoxAspect(in GalaxyShape s) {
    return gxVerticalHalfExtent(s) / max(gxLateralHalfExtent(s), 1e-6);
}

// Luma weights of the offline pipeline (particles-to-volume.py, LUMA), which is what its
// "mean ray integral" is averaged with. Kept the same so the two numbers can be compared.
const vec3 GX_LUMA = vec3(0.2126, 0.7152, 0.0722);

// Several fields summed at one point, which is what one volume slot holds: gas+bulge in the
// emissive slot, dust in the occlusive one.
struct GalaxyFields {
    // Sum of intensity * density * colour. This is the emitted light as a vector, exactly as the
    // offline pipeline merges channels: colour and brightness are its direction and its length.
    vec3 light;
    // The same at unit intensity, which is what the "merged reference" in the volume metadata is
    // the peak of: a property of the shape, unaffected by the intensities.
    vec3 unitLight;
    // Sum of intensity * density, with no colour in it. This is what an occluding field stores:
    // optical depth is a scalar, and taking it from `light` instead would let two fields of
    // different colours partially cancel in the alpha channel.
    float density;
    // The same at unit intensity, which is the reference the occlusive slot is normalised by.
    float unitDensity;
};

GalaxyFields gxSampleFields(vec3 p, in float d[GX_FIELD_ARRAY], int count, float cell) {
    GalaxyFields r;
    r.light = vec3(0.0);
    r.unitLight = vec3(0.0);
    r.density = 0.0;
    r.unitDensity = 0.0;
    for (int i = 0; i < GX_MAX_FIELDS; i++) {
        if (i >= count) {
            break;
        }
        GalaxyShape s = gxUnpack(d, i * GX_FIELD_STRIDE);
        vec4 v = galaxyDensity(p, s, cell);
        vec3 emission = v.rgb * v.a;
        r.light += s.weight * emission;
        r.unitLight += emission;
        r.density += s.weight * v.a;
        r.unitDensity += v.a;
    }
    return r;
}

// Brightness of a light vector: its largest channel. Storing that in .a and the vector divided by
// it in .rgb reconstructs the light exactly, whatever the hue, in 8 + 8 + 8 bits.
float gxBrightness(vec3 light) {
    return max(max(light.r, light.g), light.b);
}

// Perceived brightness, i.e. the quantity the mean ray integral of the pipeline averages.
float gxLuma(vec3 light) {
    return dot(light, GX_LUMA);
}

#endif
