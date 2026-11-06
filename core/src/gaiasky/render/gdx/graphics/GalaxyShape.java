/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

/**
 * One volume field, unpacked out of the flat array the compute shader reads.
 * <p>
 * This is the Java counterpart of {@code struct GalaxyShape} in
 * {@code assets/shader/lib/galaxy.glsl}: the member order and the offsets are the ones of
 * {@link gaiasky.scene.record.GalaxyAttributes#packFields}, which is the single writer of the packed
 * layout that both readers (the shader's {@code gxUnpack} and {@link #from}) follow.
 * <p>
 * Unpacking once per generation rather than once per voxel is the one structural difference from the
 * shader: the shader pays for it inside the loop because it has no per-invocation storage to keep it
 * in, while here it is a few dozen floats read once.
 */
public final class GalaxyShape {
    /** Distribution ids. Must match the {@code GX_*} constants and the {@code Distribution} ordinals. **/
    public static final int SPHERE = 0, DISK = 1, SPIRAL = 2, SPIRAL_LOG = 3, BAR = 4, ELLIPSOID = 5,
            DISK_GAUSS = 6, SPHERE_GAUSS = 7, CONE = 8, IRREGULAR = 9;
    /** {@code SPIRAL_LOG} is reserved: the volume packs it as {@code SPIRAL} ({@code Distribution#toVolumeDistribution}). **/
    /** Height profile ids. Must match the {@code GX_HEIGHT_*} constants and the ordinals. **/
    public static final int HEIGHT_CONSTANT = 0, HEIGHT_LINEAR_INC = 1, HEIGHT_LINEAR_DEC = 2,
            HEIGHT_SMOOTH_INC = 3, HEIGHT_SMOOTH_DEC = 4;
    /** Channel ids. Must match the {@code GX_CHANNEL_*} constants and the {@code ChannelType} ordinals. **/
    public static final int CHANNEL_DUST = 0, CHANNEL_BULGE = 1, CHANNEL_STAR = 2, CHANNEL_GAS = 3,
            CHANNEL_HII = 4, CHANNEL_POINT = 5;

    public final int distribution;
    public final float baseRadius;
    public final float minRadius;
    public final int numArms;
    public final float armSigma;
    public final float baseAngle;
    public final float eccentricityX;
    public final float eccentricityY;
    public final float aspect;
    public final float warpStrength;
    public final float heightScale;
    public final int heightProfile;
    public final float colorNoise;
    /** The field's intensity, the weight of its contribution to the slot. **/
    public final float weight;
    public final int channel;
    public final float laneWarp;
    public final float noiseMask;
    public final float noiseMaskFreq;
    public final int noiseMaskOctaves;
    public final float laneNoiseScale;
    /** Depth of the substructure inside a dust lane, in [0,1]. **/
    public final float filamentContrast;
    /** Frequency of that substructure across the lane, in cycles per baseRadius. **/
    public final float filamentScale;
    /** Level of the field above which a point is inside a strand. **/
    public final float filamentThreshold;
    /** Four colour stops, core first, three components each. **/
    public final float[] baseColors = new float[12];

    private GalaxyShape(float[] d, int o) {
        distribution = Math.round(d[o + 0]);
        baseRadius = d[o + 1];
        minRadius = d[o + 2];
        numArms = Math.round(d[o + 3]);
        armSigma = d[o + 4];
        baseAngle = d[o + 5];
        eccentricityX = d[o + 6];
        eccentricityY = d[o + 7];
        // [8] and [9] are the density-wave displacement: declared a no-op by the field model.
        aspect = d[o + 10];
        warpStrength = d[o + 11];
        heightScale = d[o + 12];
        heightProfile = Math.round(d[o + 13]);
        colorNoise = d[o + 14];
        weight = d[o + 15];
        System.arraycopy(d, o + 16, baseColors, 0, 12);
        channel = Math.round(d[o + 28]);
        laneWarp = d[o + 29];
        noiseMask = d[o + 30];
        laneNoiseScale = d[o + 31];
        noiseMaskFreq = d[o + 32];
        noiseMaskOctaves = Math.round(d[o + 33]);
        filamentContrast = d[o + 34];
        filamentScale = d[o + 35];
        filamentThreshold = d[o + 36];
    }

    /**
     * Reads one shape out of a packed array.
     *
     * @param packed The packed fields, as produced by {@link VolumeLayout#packFields}.
     * @param offset Offset of the shape in floats, normally {@code i * FIELD_STRIDE}.
     *
     * @return The shape.
     */
    public static GalaxyShape from(float[] packed,
                                   int offset) {
        return new GalaxyShape(packed, offset);
    }

    /** @return Whether this field is dust. Only dust is treated differently by the field. **/
    public boolean isDust() {
        return channel == CHANNEL_DUST;
    }

    /** @return The vertical scale height, thinner for dust. Mirrors {@code gxHeightScale()}. **/
    public float effectiveHeightScale() {
        return isDust() ? GalaxyDensityField.DUST_HEIGHT * heightScale : heightScale;
    }
}
