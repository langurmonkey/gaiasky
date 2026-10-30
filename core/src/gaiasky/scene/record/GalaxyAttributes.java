/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.math.Vector3;
import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.util.LocalizedEnum;
import gaiasky.util.Settings.GraphicsQuality;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The shape of one component of a procedurally generated galaxy - a field.
 * <p>
 * These are the parameters of the analytic density model in {@code assets/shader/lib/galaxy.glsl},
 * which is what {@code galgen.comp.glsl} (billboard sampling) and {@code galtype.comp.glsl} (volume
 * voxelisation) both consume. Anything that describes the shape of the galaxy lives here; anything
 * specific to how the particles are drawn lives in {@link BillboardDataset}.
 * <p>
 * A volume entity declares a list of these directly; a billboard dataset is one of these plus its
 * sampler parameters.
 */
public class GalaxyAttributes {
    /**
     * Maximum number of supported colors.
     **/
    private static final int MAX_COLORS = 4;

    /**
     * Type (channel) of this field.
     */
    public ChannelType type = ChannelType.POINT;
    /**
     * Probability distribution.
     */
    public Distribution distribution = Distribution.DISK;
    /**
     * Base color(s) of this field. These colors will be used as base to generate the particle colors.
     * {@link #MAX_COLORS} RGBA colors are supported, so the size of this array must be {@link #MAX_COLORS} * 3.
     */
    public float[] baseColors = new float[MAX_COLORS * 3];
    /**
     * Color randomness to apply to the base colors to generate the final colors.
     */
    public float colorNoise = 0.08f;
    /**
     * Base radius of this field. 1 is the default radius, but you can make this component spread out wider or thinner by changing this.
     */
    public float baseRadius = 1;
    /**
     * Minimum radius of this field, in [0,1], where 0 is the very centre and 1 is {@link #baseRadius}.
     * <p>
     * What it does depends on the channel, because the density field is analytic rather than sampled
     * from particles:
     * <ul>
     *     <li>A {@code DUST} field is a ring of lanes whose inner edge is this radius: 0 lets the dust
     *     reach the core, and the default 0.01 nearly so. (The inner edge used to be a constant
     *     0.25 in {@code gxDustDensity()}, which is why setting this on a dust field only moved the
     *     arm modulation and the lanes stayed where they were.)</li>
     *     <li>Any other flat distribution uses it as the radius at which the azimuthal arm structure
     *     fades in, over {@code 0..2 * minRadius}. The radial profile is unaffected, so the disk of
     *     a spiral still peaks at the centre whatever this says.</li>
     *     <li>Spheroids ignore it: their profile is normalised on {@link #baseRadius} alone.</li>
     * </ul>
     */
    public float minRadius = 0.01f;

    /**
     * Translation vector for this field.
     */
    public Vector3 translation = new Vector3(0f, 0f, 0f);

    /**
     * Scale vector for this field.
     */
    public Vector3 scale = new Vector3(1f, 1f, 1f);

    /**
     * Strength of the warp, for flat distributions.
     */
    public float warpStrength = 0.0f;
    /**
     * Height scale.
     */
    public float heightScale = 0.01f;
    /**
     * Height (vertical) profile.
     */
    public HeightProfile heightProfile = HeightProfile.CONSTANT;
    /**
     * Eccentricities for density wave [1] and ellipsoid [2].
     */
    public float[] eccentricity = new float[]{0.3f, 0f};
    /**
     * Bar aspect ratio, e.g. 0.3 = short bar, 1.0 = long bar.
     */
    public float aspect = 1.0f;

    /**
     * Spiral arm pitch angle in degrees.
     * <p>
     * When using {@link Distribution#SPIRAL_LOG}, this controls how tightly the spiral arms wind around the galactic center.
     * Lower values (≈5–10°) produce tightly wound Sa-type spirals,
     * while higher values (≈25–40°) yield open Sc–Sd morphologies.
     * This parameter maps directly to the logarithmic spiral pitch angle
     * used in the compute shader.
     * <p>
     * When using {@link Distribution#SPIRAL}, this controls the total rotation of the concentric ellipses.
     */
    public float baseAngle = 6.0f;
    /**
     * Displacement of the ellipses in the {@link Distribution#SPIRAL} mode.
     */
    public float[] spiralDeltaPos = new float[]{0.0f, 0.0f};
    /**
     * Number of spiral arms.
     */
    public int numArms = 4;

    /**
     * Standard deviation across arm.
     */
    public float armSigma = 0.4f;

    /**
     * Amplitude of the azimuthal warp of the arms, in fractions of the arm period. Zero, the
     * default, leaves the lanes on their analytic log spiral; a few tenths of the arm spacing turn
     * the arms from drawn arcs into something with a varying sweep. Applies to the whole arm
     * modulation, so the gas and the dust of a galaxy bend together.
     */
    public float laneWarp = 0.0f;

    /**
     * Depth of the 3D noise mask, in [0,1]. Applies to every channel and every {@link #distribution}:
     * clumping is a property of the volume, not of a dust lane or of an arm.
     * <p>
     * The density is multiplied by a 3D noise field evaluated at each point, isotropic and in
     * cartesian space: a clump is a lump of material at a place in the box, so the field that carves
     * it is a function of that place and nothing else. It reaches zero in the troughs, which is what
     * breaks the field into separate clumps - where the field is drawn as extinction (dust) a tube of
     * it is far past saturation, so the visible boundary is a single level set of the density, and
     * only a factor that reaches zero can open it. On an emissive field the same remap gives the
     * mottling instead.
     * <p>
     * It lowers the mean density, so raise {@link #intensity} to compensate.
     *
     * @see #noiseMaskFreq
     * @see #noiseMaskOctaves
     */
    public float noiseMask = 0.0f;

    /**
     * Base frequency of the noise mask, in cycles per {@code baseRadius}. Zero or less means 10.
     * Larger values give smaller, denser clumps.
     */
    public float noiseMaskFreq = 0.0f;

    /**
     * Number of noise octaves summed into the mask, 1 to 8. Zero or less means 3. Octaves whose
     * wavelength the voxel grid cannot resolve are clamped down to it rather than dropped, so a
     * coarse grid gets coarser clumps, not a missing mask.
     */
    public int noiseMaskOctaves = 0;

    /**
     * Frequency multiplier of {@link #laneWarp} and {@link #laneRough}. Zero or less means 1. Not
     * applied to the noise mask, which has its own frequency.
     */
    public float laneNoiseScale = 1.0f;

    /**
     * Amplitude of the ragged edge of the dust lanes, in units of {@code armSigma}: 1 displaces the
     * edge by a few sigma at the peaks. Only {@link ChannelType#DUST} uses it.
     * <p>
     * The displacement is summed over four octaves starting at a few cycles per {@code baseRadius},
     * so the silhouette of a lane varies at every scale the voxel grid can carry. A single octave -
     * or a smooth function - moves the whole edge up and down together, which is what makes a lane
     * read as a deformed liquid surface rather than as a ragged one. Pair it with
     * {@link #noiseMask}, which does the breaking.
     */
    public float laneRough = 0.0f;

    /**
     * The intensity factor.
     */
    public float intensity = 1;

    /**
     * Whether this field absorbs light rather than emitting it. A procedural volume routes its
     * occluding fields into the occlusive slot and the rest into the emissive one.
     *
     * @return True if this field's channel occludes.
     */
    public boolean isOcclusive() {
        return type.occludes();
    }

    /** Vertical tail of the density field, in scale heights. Mirrors {@code GX_VERTICAL_TAIL}. **/
    private static final double GX_VERTICAL_TAIL = 3.0;
    /** Arm width, in units of {@link #baseRadius}. Mirrors {@code GX_ARM_WIDTH}. **/
    private static final double GX_ARM_WIDTH = 0.2;
    /** Dust slab thickness, as a fraction of {@link #heightScale}. Mirrors {@code GX_DUST_HEIGHT}. **/
    private static final double GX_DUST_HEIGHT = 0.5;

    /** Length in floats of one field in the packed uniform of {@code galtype.comp.glsl}. **/
    public static final int FIELD_STRIDE = 36;
    /**
     * Maximum number of fields summed into one volume slot. Mirrors {@code GX_MAX_FIELDS} in
     * {@code assets/shader/lib/galaxy.glsl}. Each slot gets its own packed array, so a galaxy can
     * carry up to twice this many fields.
     **/
    public static final int MAX_FIELDS = 8;

    /**
     * Packs this field of the density model into {@code out[offset..offset+FIELD_STRIDE]}.
     * <p>
     * The order is the member order of {@code GalaxyShape} in {@code assets/shader/lib/galaxy.glsl},
     * where {@code gxUnpack()} reads it back. Anything added to the struct has to be added here and to
     * {@code GX_FIELD_STRIDE} at the same time, or every field after it shifts by one.
     *
     * @param out    The destination array, at least {@code offset + FIELD_STRIDE} long.
     * @param offset Where this field starts.
     */
    public void packFields(float[] out, int offset) {
        out[offset] = distribution.ordinal();
        out[offset + 1] = baseRadius;
        out[offset + 2] = minRadius;
        out[offset + 3] = numArms;
        out[offset + 4] = armSigma;
        out[offset + 5] = baseAngle;
        out[offset + 6] = eccentricity[0];
        out[offset + 7] = eccentricity[1];
        out[offset + 8] = spiralDeltaPos[0];
        out[offset + 9] = spiralDeltaPos[1];
        out[offset + 10] = aspect;
        out[offset + 11] = warpStrength;
        out[offset + 12] = heightScale;
        out[offset + 13] = heightProfile.ordinal();
        out[offset + 14] = colorNoise;
        out[offset + 15] = intensity;
        System.arraycopy(baseColors, 0, out, offset + 16, MAX_COLORS * 3);
        out[offset + 28] = type.ordinal();
        out[offset + 29] = laneWarp;
        out[offset + 30] = noiseMask;
        out[offset + 31] = laneNoiseScale;
        out[offset + 32] = laneRough;
        out[offset + 33] = noiseMaskFreq;
        out[offset + 34] = (float) noiseMaskOctaves;
    }

    /**
     * Half-extent of this field laterally, in galaxy space.
     * <p>
     * Mirrors {@code gxLateralHalfExtent()} in {@code assets/shader/lib/galaxy.glsl}. Absolute units,
     * not normalised: the fields sharing a slot have their own {@link #baseRadius}, so their boxes
     * have to be unioned in a common space.
     */
    public double volumeLateralHalfExtent() {
        if (!(distribution.isFlat() || distribution.isBar())) {
            return baseRadius;
        }
        return baseRadius * (1.0 + MathUtilsDouble.clamp(GX_VERTICAL_TAIL * armSigma * GX_ARM_WIDTH, 0.0, 0.2));
    }

    /** Half-extent of this field vertically, in galaxy space. Mirrors {@code gxVerticalHalfExtent()}. **/
    public double volumeVerticalHalfExtent() {
        if (!(distribution.isFlat() || distribution.isBar())) {
            return volumeLateralHalfExtent();
        }
        return GX_VERTICAL_TAIL * effectiveHeightScale() + Math.abs(warpStrength);
    }

    /** Scale height of the field: dust sits in a thinner slab than the gas. Mirrors {@code gxHeightScale()}. **/
    public double effectiveHeightScale() {
        return type == ChannelType.DUST ? GX_DUST_HEIGHT * heightScale : heightScale;
    }

    /**
     * Vertical-to-lateral aspect ratio of the box that encloses this distribution, used to size the
     * volume grid of procedurally generated galaxies. Flat distributions (and bars) are thin, the
     * rest are isotropic.
     * <p>
     * Must match {@code gxBoxAspect()} in {@code assets/shader/lib/galaxy.glsl}, which is where the
     * density field defines the box it lives in.
     */
    public double volumeAspectY() {
        return MathUtilsDouble.clamp(volumeVerticalHalfExtent() / Math.max(volumeLateralHalfExtent(), 1e-6), 0.02, 1.0);
    }

    /**
     * Total angle the arm centreline sweeps between {@link #minRadius} and {@link #baseRadius}, in
     * degrees.
     * <p>
     * Mirrors the arm centreline of {@code gxArmFactor()}: {@code theta = b * ln(r / (0.1 *
     * baseRadius))} with {@code b = tan(baseAngle * 0.1)}. Note what that makes {@link #baseAngle}:
     * the angle at which the arm leaves the <em>radial</em> direction, in tenths of a degree, so
     * larger means more wound and 0 means radial spokes. A sweep under ~25 deg is a cross of four
     * spokes rather than a spiral, which is why {@code GalaxyVolumeGenerator} warns about it.
     */
    public double spiralArmSweepDegrees() {
        if (!distribution.isAnySpiral()) {
            return 0.0;
        }
        double deg = MathUtilsDouble.clamp(baseAngle * 0.1, 0.0, 85.0);
        double b = Math.tan(Math.toRadians(deg));
        // r runs from minRadius * baseRadius to baseRadius, so ln(r / (0.1 * baseRadius)) goes from
        // ln(10 * minRadius) to ln(10) and the difference is ln(1 / minRadius).
        return Math.toDegrees(b * Math.log(1.0 / MathUtilsDouble.clamp(minRadius, 1e-4, 1.0)));
    }

    /**
     * Derives the volume grid of a whole slot, which holds several fields at once - gas and bulge
     * share the emissive slot - so the box has to contain all of them, not just the first.
     *
     * @param fields  The fields going into the slot.
     * @param sizePc  Lateral span of the galaxy, in parsecs.
     * @param quality Graphics quality, which sets the target voxel size and the voxel budget.
     *
     * @return The grid.
     */
    public static VoxelGrid volumeGrid(List<? extends GalaxyAttributes> fields,
                                       double sizePc,
                                       GraphicsQuality quality) {
        double lateral = 0.0, vertical = 0.0;
        for (var field : fields) {
            lateral = Math.max(lateral, field.volumeLateralHalfExtent());
            vertical = Math.max(vertical, field.volumeVerticalHalfExtent());
        }
        return VoxelGrid.forSpan(sizePc, vertical / Math.max(lateral, 1e-6), quality);
    }

    /**
     * Derives the volume grid this field should be voxelised into on its own.
     *
     * @param sizePc  Lateral span of the galaxy, in parsecs.
     * @param quality Graphics quality, which sets the target voxel size and the voxel budget.
     *
     * @return The grid.
     */
    public VoxelGrid volumeGrid(double sizePc,
                                GraphicsQuality quality) {
        return VoxelGrid.forSpan(sizePc, volumeAspectY(), quality);
    }

    public void setIntensity(Double intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setIntensity(Long intensity) {
        this.intensity = intensity.floatValue();
    }

    public void setBaseRadius(Double baseRadius) {
        this.baseRadius = baseRadius.floatValue();
    }

    public void setBaseRadius(Long baseRadius) {
        this.baseRadius = baseRadius.floatValue();
    }

    public void setMinRadius(Double minRadius) {
        this.minRadius = minRadius.floatValue();
    }

    public void setMinRadius(Long minRadius) {
        this.minRadius = minRadius.floatValue();
    }

    public void setTranslation(double[] d) {
        this.translation.set((float) d[0], (float) d[1], (float) d[2]);
    }

    public void setTranslation(int[] d) {
        this.translation.set((float) d[0], (float) d[1], (float) d[2]);
    }

    public void setScale(double[] s) {
        this.scale.set((float) s[0], (float) s[1], (float) s[2]);
    }

    public void setScale(int[] s) {
        this.scale.set((float) s[0], (float) s[1], (float) s[2]);
    }

    public void setScaleX(double s) {
        this.scale.x = (float) s;
    }

    public void setScaleY(double s) {
        this.scale.y = (float) s;
    }

    public void setScaleZ(double s) {
        this.scale.z = (float) s;
    }

    public void setEccentricity(Double eccentricity) {
        this.eccentricity[0] = eccentricity.floatValue();
    }

    public void setEccentricity(Long eccentricity) {
        this.eccentricity[0] = eccentricity.floatValue();
    }

    public void setEccentricityX(Double eccentricity) {
        setEccentricity(eccentricity);
    }

    public void setEccentricityX(Long eccentricity) {
        setEccentricity(eccentricity);
    }

    public void setEccentricityY(Double eccentricity) {
        this.eccentricity[1] = eccentricity.floatValue();
    }

    public void setEccentricityY(Long eccentricity) {
        this.eccentricity[1] = eccentricity.floatValue();
    }

    public void setEccentricity(double[] eccentricity) {
        this.eccentricity[0] = (float) eccentricity[0];
        this.eccentricity[1] = (float) eccentricity[1];
    }

    public void setWarpStrength(Double warpStrength) {
        this.warpStrength = warpStrength.floatValue();
    }

    public void setWarpStrength(Long warpStrength) {
        this.warpStrength = warpStrength.floatValue();
    }

    public void setHeightScale(Double heightScale) {
        this.heightScale = heightScale.floatValue();
    }

    public void setHeightScale(Long heightScale) {
        this.heightScale = heightScale.floatValue();
    }

    public void setHeightProfile(String profile) {
        if (profile != null && !profile.isBlank()) {
            this.heightProfile = HeightProfile.valueOf(profile.toUpperCase(Locale.ROOT));
        }
    }

    public void setHeightProfile(HeightProfile p) {
        this.heightProfile = p;
    }

    public void setHeightParameters(Double warpStrength,
                                    Double heightScale,
                                    HeightProfile profile) {
        setWarpStrength(warpStrength);
        setHeightScale(heightScale);
        setHeightProfile(profile);
    }

    public void setBaseAngle(Double baseAngle) {
        this.baseAngle = baseAngle.floatValue();
    }

    public void setBaseAngle(Long baseAngle) {
        this.baseAngle = baseAngle.floatValue();
    }

    public void setNumArms(Long numArms) {
        this.numArms = numArms.intValue();
    }

    public void setSpiralArms(Long spiralArms) {
        setNumArms(spiralArms);
    }

    public void setArmSigma(Double armSigma) {
        this.armSigma = armSigma.floatValue();
    }

    public void setArmSigma(Long armSigma) {
        this.armSigma = armSigma.floatValue();
    }

    public void setLaneWarp(Double laneWarp) {
        this.laneWarp = laneWarp.floatValue();
    }

    public void setLaneWarp(Long laneWarp) {
        this.laneWarp = laneWarp.floatValue();
    }

    public void setNoiseMask(Double noiseMask) {
        this.noiseMask = MathUtilsDouble.clamp(noiseMask.floatValue(), 0f, 1f);
    }

    public void setNoiseMask(Long noiseMask) {
        setNoiseMask(noiseMask.doubleValue());
    }

    public void setLaneNoiseScale(Double laneNoiseScale) {
        this.laneNoiseScale = laneNoiseScale.floatValue();
    }

    public void setLaneNoiseScale(Long laneNoiseScale) {
        this.laneNoiseScale = laneNoiseScale.floatValue();
    }

    public void setNoiseMaskFreq(Double noiseMaskFreq) {
        this.noiseMaskFreq = noiseMaskFreq.floatValue();
    }

    public void setNoiseMaskFreq(Long noiseMaskFreq) {
        this.noiseMaskFreq = noiseMaskFreq.floatValue();
    }

    public void setNoiseMaskOctaves(Long noiseMaskOctaves) {
        this.noiseMaskOctaves = MathUtilsDouble.clamp(noiseMaskOctaves.intValue(), 1, 8);
    }

    public void setLaneRough(Double laneRough) {
        this.laneRough = MathUtilsDouble.clamp(laneRough.floatValue(), 0f, 4f);
    }

    public void setLaneRough(Long laneRough) {
        setLaneRough(laneRough.doubleValue());
    }

    /**
     * Sets the lane perturbations in one go: warp, roughness and frequency scale, from one to three
     * values. Missing ones are left as they are.
     *
     * @param v The values.
     */
    public void setLaneNoise(double[] v) {
        if (v.length > 0) setLaneWarp(v[0]);
        if (v.length > 1) setLaneRough(v[1]);
        if (v.length > 2) setLaneNoiseScale(v[2]);
    }

    public void setLaneNoise(int[] v) {
        if (v.length > 0) setLaneWarp((double) v[0]);
        if (v.length > 1) setLaneRough((double) v[1]);
        if (v.length > 2) setLaneNoiseScale((double) v[2]);
    }

    /**
     * Sets the noise mask in one go: amplitude, frequency and octaves, from one to three values.
     * Missing ones are left as they are.
     *
     * @param v The values.
     */
    public void setNoiseMaskNoise(double[] v) {
        if (v.length > 0) setNoiseMask(v[0]);
        if (v.length > 1) setNoiseMaskFreq(v[1]);
        if (v.length > 2) setNoiseMaskOctaves((long) v[2]);
    }

    public void setNoiseMaskNoise(int[] v) {
        if (v.length > 0) setNoiseMask((double) v[0]);
        if (v.length > 1) setNoiseMaskFreq((double) v[1]);
        if (v.length > 2) setNoiseMaskOctaves((long) v[2]);
    }

    public void setAspect(Double aspect) {
        this.aspect = aspect.floatValue();
    }

    public void setAspect(Long aspect) {
        this.aspect = aspect.floatValue();
    }

    public void setType(ChannelType type) {
        this.type = type;
    }

    public void setType(String type) {
        if (type != null && !type.isBlank()) {
            this.type = ChannelType.valueOf(type.toUpperCase(Locale.ROOT));
        }
    }

    public void setDistribution(Distribution distribution) {
        this.distribution = distribution;
    }

    public void setDistribution(String distribution) {
        if (distribution != null && !distribution.isBlank()) {
            this.distribution = Distribution.valueOf(distribution.toUpperCase(Locale.ROOT));
        }
    }

    public void setSpiralDeltaPos(double[] d) {
        if (d.length == 1) {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[0];
        } else {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[1];
        }
    }

    public void setSpiralDeltaPos(int[] d) {
        if (d.length == 1) {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[0];
        } else {
            this.spiralDeltaPos[0] = (float) d[0];
            this.spiralDeltaPos[1] = (float) d[1];
        }
    }

    public void setDisplacement(Double d) {
        this.spiralDeltaPos[0] = d.floatValue();
        this.spiralDeltaPos[1] = d.floatValue();
    }

    /**
     * Sets the given color to the given index in the colors array.
     *
     * @param r   The red.
     * @param g   The green.
     * @param b   The blue.
     * @param idx The index of the color, in [0,MAX_COLORS-1]
     */
    private void setColor(double r,
                          double g,
                          double b,
                          int idx) {
        assert idx >= 0 && idx < MAX_COLORS;
        this.baseColors[idx * 3] = (float) r;
        this.baseColors[idx * 3 + 1] = (float) g;
        this.baseColors[idx * 3 + 2] = (float) b;
    }

    public void setBaseColors(float[] baseColors) {
        this.baseColors = Arrays.copyOf(baseColors, baseColors.length);
    }

    /**
     * Sets the base colors. Four RGB colors are supported, so the size of the array must be one of [3, 6, 9, 12].
     *
     * @param baseColors The base colors.
     */
    public void setBaseColors(double[] baseColors) {
        assert baseColors.length == 3 || baseColors.length == 6 || baseColors.length == 9 || baseColors.length == 12;
        switch (baseColors.length) {
            case 3 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[0], baseColors[1], baseColors[2], 1);
                setColor(baseColors[0], baseColors[1], baseColors[2], 2);
                setColor(baseColors[0], baseColors[1], baseColors[2], 3);
            }
            case 6 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[0], baseColors[1], baseColors[2], 1);
                setColor(baseColors[3], baseColors[4], baseColors[5], 2);
                setColor(baseColors[3], baseColors[4], baseColors[5], 3);
            }
            case 9 -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[3], baseColors[4], baseColors[5], 1);
                setColor(baseColors[6], baseColors[7], baseColors[8], 2);
                setColor(baseColors[0], baseColors[1], baseColors[2], 3);
            }
            default -> {
                setColor(baseColors[0], baseColors[1], baseColors[2], 0);
                setColor(baseColors[3], baseColors[4], baseColors[5], 1);
                setColor(baseColors[6], baseColors[7], baseColors[8], 2);
                setColor(baseColors[9], baseColors[10], baseColors[11], 3);
            }
        }
        for (int i = 0; i < baseColors.length; i++) {
            this.baseColors[i] = (float) baseColors[i];
        }
    }

    public void setBaseColor(double[] baseColors) {
        setBaseColors(baseColors);
    }

    public float[] getColorRGBA(int i) {
        var c = new float[4];
        c[0] = baseColors[i * 3];
        c[1] = baseColors[i * 3 + 1];
        c[2] = baseColors[i * 3 + 2];
        c[3] = 1f;
        return c;
    }

    public void setColorRGBA(float[] rgba,
                             int i) {
        baseColors[i * 3] = rgba[0];
        baseColors[i * 3 + 1] = rgba[1];
        baseColors[i * 3 + 2] = rgba[2];
    }

    public void setColorNoise(Double colorNoise) {
        this.colorNoise = MathUtilsDouble.clamp(colorNoise.floatValue(), 0f, 1f);
    }

    /**
     * Sets the spiral parameters all in one go.
     *
     * @param baseAngle The base angle.
     * @param ec        The eccentricity.
     * @param deltaPos  The delta pos.
     * @param numArms   The number of arms.
     * @param armSigma  The arm spread.
     */
    public void setSpiralData(double baseAngle,
                              double ec,
                              double[] deltaPos,
                              long numArms,
                              double armSigma) {
        setBaseAngle(baseAngle);
        setEccentricity(ec);
        setSpiralDeltaPos(deltaPos);
        setNumArms(numArms);
        setArmSigma(armSigma);
    }

    /**
     * Channel types. Particle types are essentially parameter
     * ranges for all parameters of a billboard dataset.
     */
    public enum ChannelType implements LocalizedEnum {
        DUST(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
             new int[]{0, 30_000},
             new int[]{0, 1, 2},
             new float[]{0f, 100f},
             new float[]{0f, 30f},
             new float[]{0.0f, 0.08f},
             null,
             null,
             null,
             null,
             null,
             null,
             null),
        BULGE(new String[]{"sphere", "bar", "ellipse", "gauss"},
              new int[]{0, 100},
              new int[]{0, 1, 2},
              new float[]{0f, 300f},
              new float[]{0f, 30f},
              new float[]{0.0f, 2.0f},
              new float[]{0.0f, 0.05f},
              new float[]{0.05f, 0.25f},
              null,
              null,
              null,
              null,
              null),
        STAR(new String[]{"gauss", "disk", "ellipse", "sphere"},
             new int[]{0, 100_000},
             new int[]{0, 1, 2},
             new float[]{0.0f, 8.0f},
             new float[]{0.05f, 0.2f},
             new float[]{0.0f, 6.0f},
             null,
             null,
             null,
             null,
             null,
             null,
             null),
        GAS(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
            new int[]{0, 30_000},
            new int[]{0, 1, 2, 3},
            new float[]{0f, 100f},
            new float[]{0f, 30f},
            new float[]{0.0f, 0.05f},
            null,
            null,
            null,
            null,
            null,
            null,
            null),
        HII(new String[]{"density", "disk", "sphere", "ellipse", "gauss"},
            new int[]{0, 10_000},
            new int[]{4},
            new float[]{0f, 10f},
            new float[]{0f, 30f},
            new float[]{0.0f, 6.0f},
            null,
            null,
            null,
            null,
            null,
            null,
            null),
        POINT(new String[]{"density", "disk", "sphere", "bar", "ellipse", "gauss"},
              new int[]{0, 50_000},
              new int[]{1},
              new float[]{0f, 1000f},
              new float[]{0f, 30f},
              new float[]{0.0f, 1.0f},
              null,
              null,
              null,
              null,
              null,
              null,
              null);

        /** Available distributions. **/
        public String[] distributions;
        /** Number of particles range. **/
        public int[] nParticles;
        /** Particle layers range. **/
        public int[] layers;
        /** Particle size range. **/
        public float[] size;
        /** Particle max size range. **/
        public float[] maxSize;
        /** Particle intensity range. **/
        public float[] intensity;
        /** Minimum radius range. **/
        public float[] minRadius = new float[]{0.0f, 0.4f};
        /** Base radius range. **/
        public float[] baseRadius = new float[]{0.8f, 2.0f};
        /** Warp strength range. **/
        public final float[] warpStrength = new float[]{-0.1f, 0.1f};

        /** Base angle [deg] range, for {@link Distribution#SPIRAL}, {@link Distribution#SPIRAL_LOG}, and {@link Distribution#CONE}. **/
        public float[] baseAngle = new float[]{0f, 1000f};
        /** Eccentricity range, for {@link Distribution#SPIRAL} and {@link Distribution#ELLIPSOID}. **/
        public float[] eccentricity = new float[]{0.0f, 1.0f};
        /** Delta pos (in X and Y) range, for {@link Distribution#SPIRAL}. **/
        public float[] spiralDeltaPos = new float[]{-0.5f, 0.5f};
        /** Aspect ratio range, for {@link Distribution#BAR}. **/
        public float[] aspect = new float[]{0.0f, 0.3f};
        /** Standard deviation across arm. **/
        public float[] armSigma = new float[]{0.0f, 1.0f};

        ChannelType(String[] distributions,
                    int[] nParticles,
                    int[] layers,
                    float[] size,
                    float[] maxSize,
                    float[] intensity,
                    float[] minRadius,
                    float[] baseRadius,
                    float[] baseAngle,
                    float[] eccentricity,
                    float[] displacement,
                    float[] aspect,
                    float[] armSigma) {
            if (distributions != null) {
                this.distributions = distributions;
            }
            if (nParticles != null) {
                this.nParticles = nParticles;
            }
            if (layers != null) {
                this.layers = layers;
            }
            if (size != null) {
                this.size = size;
            }
            if (maxSize != null) {
                this.maxSize = maxSize;
            }
            if (intensity != null) {
                this.intensity = intensity;
            }
            if (minRadius != null) {
                this.minRadius = minRadius;
            }
            if (baseRadius != null) {
                this.baseRadius = baseRadius;
            }
            if (baseAngle != null) {
                this.baseAngle = baseAngle;
            }
            if (eccentricity != null) {
                this.eccentricity = eccentricity;
            }
            if (displacement != null) {
                this.spiralDeltaPos = displacement;
            }
            if (aspect != null) {
                this.aspect = aspect;
            }
            if (armSigma != null) {
                this.armSigma = armSigma;
            }
        }

        /**
         * Whether this channel absorbs light rather than emitting it. Dust is the only absorbing
         * channel: gas, bulge, stars and HII all emit. This decides which of the two volume slots a
         * procedurally generated field lands in.
         *
         * @return True if this channel occludes.
         */
        public boolean occludes() {
            return this == DUST;
        }

        @Override
        public String localizedName() {
            return I18n.msg("gui.galaxy.ds.type." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Particle distributions. **/
    public enum Distribution implements LocalizedEnum {
        /** Simple uniformly distributed sphere. **/
        SPHERE,
        /** Simple uniformly distributed disk. **/
        DISK,
        /** Density wave distribution, producing natural spirals. **/
        SPIRAL,
        /** An artificial logarithmic spiral. **/
        SPIRAL_LOG,
        /** A simple bar. **/
        BAR,
        /** An ellipse, with an eccentricity. **/
        ELLIPSOID,
        /** Gaussian distribution in a disk, with an overdense center. **/
        DISK_GAUSS,
        /** Gaussian distribution in a sphere. **/
        SPHERE_GAUSS,
        /** A cone distribution. **/
        CONE,
        /** An irregular distribution. **/
        IRREGULAR;

        public boolean isFlat() {
            return this == DISK || this == DISK_GAUSS || this == SPIRAL || this == SPIRAL_LOG;
        }

        public boolean isAnySpiral() {
            return isDensityWaveSpiral() || isLogarithmicSpiral();
        }

        public boolean isDensityWaveSpiral() {
            return this == SPIRAL;
        }

        public boolean isLogarithmicSpiral() {
            return this == SPIRAL_LOG;
        }

        public boolean isEllipsoid() {
            return this == ELLIPSOID;
        }

        public boolean isBar() {
            return this == BAR;
        }

        @Override
        public String localizedName() {
            return I18n.msg("gui.galaxy.ds.distribution." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** Vertical dispersion profile, with {@link #heightScale} as base. **/
    public enum HeightProfile {
        /** Constant over radius. **/
        CONSTANT,
        /** Increasing linearly with radius. **/
        LINEAR_INC,
        /** Decreasing linearly with radius. **/
        LINEAR_DEC,
        /** Increasing smoothly with radius (smoothstep). **/
        SMOOTH_INC,
        /** Decreasing smoothly with radius (smoothstep). **/
        SMOOTH_DEC,
    }
}
