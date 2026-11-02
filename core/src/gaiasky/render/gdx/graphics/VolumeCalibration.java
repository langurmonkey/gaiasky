/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.scene.record.GalaxyAttributes;

import java.util.List;

/**
 * Brightness calibration of a procedurally generated galaxy volume against the Milky Way's own
 * volume.
 * <p>
 * The two volumes are built by different code - one splats particles with a smoothing kernel, the
 * other evaluates an analytic field - so nothing makes them agree on absolute brightness. What they
 * can agree on is an integral: the <em>mean ray integral</em>, the emission a face-on ray
 * accumulates through the box, averaged over its cross-section. It is invariant in resolution and
 * in shape, and in UVW units - the units the raymarcher integrates in - it reduces to the mean luma
 * of the stored voxels, which is what the voxeliser measures.
 * <p>
 * The reference numbers are measured from the textures shipped in
 * {@code data/default-data/galaxy/volume/gal-med}, written by
 * {@code core/scripts/other/volumes/particles-to-volume.py}. Two things to keep in mind when
 * re-deriving them after the Milky Way data changes:
 * <ul>
 *     <li>The "Mean ray integral" line of each metadata file is in <em>box</em> units (the box has a
 *     side of 1.37 and a thickness of 0.137), so it is that many times smaller than the UVW number
 *     this class needs: {@code 2.7606e-4 / 0.137 = 2.015e-3}, which is what the shipped texture
 *     actually measures over its 6.4M voxels.</li>
 *     <li>The stored bytes already have the descriptor's intensities baked in (gas 0.7, bulge 0.2,
 *     dust 0.06), so the target for a field at <em>unit</em> intensity is that measurement divided
 *     by the intensity of the channel that dominates it. Dividing is what keeps an intensity a
 *     linear knob: the voxeliser normalises by peak and mean measured at unit intensity, so a
 *     procedural galaxy whose descriptor says 0.7 renders like the Milky Way's gas at 0.7, and one
 *     that says 2 is twice as bright.</li>
 * </ul>
 */
public final class VolumeCalibration {

    /**
     * Mean luma of the Milky Way's emissive texture, in UVW units, as shipped (gas at 0.7, bulge at
     * 0.2). Measured over {@code gal-med/galaxy_emissive_rgba.raw.gz}: 399x40x400 voxels.
     */
    public static final double MW_EMISSIVE_MEAN_LUMA = 0.002015;
    /**
     * Intensity of the Milky Way's gas channel, the emitter that dominates that volume: 14864 gas
     * particles against 933 of the bulge, spread over the whole disk against a compact core. The
     * division is therefore approximate - the bulge's 0.2 adds a few tens of percent on top of the
     * gas's 0.7 - and the A/B screenshot against the billboard Andromeda is what settles it.
     */
    public static final double MW_GAS_INTENSITY = 0.7;
    /**
     * Target mean luma of the emissive slot at unit field intensity. Passed to
     * {@code galgen.volume.comp.glsl} as {@code u_targetMeanRayIntegral}.
     */
    public static final double EMISSIVE_TARGET_MEAN_LUMA = MW_EMISSIVE_MEAN_LUMA / MW_GAS_INTENSITY;

    /** Mean luma of the Milky Way's occlusive texture, in UVW units, as shipped (dust at 0.06). **/
    public static final double MW_OCCLUSIVE_MEAN_LUMA = 0.000019333;
    /** Intensity of the Milky Way's dust channel. **/
    public static final double MW_DUST_INTENSITY = 0.06;
    /** Target mean luma of the occlusive slot at unit field intensity. **/
    public static final double OCCLUSIVE_TARGET_MEAN_LUMA = MW_OCCLUSIVE_MEAN_LUMA / MW_DUST_INTENSITY;
    /** Peak stored brightness of the Milky Way's dust texture. **/
    public static final double MW_OCCLUSIVE_PEAK = 0.059;
    /**
     * Target stored peak of the occlusive slot at unit field intensity: 0.98, applied <em>per
     * field</em>.
     * <p>
     * The occlusive slot is calibrated on its <em>peak</em> and not on its mean, unlike the emissive
     * one. What the raymarcher compares against {@code u_volumeNDepthThreshold} is the lanes, which
     * sit at the peak; the mean is dominated by the faint tail that never occludes anything, so
     * matching it would let a galaxy with the right mean and no lanes look correct. The mean-to-peak
     * ratio below is then a shape diagnostic rather than a target - the analytic dust field measures
     * 3.5e-4 against the Milky Way's 3.3e-4, which is what says the lanes are where they should be.
     * <p>
     * Per field, not per slot: each field is divided by the peak of its own unit density, so the
     * densest voxel of any component stores {@code OCCLUSIVE_TARGET_PEAK * intensity} whatever its
     * shape. Normalising by the peak of the sum instead makes a field's stored brightness depend on
     * the others: a broad flat field peaks an order of magnitude above a lane field, so adding one to
     * the slot divides every lane by that ratio and the threshold's level set stops being the lanes.
     * <p>
     * Note the consequence of the per-unit convention: the Milky Way's own dust intensity is 0.06, so
     * a procedural dust field at intensity 1 saturates the 8-bit alpha. That is intentional - an
     * intensity of 1 means "about 17 times the Milky Way's dust". It is also what makes an intensity
     * usable as a switch for occlusion: a field whose intensity puts it below the slot's depth
     * threshold only dims, and never hides anything behind it.
     */
    public static final double OCCLUSIVE_TARGET_PEAK = MW_OCCLUSIVE_PEAK / MW_DUST_INTENSITY;

    /**
     * Stored depth threshold that follows from the occlusion fractions of a slot's fields: the
     * intensity-weighted sum of {@code OCCLUSIVE_TARGET_PEAK * intensity * occlusionThreshold},
     * clamped to [0,1].
     * <p>
     * This is what turns a per-field, dimensionless fraction into the single number the raymarcher
     * compares the stored density against. The stored alpha of a slot is
     * {@code OCCLUSIVE_TARGET_PEAK * sum(intensity * relativeDensity)}, so testing it against this
     * value is exactly "the intensity-weighted relative density is above the intensity-weighted
     * fraction". With one field in the slot that is "this field is above its fraction of its own
     * peak"; with several, it is the aggregate, and the value stays a fraction of a peak rather than
     * a number that has to be re-tuned whenever an intensity changes.
     * <p>
     * Zero means no field asks to occlude, and the raymarcher reads a threshold of 0 as "this slot
     * never occludes" - which is why a fractional value near 0 still has to be written as a small
     * positive number rather than as 0.
     *
     * @param fields The fields of the occlusive slot.
     *
     * @return The stored threshold, in [0,1]. Zero when no field asks to occlude.
     */
    public static float occlusiveThreshold(List<GalaxyAttributes> fields) {
        if (fields == null || fields.isEmpty()) {
            return 0f;
        }
        double weighted = 0.0;
        for (var field : fields) {
            weighted += Math.max(0.0, field.intensity) * Math.max(0.0, field.occlusionThreshold);
        }
        return (float) Math.min(1.0, OCCLUSIVE_TARGET_PEAK * weighted);
    }
    /**
     * Mean-to-peak ratio of the Milky Way's dust, 4.2e-4: its dust is a few saturated lanes in an
     * almost empty box (0.3% of the voxels are lit). A shape diagnostic for a procedural dust
     * field - matching the mean alone is not enough, because a smooth field with the same mean has a
     * peak three orders of magnitude lower and reads as a uniform fog instead of lanes.
     */
    public static final double MW_OCCLUSIVE_MEAN_PEAK_RATIO = MW_OCCLUSIVE_MEAN_LUMA / MW_OCCLUSIVE_PEAK;

    private VolumeCalibration() {
    }
}
