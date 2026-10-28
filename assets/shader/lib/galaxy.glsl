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
// so the field is resolution-independent and changing a parameter never needs a reseed.

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
};

// ---- Packed layout of a GalaxyShape in a float[] uniform ----
//
// A shape travels from Java as a flat float array, because a compute program here has no uniform
// buffer and struct members cannot be set by index. The order below is the order of the struct
// above; GX_FIELD_STRIDE is its length in floats. Mirror any change here in the Java packer
// (BillboardDataset.packFields) or every field after the change shifts by one.
#define GX_MAX_FIELDS 3
#define GX_FIELD_STRIDE 28
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

// Value of the arm modulation at a point, in [0,1]: 1 on the arm centreline, 0 far from it.
float gxArmFactor(in GalaxyShape s, float r, float phi) {
    float base = max(s.baseRadius, 1e-6);
    float b = gxPitchB(s.baseAngle);
    float arms = float(max(s.numArms, 1));
    // Arm centreline: theta = b * ln(r / r0), the same relation the particle sampler uses.
    float theta = b * log(max(r / (0.1 * base), 1e-4));
    // Angular distance to the nearest arm, wrapped to the arm period.
    float period = 6.2831853 / arms;
    float d = mod(phi - theta + 0.5 * period, period) - 0.5 * period;
    // Turn the angular distance into a linear one: a log spiral of pitch b has perpendicular
    // spacing r/sqrt(1+b^2) per unit of angle.
    float perpendicular = abs(d) * r / sqrt(1.0 + b * b);
    float sigma = max(s.armSigma * GX_ARM_WIDTH * base, 1e-4);
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

// Soft outer rim, so the distribution ends at baseRadius instead of being clipped by it.
float gxTaper(float rn, float start) {
    return 1.0 - smoothstep(start, 1.0, rn);
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

// The density field. Returns vec4(colour, density); see the note at the top of the file.
vec4 galaxyDensity(vec3 p, in GalaxyShape s) {
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
            float arm = mix(1.0, gxArmFactor(s, r, phi), armWeight);
            density = disk * (0.22 + 0.78 * arm) * gxClumps(p, base);
        } else {
            density = gxRadial(s, rnCyl);
        }

        // Vertical structure: sech^2, which has the exponential tail of a real disk and is
        // cut at GX_VERTICAL_TAIL scale heights where it drops below 1/255.
        float h = max(gxHeightFactor(s, r) * s.heightScale, 1e-5);
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
    return GX_VERTICAL_TAIL * s.heightScale + abs(s.warpStrength);
}

// Vertical to lateral aspect ratio of the containing box. The volume grid is sized from this,
// so that a flat galaxy gets a thin texture and a spheroid a cubic one.
float gxBoxAspect(in GalaxyShape s) {
    return gxVerticalHalfExtent(s) / max(gxLateralHalfExtent(s), 1e-6);
}

// Several fields summed at one point, which is what one volume slot holds: gas+bulge in the
// emissive slot, dust in the occlusive one.
struct GalaxyFieldSample {
    // Brightness-weighted colour mix, normalised so its brightest component is 1. The volume
    // raymarcher wants chromaticity in .rgb and magnitude in .a, so the hue lives here and the
    // brightness in `weighted`; scaling one never disturbs the other.
    vec3 chroma;
    // Sum of weight * density: the brightness at the fields' actual intensities.
    float weighted;
    // Sum of density: the brightness at unit intensity, which is the "merged reference" the
    // volume metadata divides by.
    float unweighted;
};

GalaxyFieldSample gxSampleFields(vec3 p, in float d[GX_FIELD_ARRAY], int count) {
    GalaxyFieldSample r;
    vec3 colSum = vec3(0.0);
    r.weighted = 0.0;
    r.unweighted = 0.0;
    r.chroma = vec3(0.0);
    for (int i = 0; i < GX_MAX_FIELDS; i++) {
        if (i >= count) {
            break;
        }
        GalaxyShape s = gxUnpack(d, i * GX_FIELD_STRIDE);
        vec4 v = galaxyDensity(p, s);
        float w = s.weight * v.a;
        r.weighted += w;
        r.unweighted += v.a;
        colSum += w * v.rgb;
    }
    if (r.weighted > 0.0) {
        r.chroma = colSum / r.weighted;
        r.chroma /= max(max(r.chroma.r, max(r.chroma.g, r.chroma.b)), 1e-6);
    }
    return r;
}

#endif
