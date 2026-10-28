/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

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
     * {@code galtype.comp.glsl} as {@code u_targetMeanRayIntegral}.
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
     * Mean-to-peak ratio of the Milky Way's dust, 4.2e-4: its dust is a few saturated lanes in an
     * almost empty box (0.3% of the voxels are lit). A shape diagnostic for a procedural dust
     * field - matching the mean alone is not enough, because a smooth field with the same mean has a
     * peak three orders of magnitude lower and reads as a uniform fog instead of lanes.
     */
    public static final double MW_OCCLUSIVE_MEAN_PEAK_RATIO = MW_OCCLUSIVE_MEAN_LUMA / MW_OCCLUSIVE_PEAK;

    private VolumeCalibration() {
    }
}
