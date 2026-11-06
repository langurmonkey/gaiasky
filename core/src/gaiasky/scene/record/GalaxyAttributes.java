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
 * which is what {@code galgen.billboard.comp.glsl} (billboard sampling) and {@code galgen.volume.comp.glsl} (volume
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
     * Outer radius of this field, as a fraction of the object radius. 1 fills the object, 0.5 covers
     * half of it. It is a size in both channel types: the particle generators place their particles
     * within it, and the volume places the field within the object's box. Values above 1 have no
     * meaning for a volume, whose box ends at the object's radius.
     */
    public float baseRadius = 1;
    /**
     * Minimum radius of this field, as a fraction of the object radius - the same quantity the
     * particle generators take it for, and the same space as {@link #baseRadius}.
     * <p>
     * It is the field's inner edge, in every channel and every distribution: the volume cuts the
     * density below it ({@code gxMinRn()}), and the particle generators start their distributions
     * there. Beyond that, the flat distributions use it twice:
     * <ul>
     *     <li>A {@code DUST} field is a ring of lanes whose inner edge is this radius: 0 lets the dust
     *     reach the core, and the default 0.01 nearly so.</li>
     *     <li>Any other flat distribution uses it as the radius at which the azimuthal arm structure
     *     fades in, over {@code 0..2 * minRadius}.</li>
     * </ul>
     */
    public float minRadius = 0.0f;

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
     * Frequency multiplier of {@link #laneWarp}. Zero or less means 1. Not applied to the noise mask
     * or to the lane substructure, which have their own frequencies.
     */
    public float laneNoiseScale = 1.0f;

    /**
     * How the width of a dust lane changes along the arm, in [-1,1]. Zero keeps it constant. A
     * positive value thins the lanes at the centre and widens them outward, a negative one does the
     * mirror image.
     * <p>
     * The width is scaled by {@code 2^(taper * (2 * rn - 1) * 1.5)}: a taper of 1 takes it down to
     * about a third at the centre and up to nearly three times at the rim.
     * <p>
     * Widening the outer lanes only shows if there is dust out there to widen, and the dust envelope
     * is {@code exp(-rn / 0.33)} raised to the fourth power: on the plain envelope the lanes fall
     * under one 8-bit step past {@code rn ≈ 0.45}, so a widened lane out there would be quantised
     * away and all the outward half of the slider would do is thin what is visible. A positive taper
     * therefore also stretches the envelope (out to twice the scale radius at 1), which is what
     * makes the outer lanes thick and bright instead of merely wide. A negative one does not need to:
     * it thickens the part of the disk that is bright anyway.
     * <p>
     * Only {@link ChannelType#DUST} in a spiral uses it: the gas is a smooth exponential along the
     * whole disk and its width is not a lane's.
     */
    public float laneWidthTaper = 0.0f;

    /**
     * Depth of the substructure inside a dust lane, in [0,1]: how far the gaps between its strands
     * drop below the lane's own density. Only {@link ChannelType#DUST} in a spiral uses it.
     * <p>
     * {@link #laneWarp} moves the centreline of a lane, so it can make it meander but not texturise
     * it: its cross-section is a Gaussian of a fixed width and a fixed peak. This is the missing
     * axis. It multiplies the lane's density by a field thresholded into
     * strands, so 0 leaves the lane smooth and a value near 1 leaves it as strands separated by
     * gaps of two orders of magnitude - which is what a real lane is. Values in between keep a veil
     * of material between the strands, so the lane stays continuous.
     * <p>
     * It lowers the mean dust density, so raise {@link #intensity} to compensate.
     *
     * @see #filamentScale
     * @see #filamentThreshold
     */
    public float filamentContrast = 0.88f;

    /**
     * Frequency of {@link #filamentContrast} across the lane, in cycles per {@code baseRadius}.
     * Zero or less means 20.
     * <p>
     * A strand is long along the lane and thin across it, so this is what sets its width, and it is
     * independent of {@link #laneNoiseScale}, which sets the scale of the warp. Past roughly 24 the
     * grid cannot carry the pattern and the strands alias into beads.
     */
    public float filamentScale = 20.0f;

    /**
     * Level, in [0,1] of the noise field behind {@link #filamentContrast}, above which a point is
     * inside a strand. Higher values keep fewer, thinner strands and empty more of the lane.
     */
    public float filamentThreshold = 0.50f;

    /**
     * The intensity factor.
     */
    public float intensity = 1;

    /**
     * Fraction of this field's own peak density above which it hides what is behind it, in [0,1].
     * This is the occlusion threshold of the field, and it is only meaningful on an occluding channel
     * ({@link ChannelType#DUST}); other channels go into the emissive slot and never write depth.
     * <p>
     * Being a fraction of the field's own peak, it means the same thing in every scene and does not
     * move when {@link #intensity} does: the intensity is how much dust there is, this is how much of
     * it is dense enough to be opaque. A small value (0.01 to 0.05) lets the field occlude almost
     * everywhere it has any density, a value near 1 keeps only the very core of it, and 0 - the
     * default - means this field does not hide anything at all. That last one matters beyond the
     * field: the raymarcher reads a slot threshold of 0 as "this slot never occludes", so a slot
     * whose fields are all at 0 is switched off rather than switched to maximum.
     * <p>
     * The slot's stored threshold is the intensity-weighted sum of
     * {@code OCCLUSIVE_TARGET_PEAK * intensity * occlusionThreshold} over its fields (see
     * {@link gaiasky.render.gdx.graphics.VolumeCalibration#occlusiveThreshold(List)}), so with one
     * field in the slot the comparison is exactly "this field's density is above this fraction of its
     * peak"; with several the test is on their intensity-weighted aggregate, and the densest field
     * decides what the ray meets first. A positive entry in
     * {@link gaiasky.scene.record.MaterialComponent#volumeDepthThreshold} still overrides the whole
     * derivation, in stored density, for a scene that wants to pin the number by hand.
     */
    public float occlusionThreshold = 0.0f;

    /**
     * Setter used by the scene JSON loader.
     */
    public void setOcclusionThreshold(Double occlusionThreshold) {
        this.occlusionThreshold = MathUtilsDouble.clamp(occlusionThreshold.floatValue(), 0f, 1f);
    }

    public void setOcclusionThreshold(Long occlusionThreshold) {
        setOcclusionThreshold(occlusionThreshold.doubleValue());
    }

    /**
     * Default constructor, used by the scene JSON loader and by {@link BillboardDataset}.
     */
    public GalaxyAttributes() {
    }

    /**
     * Creates a field with the same shape as another one.
     * <p>
     * This copies exactly the members that describe the density model, so a
     * {@link BillboardDataset} can be turned into a volume field without carrying its sampler
     * parameters (particle count, size, layers, data, blending, ...) along. Those live in the
     * subclass and are therefore not copied by construction, which is what keeps the two paths from
     * drifting apart as either side gains members.
     *
     * @param other The field to copy.
     */
    public GalaxyAttributes(GalaxyAttributes other) {
        this.type = other.type;
        this.distribution = other.distribution;
        this.baseColors = other.baseColors.clone();
        this.colorNoise = other.colorNoise;
        this.baseRadius = other.baseRadius;
        this.minRadius = other.minRadius;
        this.translation = new Vector3(other.translation);
        this.scale = new Vector3(other.scale);
        this.warpStrength = other.warpStrength;
        this.heightScale = other.heightScale;
        this.heightProfile = other.heightProfile;
        this.eccentricity = other.eccentricity.clone();
        this.aspect = other.aspect;
        this.baseAngle = other.baseAngle;
        this.spiralDeltaPos = other.spiralDeltaPos.clone();
        this.numArms = other.numArms;
        this.armSigma = other.armSigma;
        this.laneWarp = other.laneWarp;
        this.noiseMask = other.noiseMask;
        this.noiseMaskFreq = other.noiseMaskFreq;
        this.noiseMaskOctaves = other.noiseMaskOctaves;
        this.laneNoiseScale = other.laneNoiseScale;
        this.laneWidthTaper = other.laneWidthTaper;
        this.filamentContrast = other.filamentContrast;
        this.filamentScale = other.filamentScale;
        this.filamentThreshold = other.filamentThreshold;
        this.intensity = other.intensity;
        this.occlusionThreshold = other.occlusionThreshold;
    }

    /**
     * Whether this field absorbs light rather than emitting it. A procedural volume routes its
     * occluding fields into the occlusive slot and the rest into the emissive one.
     *
     * @return True if this field's channel occludes.
     */
    public boolean isOcclusive() {
        return type.occludes();
    }

    /**
     * Intensity a volumetric channel starts from when its field is built out of the billboard
     * dataset of the same channel.
     * <p>
     * A sprite's intensity is an alpha, and it is tuned against how many sprites carry it and how
     * large they are. The gas cloud is drawn with thousands of 50-80 px sprites, so its alpha is
     * 0.011; the bulge is a dozen sprites, so its is near 1. A volume field has neither a count nor
     * a size - its stored alpha is a density that the raymarcher integrates along the ray - so those
     * numbers mean nothing there. Carried over as they are, the gas lands two orders of magnitude
     * below the bulge it shares the emissive slot with, quantises to a byte of zero almost
     * everywhere, and the galaxy renders with a bulge and no gas at all.
     * <p>
     * Every volumetric channel therefore starts from a density weight that says how bright it is
     * next to the others in its slot, and the volume editors are what vary it from there. The values
     * in {@code GalaxyGenerator} stay what they always were: sprite alphas.
     *
     * @param type The channel of the field.
     *
     * @return The density weight the field starts with.
     */
    public static float initialVolumeIntensity(ChannelType type) {
        return switch (type) {
            case GAS -> 1.0f;
            // The bulge shares the emissive slot with the gas, and a slot is normalised by its own
            // mean, so only the ratio between the two reaches the screen. 0.3 is the Milky Way's
            // measured one: its emissive texture is "gas at 0.7, bulge at 0.2" (see
            // VolumeCalibration.MW_EMISSIVE_MEAN_LUMA). At parity the bulge filled a fifth of the
            // radius with a saturated plateau and the gas was left as a halo around it.
            case BULGE -> 0.3f;
            // Dust is the occluding slot, and its stored peak is this intensity, so 1 puts a fully
            // opaque lane centre at the disposal of the occlusion threshold.
            case DUST -> 1.0f;
            default -> 1.0f;
        };
    }

    /** Vertical tail of the density field, in scale heights. Mirrors {@code GX_VERTICAL_TAIL}. **/
    private static final double GX_VERTICAL_TAIL = 3.0;
    /**
     * Lateral half-extent of the box a field is voxelised in, in galaxy space: the object's radius.
     * Mirrors {@code GX_BOX_RADIUS}.
     * <p>
     * Fixed rather than derived from {@link #baseRadius}. While it followed the field, a volume
     * always filled its own box, so changing {@code baseRadius} only rescaled the profile and the
     * particle and volume channels disagreed about what the number meant. Now both take
     * {@code baseRadius} for a fraction of the object radius.
     **/
    private static final double GX_BOX_RADIUS = 1.0;
    /** Dust slab thickness, as a fraction of {@link #heightScale}. Mirrors {@code GX_DUST_HEIGHT}. **/
    private static final double GX_DUST_HEIGHT = 0.5;

    /** Length in floats of one field in the packed uniform of {@code galgen.volume.comp.glsl}. **/
    public static final int FIELD_STRIDE = 38;
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
        // One spiral in the volume: the second name has no branch of its own in the field model.
        out[offset] = distribution.toVolumeDistribution().ordinal();
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
        out[offset + 32] = noiseMaskFreq;
        out[offset + 33] = (float) noiseMaskOctaves;
        out[offset + 34] = filamentContrast;
        out[offset + 35] = filamentScale;
        out[offset + 36] = filamentThreshold;
        out[offset + 37] = laneWidthTaper;
    }

    /**
     * Half-extent of the box this field is voxelised in, laterally, in galaxy space.
     * <p>
     * Mirrors {@code gxLateralHalfExtent()} in {@code assets/shader/lib/galaxy.glsl}. This is the
     * object's radius and not the field's: a field of {@code baseRadius} 0.5 is placed in the inner
     * half of it, exactly as a particle generator with the same number places its particles. When
     * this returned {@code baseRadius} the box followed the field, so every field filled its own box
     * and {@code baseRadius} changed nothing but the normalisation - which is why the volume and the
     * billboards disagreed about the meaning of the same slider.
     */
    public double volumeLateralHalfExtent() {
        return GX_BOX_RADIUS;
    }

    /**
     * Half-extent of the box this field is voxelised in, vertically, in galaxy space. Mirrors
     * {@code gxVerticalHalfExtent()}: flat distributions fill their slab, spheroids are as tall as
     * their own radius, and neither goes past the lateral box.
     **/
    public double volumeVerticalHalfExtent() {
        if (!(distribution.isFlat() || distribution.isBar())) {
            return Math.min(baseRadius, GX_BOX_RADIUS);
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
        // minRadius is absolute, so the arm runs from r = minRadius to r = baseRadius; in units of
        // baseRadius that is minRadius / baseRadius to 1, and the sweep is b * ln(baseRadius /
        // minRadius).
        double inner = MathUtilsDouble.clamp(minRadius / Math.max(baseRadius, 1e-6), 1e-4, 1.0);
        return Math.toDegrees(b * Math.log(1.0 / inner));
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

    public void setLaneWidthTaper(Double laneWidthTaper) {
        this.laneWidthTaper = MathUtilsDouble.clamp(laneWidthTaper.floatValue(), -1f, 1f);
    }

    public void setLaneWidthTaper(Long laneWidthTaper) {
        setLaneWidthTaper(laneWidthTaper.doubleValue());
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

    public void setFilamentContrast(Double filamentContrast) {
        this.filamentContrast = MathUtilsDouble.clamp(filamentContrast.floatValue(), 0f, 0.98f);
    }

    public void setFilamentContrast(Long filamentContrast) {
        setFilamentContrast(filamentContrast.doubleValue());
    }

    public void setFilamentScale(Double filamentScale) {
        this.filamentScale = MathUtilsDouble.clamp(filamentScale.floatValue(), 0f, 64f);
    }

    public void setFilamentScale(Long filamentScale) {
        setFilamentScale(filamentScale.doubleValue());
    }

    public void setFilamentThreshold(Double filamentThreshold) {
        this.filamentThreshold = MathUtilsDouble.clamp(filamentThreshold.floatValue(), 0f, 0.99f);
    }

    public void setFilamentThreshold(Long filamentThreshold) {
        setFilamentThreshold(filamentThreshold.doubleValue());
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
        /**
         * Base radius range, as a fraction of the object radius. Capped at 1 because that is the
         * edge of the volume's box and the only value a volume can use: the particle channels could
         * carry on past it, but then the same slider would mean two different things again.
         **/
        public float[] baseRadius = new float[]{0.05f, 1.6f};
        /**
         * Warp strength range: the displacement of the disk plane at the rim, as a fraction of
         * baseRadius. It is a magnitude, so it is not signed; past 0.15 the disk stops reading as a
         * warped plane and starts reading as a folded sheet.
         */
        public final float[] warpStrength = new float[]{0.0f, 0.15f};

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

        /**
         * Whether this channel is drawn by the volume raymarcher rather than as billboards.
         * <p>
         * Gas, bulge and dust are the diffuse components: they are what the analytic density field
         * in {@code galaxy.glsl} describes best, and what looks wrong when sampled into sprites.
         * Stars, HII regions and points are compact emitters, so they stay billboards.
         *
         * @return True if this channel is a volume field.
         */
        public boolean isVolumetric() {
            return this == GAS || this == BULGE || this == DUST;
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

        /**
         * @return Whether the distribution is a flat one: it has a vertical profile, a warp and a
         *         cylindrical radial profile. Includes the bar, as {@code gxIsFlat} in
         *         {@code galaxy.glsl} and {@code GalaxyDensityField#isFlat} do.
         */
        public boolean isFlat() {
            return this == DISK || this == DISK_GAUSS || this == SPIRAL || this == SPIRAL_LOG || this == BAR;
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

        /**
         * This distribution as the volume pipeline knows it.
         * <p>
         * The field model draws both spirals with the same curve - {@code gxArmFactor}, a
         * logarithmic spiral of the field's own pitch - so a volume has one spiral, and
         * {@link #SPIRAL_LOG} collapses onto {@link #SPIRAL}. The particle path keeps the two apart
         * (they differ in how a dust sprite is scaled), so this is applied only where a
         * {@link GalaxyAttributes} becomes a density field.
         *
         * @return The distribution to store in the volume pipeline.
         */
        public Distribution toVolumeDistribution() {
            return isLogarithmicSpiral() ? SPIRAL : this;
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
