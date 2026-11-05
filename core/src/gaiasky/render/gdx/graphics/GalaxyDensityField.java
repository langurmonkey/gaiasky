/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import java.util.Arrays;

/**
 * Java port of the analytic galaxy density field of {@code assets/shader/lib/galaxy.glsl}.
 * <p>
 * This exists so that a volume can be voxelised without a compute shader. It is a port, not a second
 * model: every function here has the same name as its GLSL counterpart and the same body, so the two
 * can be read side by side, and any change to the field belongs in both. What it is <em>not</em> is
 * bit-identical, and cannot be:
 * <ul>
 *     <li>The noise lattice values come from an integer hash rather than the shader's
 *     {@code fract(sin(dot(...)) * 43758.5453)}. That sin-hash is evaluated in float32 on arguments
 *     of order thousands, where the precision is gone, so its per-GPU result is not reproducible
 *     anywhere - not even on two GPUs. A hash gives values from the same uniform distribution at the
 *     same lattice points with the same interpolation, so the noise has the same character and the
 *     same spectrum; what it does not have is the same realisation. The same is true of the existing
 *     {@code CPUGalGenFallback} for billboards, and it is the accepted behaviour.</li>
 *     <li>The statistics are accumulated in double here and in fixed point there.</li>
 * </ul>
 * Because the calibration normalises by quantities measured on the same field (see
 * {@link VolumeCalibration}), a different realisation of the noise changes the fine structure and
 * not the brightness: the mean ray integral of an emissive slot and the stored peak of an occlusive
 * one land on their targets either way.
 * <p>
 * The <em>structure</em> is identical - distributions, arms, warp, height profiles, dust rings,
 * lane perturbations, the noise mask, the colour ramp - because that is the part that is a function
 * of the shape and not of the random draw behind the lattice.
 * <p>
 * Not thread safe: the sampling methods take their scratch as an argument ({@link Sample},
 * {@link FieldSum}) so that a single instance of each can be reused for millions of voxels without
 * allocating, and the caller must not share them across threads.
 */
public final class GalaxyDensityField {
    /** Width of the arm, in units of baseRadius. Mirrors {@code GX_ARM_WIDTH}. **/
    public static final float ARM_WIDTH = 0.2f;
    /** Scale radius of the exponential disk, in units of baseRadius. Mirrors {@code GX_DISK_SCALE}. **/
    public static final float DISK_SCALE = 0.33f;
    /** Vertical slab is cut at this many scale heights. Mirrors {@code GX_VERTICAL_TAIL}. **/
    public static final float VERTICAL_TAIL = 3.0f;
    /** Frequency of the lane warp, in cycles per baseRadius. Mirrors {@code GX_LANE_FREQ}. **/
    public static final float LANE_FREQ = 2.4f;
    /** Lowest frequency of the ragged dust edge. Mirrors {@code GX_LANE_ROUGH_FREQ}. **/
    public static final float LANE_ROUGH_FREQ = 4.0f;
    /** Octaves summed into the ragged edge. Mirrors {@code GX_LANE_ROUGH_OCTAVES}. **/
    public static final int LANE_ROUGH_OCTAVES = 4;
    /** Highest frequency the grid can carry smoothly, in cycles per voxel. Mirrors {@code GX_NOISE_MAX_LOD}. **/
    public static final float NOISE_MAX_LOD = 0.15f;
    /** Displacement of the lane edge at laneRough = 1. Mirrors {@code GX_LANE_ROUGH_SIGMA}. **/
    public static final float LANE_ROUGH_SIGMA = 3.0f;
    /** Feature stretch along the lane. Mirrors {@code GX_LANE_STRETCH}. **/
    public static final float LANE_STRETCH = 2.5f;
    /** Vertical flattening of the lane noise. Mirrors {@code GX_LANE_FLATTEN}. **/
    public static final float LANE_FLATTEN = 0.35f;
    /** Outer edge of the dust ring. Mirrors {@code GX_DUST_RING_OUT}. **/
    public static final float DUST_RING_OUT = 0.95f;
    /** Width of the radial fade at both edges of the dust ring. Mirrors {@code GX_DUST_RING_FADE}. **/
    public static final float DUST_RING_FADE = 0.15f;
    /** Contrast power of the dust field. Mirrors {@code GX_DUST_CONTRAST}. **/
    public static final float DUST_CONTRAST = 4.0f;
    /** Dust slab thickness relative to the gas one. Mirrors {@code GX_DUST_HEIGHT}. **/
    public static final float DUST_HEIGHT = 0.5f;
    /** Scale of the bulge's exponential envelope. Mirrors {@code GX_BULGE_SCALE}. **/
    public static final float BULGE_SCALE = 0.32f;
    /**
     * Radius, in units of baseRadius, inside which the azimuthal structure fades out. Mirrors
     * {@code GX_ARM_START}: an anti-aliasing term, not a physical inner edge.
     **/
    public static final float ARM_START = 0.12f;
    /** Luma weights of the offline pipeline. Mirrors {@code GX_LUMA}. **/
    public static final float LUMA_R = 0.2126f, LUMA_G = 0.7152f, LUMA_B = 0.0722f;

    private static final float TWO_PI = 6.2831853f;

    private GalaxyDensityField() {
    }

    /**
     * One field evaluated at one point: the chromaticity and the density, in source units.
     * <p>
     * Reused across voxels; {@link #set} resets every member, so a caller that forgets to reset
     * gets the previous voxel's colour on an empty one. Read it immediately.
     */
    public static final class Sample {
        public float r, g, b, density;

        void set(float r, float g, float b, float density) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.density = density;
        }

        void clear() {
            set(0f, 0f, 0f, 0f);
        }
    }

    /**
     * Several fields summed at one point, which is what one volume slot holds: the emitted light and
     * the summed density, at the actual intensities and at unit intensity.
     */
    public static final class FieldSum {
        public float lightR, lightG, lightB;
        public float unitR, unitG, unitB;
        public float density;
        public float unitDensity;
        /** Per field: {@code weight * density}. An occlusive slot normalises each on its own peak. **/
        public final float[] fieldDensity = new float[VolumeLayout.MAX_FIELDS];
        /** Per field: the density at unit intensity. **/
        public final float[] fieldUnitDensity = new float[VolumeLayout.MAX_FIELDS];

        void clear() {
            lightR = lightG = lightB = 0f;
            unitR = unitG = unitB = 0f;
            density = unitDensity = 0f;
            Arrays.fill(fieldDensity, 0f);
            Arrays.fill(fieldUnitDensity, 0f);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Noise.
    // ---------------------------------------------------------------------------------------------

    /**
     * Value noise in [0,1], trilinear with a smoothstep fade.
     * <p>
     * Same construction as {@code noise(vec3)} in {@code distributions.glsl} - eight corner values on
     * the integer lattice, blended with {@code f*f*(3-2f)} - but with the corner values from an
     * integer hash instead of the shader's sin-hash; see the class doc for why.
     *
     * @param x First coordinate.
     * @param y Second coordinate.
     * @param z Third coordinate.
     *
     * @return Noise value at that point, in [0,1].
     */
    public static float noise(float x,
                              float y,
                              float z) {
        float fx = (float) Math.floor(x);
        float fy = (float) Math.floor(y);
        float fz = (float) Math.floor(z);
        int ix = (int) fx;
        int iy = (int) fy;
        int iz = (int) fz;
        float ux = x - fx;
        float uy = y - fy;
        float uz = z - fz;
        ux = ux * ux * (3f - 2f * ux);
        uy = uy * uy * (3f - 2f * uy);
        uz = uz * uz * (3f - 2f * uz);

        float a = hash(ix, iy, iz);
        float b = hash(ix + 1, iy, iz);
        float c = hash(ix, iy + 1, iz);
        float d = hash(ix + 1, iy + 1, iz);
        float e = hash(ix, iy, iz + 1);
        float f = hash(ix + 1, iy, iz + 1);
        float g = hash(ix, iy + 1, iz + 1);
        float h = hash(ix + 1, iy + 1, iz + 1);

        return mix(mix(mix(a, b, ux), mix(c, d, ux), uy),
                   mix(mix(e, f, ux), mix(g, h, ux), uy), uz);
    }

    /** Uniform value in [0,1) for an integer lattice point. **/
    private static float hash(int x,
                              int y,
                              int z) {
        int h = x * 0x8DA6B343 + y * 0xD8163841 + z * 0xCB1AB31F;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h >>> 8) * (1.0f / 16777216.0f);
    }

    /** Two-octave value noise in [0,1], the substructure of the field. Mirrors {@code gxNoise3}. **/
    public static float noise3(float x,
                               float y,
                               float z) {
        return 0.65f * noise(x, y, z)
                + 0.35f * noise(x * 2.03f + 17.1f, y * 2.03f + 3.7f, z * 2.03f + 11.3f);
    }

    /**
     * Value-noise fbm in [0,1]. Mirrors {@code gxNoiseFbm}: octaves that would alias are clamped to
     * the highest frequency the grid can carry, and the result is renormalised by the weights.
     */
    static float noiseFbm(float qx,
                          float qy,
                          float qz,
                          float lod,
                          int octaves) {
        float maxMul = lod > 1.0e-9f ? NOISE_MAX_LOD / lod : 1.0e9f;
        float sum = 0f;
        float weight = 0f;
        float amp = 1f;
        float mul = 1f;
        for (int i = 0; i < 8; i++) {
            if (i >= octaves) {
                break;
            }
            float m = Math.min(mul, maxMul);
            sum += amp * noise(qx * m + i * 17.1f, qy * m + i * 9.4f, qz * m + i * 31.7f);
            weight += amp;
            if (mul >= maxMul) {
                break;
            }
            amp *= 0.5f;
            mul *= 2.07f;
        }
        return weight > 0f ? sum / weight : 0f;
    }

    // ---------------------------------------------------------------------------------------------
    // Shape.
    // ---------------------------------------------------------------------------------------------

    /** @return Whether the field is flat (a disk, a spiral or a bar). Mirrors {@code gxIsFlat}. **/
    public static boolean isFlat(GalaxyShape s) {
        return s.distribution == GalaxyShape.DISK || s.distribution == GalaxyShape.DISK_GAUSS
                || s.distribution == GalaxyShape.SPIRAL || s.distribution == GalaxyShape.SPIRAL_LOG
                || s.distribution == GalaxyShape.BAR;
    }

    /** Height scale factor with radius. Mirrors {@code gxHeightFactor}. **/
    static float heightFactor(GalaxyShape s,
                              float r) {
        float rn = clamp(r / Math.max(s.baseRadius, 1e-6f), 0f, 1f);
        return switch (s.heightProfile) {
            case GalaxyShape.HEIGHT_SMOOTH_INC -> smoothstep(0f, 1f, rn);
            case GalaxyShape.HEIGHT_SMOOTH_DEC -> smoothstep(1f, 0f, rn);
            case GalaxyShape.HEIGHT_LINEAR_INC -> rn;
            case GalaxyShape.HEIGHT_LINEAR_DEC -> 1f - rn;
            default -> 1f;
        };
    }

    /** Vertical warp of the disk plane. Mirrors {@code gxWarp}. **/
    static float warp(GalaxyShape s,
                      float r,
                      float z) {
        if (s.warpStrength == 0f) {
            return 0f;
        }
        float rn = r / Math.max(s.baseRadius, 1e-6f);
        final float warpStart = 0.7f;
        if (rn <= warpStart) {
            return 0f;
        }
        float intensity = (rn - warpStart) / (1f - warpStart);
        float angular = z / Math.max(r, 1e-4f);
        return angular * intensity * s.warpStrength;
    }

    /** Log-spiral tightness: {@code baseAngle} is in tenths of a degree. Mirrors {@code gxPitchB}. **/
    static float pitchB(float baseAngle) {
        float deg = clamp(baseAngle * 0.1f, 0f, 85f);
        return (float) Math.tan(Math.toRadians(deg));
    }

    /** Angular distance to the nearest arm centreline. Mirrors {@code gxArmDelta}. **/
    static float armDelta(GalaxyShape s,
                          float r,
                          float phi,
                          float warp) {
        float b = pitchB(s.baseAngle);
        float arms = Math.max(s.numArms, 1);
        float theta = b * (float) Math.log(Math.max(r / (0.1f * Math.max(s.baseRadius, 1e-6f)), 1e-4f));
        float period = TWO_PI / arms;
        return mod(phi - theta - warp * period + 0.5f * period, period) - 0.5f * period;
    }

    /** Value of the arm modulation at a point, in [0,1]. Mirrors {@code gxArmFactor}. **/
    static float armFactor(GalaxyShape s,
                           float r,
                           float phi,
                           float warp,
                           float rough) {
        float base = Math.max(s.baseRadius, 1e-6f);
        float b = pitchB(s.baseAngle);
        float d = armDelta(s, r, phi, warp);
        float sigma = Math.max(s.armSigma * ARM_WIDTH * base, 1e-4f);
        float perpendicular = Math.abs(d) * r / (float) Math.sqrt(1.0 + b * b) + rough * sigma;
        return (float) Math.exp(-0.5f * perpendicular * perpendicular / (sigma * sigma));
    }

    /** Clumping of the disk, roughly [0.55, 1.45] with a mean of 1. Mirrors {@code gxClumps}. **/
    static float clumps(float px,
                        float py,
                        float pz,
                        float base) {
        float f = 5.0f / base;
        return 0.55f + 0.9f * noise3(px * f, py * f, pz * f);
    }

    /** Multiplier of the lane noise frequency: 0 or less means 1. Mirrors {@code gxLaneScale}. **/
    static float laneScale(GalaxyShape s) {
        return s.laneNoiseScale > 0f ? s.laneNoiseScale : 1f;
    }

    /** Lane noise in [0,1], sampled in the spiral's own frame. Mirrors {@code gxLaneNoise}. **/
    static float laneNoise(GalaxyShape s,
                           float px,
                           float py,
                           float pz,
                           float r,
                           float warp,
                           float freq,
                           int octaves,
                           float cell) {
        float base = Math.max(s.baseRadius, 1e-6f);
        float b = pitchB(s.baseAngle);
        float q1 = (float) Math.sqrt(1.0 + b * b);
        float along = r * q1;
        float across = armDelta(s, r, (float) Math.atan2(pz, px), warp) * r / q1;
        float f = freq * laneScale(s) / base;
        return noiseFbm(along / LANE_STRETCH * f, across * f, py * LANE_FLATTEN * f, f * cell, octaves);
    }

    /** Displacement of the arm phase, in fractions of the arm period. Mirrors {@code gxLaneWarp}. **/
    static float laneWarp(GalaxyShape s,
                          float px,
                          float py,
                          float pz) {
        if (s.laneWarp == 0f) {
            return 0f;
        }
        float freq = LANE_FREQ * laneScale(s) / Math.max(s.baseRadius, 1e-6f);
        return s.laneWarp * (2f * noise3(px * freq, py * freq, pz * freq) - 1f);
    }

    /** Displacement of the dust lane edge, in units of the arm sigma. Mirrors {@code gxDustRoughness}. **/
    static float dustRoughness(GalaxyShape s,
                               float px,
                               float py,
                               float pz,
                               float r,
                               float warp,
                               float cell) {
        if (s.laneRough <= 0f) {
            return 0f;
        }
        float n = laneNoise(s, px, py, pz, r, warp, LANE_ROUGH_FREQ, LANE_ROUGH_OCTAVES, cell);
        return LANE_ROUGH_SIGMA * s.laneRough * (2f * n - 1f);
    }

    /** 3D noise mask in [0,1], multiplied into the density of any field. Mirrors {@code gxNoiseMask}. **/
    static float noiseMask(GalaxyShape s,
                           float px,
                           float py,
                           float pz,
                           float cell) {
        if (s.noiseMask <= 0f) {
            return 1f;
        }
        float freq = (s.noiseMaskFreq > 0f ? s.noiseMaskFreq : 10f) / Math.max(s.baseRadius, 1e-6f);
        int octaves = s.noiseMaskOctaves > 0 ? Math.min(s.noiseMaskOctaves, 8) : 3;
        float n = noiseFbm(px * freq, py * freq, pz * freq, freq * cell, octaves);
        // Wide band; see gxNoiseMask for why it is not [0.32, 0.68].
        float clump = smoothstep(0.18f, 0.82f, n);
        return clamp(1f - s.noiseMask * (1f - clump), 0f, 1f);
    }

    /** Soft outer rim, so the distribution ends at baseRadius instead of being clipped by it. **/
    static float taper(float rn,
                       float start) {
        return 1f - smoothstep(start, 1f, rn);
    }

    /** Dust density inside the flat branch. Mirrors {@code gxDustDensity}. **/
    static float dustDensity(GalaxyShape s,
                             float rnCyl,
                             float disk,
                             float arm,
                             float px,
                             float py,
                             float pz) {
        float ringIn = Math.min(minRn(s), DUST_RING_OUT - DUST_RING_FADE);
        float ring = smoothstep(ringIn, ringIn + DUST_RING_FADE, rnCyl)
                * (1f - smoothstep(DUST_RING_OUT - DUST_RING_FADE, DUST_RING_OUT, rnCyl));
        float d = disk * ring * arm * arm * clumps(px, py, pz, s.baseRadius);
        return (float) Math.pow(Math.max(d, 0f), DUST_CONTRAST);
    }

    /** Radial density profile, normalised radius in. Mirrors {@code gxRadial}. **/
    static float radial(GalaxyShape s,
                        float rn) {
        if (s.distribution == GalaxyShape.SPHERE) {
            // The bulge: an exponential envelope. Mirrors GX_BULGE_SCALE.
            return (float) Math.exp(-rn / BULGE_SCALE) * taper(rn, 0.85f);
        } else if (s.distribution == GalaxyShape.ELLIPSOID) {
            return (1f + 1.5f * (float) Math.exp(-rn * rn / 0.0625f)) * taper(rn, 0.75f);
        } else if (s.distribution == GalaxyShape.SPHERE_GAUSS || s.distribution == GalaxyShape.DISK_GAUSS) {
            return (float) Math.exp(-0.5f * rn * rn / 0.1225f);
        } else if (s.distribution == GalaxyShape.DISK) {
            return taper(rn, 0.6f);
        } else if (s.distribution == GalaxyShape.BAR) {
            return (float) Math.exp(-2f * rn * rn);
        }
        return taper(rn, 0.75f);
    }

    /** Colour as a function of radius only. Mirrors {@code gxPalette}. **/
    static void palette(GalaxyShape s,
                        float rn,
                        Sample out) {
        float t = clamp(rn, 0f, 1f) * 3f;
        float[] c = s.baseColors;
        if (t < 1f) {
            out.r = mix(c[0], c[3], t);
            out.g = mix(c[1], c[4], t);
            out.b = mix(c[2], c[5], t);
        } else if (t < 2f) {
            out.r = mix(c[3], c[6], t - 1f);
            out.g = mix(c[4], c[7], t - 1f);
            out.b = mix(c[5], c[8], t - 1f);
        } else {
            out.r = mix(c[6], c[9], t - 2f);
            out.g = mix(c[7], c[10], t - 2f);
            out.b = mix(c[8], c[11], t - 2f);
        }
    }

    /** Inner radius, normalised by baseRadius like {@code rn}. Mirrors {@code gxMinRn}. **/
    static float minRn(GalaxyShape s) {
        return Math.max(0f, Math.min(s.minRadius / Math.max(s.baseRadius, 1e-6f), 0.98f));
    }

    /** Effective radius, normalised by baseRadius. Mirrors {@code gxRadius}. **/
    static float radius(GalaxyShape s,
                        float px,
                        float py,
                        float pz) {
        float base = Math.max(s.baseRadius, 1e-6f);
        if (s.distribution == GalaxyShape.ELLIPSOID) {
            float c = Math.max(1f - s.eccentricityX, 0.05f);
            float b = Math.max(1f - s.eccentricityY, 0.05f);
            return (float) Math.sqrt(px * px + (py / c) * (py / c) + (pz / b) * (pz / b)) / base;
        } else if (s.distribution == GalaxyShape.BAR) {
            float ax = px / Math.max(s.aspect, 0.05f);
            return (float) Math.sqrt(ax * ax + pz * pz) / base;
        }
        return (float) Math.sqrt(px * px + py * py + pz * pz) / base;
    }

    /**
     * The density field. Mirrors {@code galaxyDensity}: fills {@code out} with the chromaticity in
     * {@code r, g, b} and the density in {@code density}, which is zero outside the field.
     *
     * @param s    The field.
     * @param px   First coordinate, in galaxy space.
     * @param py   Second coordinate, in galaxy space.
     * @param pz   Third coordinate, in galaxy space.
     * @param cell Galaxy-space size of one voxel, used only to fade noise the grid cannot resolve.
     * @param out  Scratch to write into.
     */
    public static void sample(GalaxyShape s,
                              float px,
                              float py,
                              float pz,
                              float cell,
                              Sample out) {
        out.clear();
        float base = Math.max(s.baseRadius, 1e-6f);
        float rn = radius(s, px, py, pz);
        if (rn > 1.2f) {
            return;
        }
        // Inner edge, as in the shader: minRadius is an absolute fraction of the object radius.
        if (rn < minRn(s)) {
            return;
        }

        float density;
        float r = (float) Math.sqrt(px * px + pz * pz);
        if (isFlat(s)) {
            float rnCyl = r / base;
            if (s.distribution == GalaxyShape.SPIRAL || s.distribution == GalaxyShape.SPIRAL_LOG) {
                float phi = (float) Math.atan2(pz, px);
                float disk = (float) Math.exp(-rnCyl / DISK_SCALE) * taper(rnCyl, 0.8f);
                // No azimuthal structure inside the arm start radius: a log spiral's angular
                // frequency grows without bound as r -> 0, which would alias into radial spokes.
                float armWeight = smoothstep(0f, 2f * Math.max(minRn(s), ARM_START), rnCyl);
                float warp = laneWarp(s, px, py, pz);
                // Only the dust gets the ragged edge: the gas keeps the analytic arms.
                float rough = s.isDust() ? dustRoughness(s, px, py, pz, r, warp, cell) : 0f;
                float arm = mix(1f, armFactor(s, r, phi, warp, rough), armWeight);
                if (s.isDust()) {
                    density = dustDensity(s, rnCyl, disk, arm, px, py, pz);
                } else {
                    density = disk * (0.22f + 0.78f * arm) * clumps(px, py, pz, base);
                }
            } else {
                density = radial(s, rnCyl);
            }

            // Vertical structure: sech^2, cut at GX_VERTICAL_TAIL scale heights where it is below
            // 1/255.
            float h = Math.max(heightFactor(s, r) * s.effectiveHeightScale(), 1e-5f);
            float t = (py - warp(s, r, pz)) / h;
            if (Math.abs(t) > VERTICAL_TAIL) {
                return;
            }
            float e = (float) Math.exp(-2f * Math.abs(t));
            density *= 4f * e / ((1f + e) * (1f + e));
        } else if (s.distribution == GalaxyShape.CONE) {
            // Truncated cone along +y, apex at -0.5 radii.
            float cosA = (float) Math.cos(Math.toRadians(clamp(s.baseAngle * 0.1f, 1f, 60f)));
            float dx = px;
            float dy = py + 0.5f * base;
            float dz = pz;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len <= 1e-12f || dy / len < cosA) {
                return;
            }
            density = taper(rn, 0.6f);
        } else {
            density = radial(s, rn);
            if (s.distribution == GalaxyShape.IRREGULAR) {
                float f = 4.0f / base;
                density *= 0.4f + 1.2f * noise3(px * f, py * f, pz * f);
            }
        }

        if (density <= 0f) {
            return;
        }

        // The mask goes after the vertical profile, so a clump scales the whole column.
        density *= noiseMask(s, px, py, pz, cell);

        // Colour: radius gradient, modulated in luminance so the hue survives the noise.
        palette(s, clamp(rn, 0f, 1f), out);
        float f = 8.0f / base;
        float modulation = 1f + s.colorNoise * (2f * noise3(px * f, py * f, pz * f) - 1f);
        out.r = clamp(out.r * modulation, 0f, 1f);
        out.g = clamp(out.g * modulation, 0f, 1f);
        out.b = clamp(out.b * modulation, 0f, 1f);
        out.density = density;
    }

    /**
     * Samples every field of the slot at one point, which is what the texture stores. Mirrors
     * {@code gxSampleFields}.
     *
     * @param shapes The fields, unpacked.
     * @param count  How many of them to sum.
     * @param px     First coordinate.
     * @param py     Second coordinate.
     * @param pz     Third coordinate.
     * @param cell   Voxel size, for the noise LOD clamp.
     * @param sum    Scratch to write into.
     * @param one    Scratch for a single field.
     */
    public static void sampleFields(GalaxyShape[] shapes,
                                    int count,
                                    float px,
                                    float py,
                                    float pz,
                                    float cell,
                                    FieldSum sum,
                                    Sample one) {
        sum.clear();
        for (int i = 0; i < count; i++) {
            var s = shapes[i];
            sample(s, px, py, pz, cell, one);
            float weight = s.weight;
            float er = one.r * one.density;
            float eg = one.g * one.density;
            float eb = one.b * one.density;
            sum.lightR += weight * er;
            sum.lightG += weight * eg;
            sum.lightB += weight * eb;
            sum.unitR += er;
            sum.unitG += eg;
            sum.unitB += eb;
            sum.density += weight * one.density;
            sum.unitDensity += one.density;
            sum.fieldDensity[i] = weight * one.density;
            sum.fieldUnitDensity[i] = one.density;
        }
    }

    /** @return Brightness of a light vector: its largest channel. Mirrors {@code gxBrightness}. **/
    public static float brightness(float r,
                                   float g,
                                   float b) {
        return Math.max(Math.max(r, g), b);
    }

    /** @return Perceived brightness, the quantity the mean ray integral averages. Mirrors {@code gxLuma}. **/
    public static float luma(float r,
                             float g,
                             float b) {
        return r * LUMA_R + g * LUMA_G + b * LUMA_B;
    }

    // ---------------------------------------------------------------------------------------------
    // GLSL built-ins, so that the bodies above read like the shader's.
    // ---------------------------------------------------------------------------------------------

    static float mix(float a,
                     float b,
                     float t) {
        return a + (b - a) * t;
    }

    static float clamp(float v,
                       float min,
                       float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** GLSL {@code smoothstep}: 0 below {@code e0}, 1 above {@code e1}, cubic in between. **/
    static float smoothstep(float e0,
                            float e1,
                            float x) {
        if (e0 == e1) {
            return x < e0 ? 0f : 1f;
        }
        float t = clamp((x - e0) / (e1 - e0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    /** GLSL {@code mod}: always the same sign as the divisor. **/
    static float mod(float x,
                     float y) {
        return x - y * (float) Math.floor(x / y);
    }
}
