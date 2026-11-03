/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.glutils.CustomTexture3DData;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import gaiasky.scene.record.GalaxyAttributes;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Builds the volume texture of a galaxy slot by evaluating the density field on the processor, for
 * hardware without compute shaders.
 * <p>
 * It reproduces what {@link GalaxyVolumeGenerator} does with two dispatches of
 * {@code galgen.volume.comp.glsl} - evaluate the field once per voxel to reduce six statistics, then
 * again to write them normalised - and it uses the same {@link VolumeCalibration} targets, the same
 * packing ({@link VolumeLayout}) and the same box and grid geometry, so a slot generated here is
 * interchangeable with one generated there: same format, same bounds, same brightness convention.
 * The field itself is the port in {@link GalaxyDensityField}, which is not bit-identical to the
 * shader (see its class doc) - the noise is a different realisation, the structure and the
 * calibration are the same.
 * <p>
 * It needs a GL context to allocate and fill the texture, but not a compute-capable one: 3D textures
 * are GL 3.0, and the raymarch side of the volume pipeline already works there.
 * <p>
 * <strong>Cost.</strong> Every voxel costs one field evaluation per field in the slot, and every
 * evaluation costs tens of noise lookups: this is the reason the compute shader exists. The grid is
 * therefore capped at {@link #VOXEL_BUDGET} voxels - see {@link #constrainGrid} - and the work runs
 * on the calling thread, which during generation is the render thread. At the budget it is a few
 * hundred milliseconds per slot, once, and the result is cached in the slot's texture.
 */
public class CPUGalaxyVolumeGenerator implements VolumeGenerator {
    private static final Logger.Log logger = Logger.getLogger(CPUGalaxyVolumeGenerator.class);

    /**
     * Cap on the number of voxels of one slot, which is what bounds the cost of a generation. A
     * quarter of a million voxels is a lateral resolution of about 64 at the usual aspect, which is
     * coarse but recognisable; the compute path is the one that goes up to the full quality grid.
     */
    public static final long VOXEL_BUDGET = 262_144L;

    /** Statistics entries: six scalars followed by one per field. Mirrors {@code STATS_UINTS} there. **/
    private static final int STATS_UINTS = 6 + VolumeLayout.MAX_FIELDS;

    private static CPUGalaxyVolumeGenerator instance;

    /** Statistics of the last generation, in the layout of {@link GalaxyVolumeGenerator#readStats}. **/
    private float[] lastStats;

    private CPUGalaxyVolumeGenerator() {
    }

    public static synchronized CPUGalaxyVolumeGenerator instance() {
        if (instance == null) {
            instance = new CPUGalaxyVolumeGenerator();
        }
        return instance;
    }

    @Override
    public String name() {
        return "CPU";
    }

    @Override
    public boolean isAvailable() {
        // The field is pure arithmetic, but binding the result needs a 3D texture.
        return Gdx.gl30 != null;
    }

    @Override
    public VoxelGrid constrainGrid(VoxelGrid grid) {
        return grid == null ? null : grid.shrinkTo(VOXEL_BUDGET);
    }

    @Override
    public VolumeTexture generate(List<? extends GalaxyAttributes> fields,
                                  VoxelGrid grid,
                                  VolumeTexture existing) {
        if (!isAvailable() || fields == null || fields.isEmpty() || grid == null) {
            return null;
        }
        try {
            float[] stats = new float[STATS_UINTS];
            float[] extents = new float[2];
            long start = System.nanoTime();
            ByteBuffer voxels = voxelise(fields, grid, stats, extents);
            long voxelised = System.nanoTime();
            VolumeTexture result = upload(voxels, grid, extents[0], extents[1], existing);
            if (result == null) {
                return null;
            }
            lastStats = stats;
            logger.info(String.format(java.util.Locale.ROOT,
                                      "Generated a %s volume on the CPU in %.1f ms (field %.1f ms, upload %.1f ms)",
                                      grid, (System.nanoTime() - start) / 1.0e6,
                                      (voxelised - start) / 1.0e6,
                                      (System.nanoTime() - voxelised) / 1.0e6));
            return result;
        } catch (Exception e) {
            logger.error(e, "Error generating galaxy volume on the CPU");
            return null;
        }
    }

    @Override
    public float[] readStats(VoxelGrid grid) {
        return lastStats;
    }

    /**
     * Evaluates the slot's fields into raw RGBA8 voxels, x fastest, then y, then z - the order GL
     * reads a 3D texture in, and the order the compute shader writes.
     * <p>
     * This is the whole field work with no GL in it, which is what makes it testable without a
     * context.
     *
     * @param fields  The fields that go into the slot, in slot order.
     * @param grid    The grid to fill.
     * @param statsOut Filled with the statistics of the field, in the layout of
     *                 {@link GalaxyVolumeGenerator#readStats}; may be null.
     *
     * @return The voxels: {@code nx * ny * nz * 4} bytes, ready to upload.
     */
    public static ByteBuffer voxelise(List<? extends GalaxyAttributes> fields,
                                      VoxelGrid grid,
                                      float[] statsOut) {
        return voxelise(fields, grid, statsOut, null);
    }

    /**
     * Same as {@link #voxelise(List, VoxelGrid, float[])}, also reporting the half-extents of the
     * box it sampled, which the volume texture needs and which are not the grid's own spans (the
     * grid is sized from the galaxy, the box from the fields).
     *
     * @param extentsOut Filled with {@code {lateralHalf, verticalHalf}}, in galaxy space; may be null.
     */
    public static ByteBuffer voxelise(List<? extends GalaxyAttributes> fields,
                                      VoxelGrid grid,
                                      float[] statsOut,
                                      float[] extentsOut) {
        int count = VolumeLayout.usableCount(fields);
        if (count == 0) {
            return BufferUtils.newByteBuffer(0);
        }
        VolumeLayout.warnOnPitch(fields, count);
        float[] packed = VolumeLayout.packFields(fields, count);
        GalaxyShape[] shapes = new GalaxyShape[count];
        for (int i = 0; i < count; i++) {
            shapes[i] = GalaxyShape.from(packed, i * VolumeLayout.FIELD_STRIDE);
        }
        boolean occlusive = VolumeLayout.slotIsOcclusive(fields, count);

        // The box has to hold every field in the slot: a bulge can be taller than its gas disk.
        double lateralHalf = 0.0;
        double verticalHalf = 0.0;
        for (int i = 0; i < count; i++) {
            lateralHalf = Math.max(lateralHalf, fields.get(i).volumeLateralHalfExtent());
            verticalHalf = Math.max(verticalHalf, fields.get(i).volumeVerticalHalfExtent());
        }
        if (lateralHalf <= 0.0 || verticalHalf <= 0.0) {
            logger.error("Cannot generate volume: empty extent");
            return BufferUtils.newByteBuffer(0);
        }

        if (extentsOut != null && extentsOut.length >= 2) {
            extentsOut[0] = (float) lateralHalf;
            extentsOut[1] = (float) verticalHalf;
        }

        float[] origin = VolumeLayout.gridOrigin(lateralHalf, verticalHalf, grid);
        float[] cell = VolumeLayout.gridCell(lateralHalf, verticalHalf, grid);

        var sum = new GalaxyDensityField.FieldSum();
        var one = new GalaxyDensityField.Sample();

        // Pass 1: reduce. Nothing is written, only measured.
        double sumLight = 0.0;
        double sumUnit = 0.0;
        float peakLight = 0f;
        float peakUnit = 0f;
        float peakDensity = 0f;
        float peakUnitDensity = 0f;
        float[] fieldPeaks = new float[count];
        for (int z = 0; z < grid.nz; z++) {
            float pz = origin[2] + z * cell[2];
            for (int y = 0; y < grid.ny; y++) {
                float py = origin[1] + y * cell[1];
                for (int x = 0; x < grid.nx; x++) {
                    float px = origin[0] + x * cell[0];
                    GalaxyDensityField.sampleFields(shapes, count, px, py, pz, cell[0], sum, one);
                    float bl = GalaxyDensityField.brightness(sum.lightR, sum.lightG, sum.lightB);
                    float bu = GalaxyDensityField.brightness(sum.unitR, sum.unitG, sum.unitB);
                    peakLight = Math.max(peakLight, bl);
                    peakUnit = Math.max(peakUnit, bu);
                    peakDensity = Math.max(peakDensity, sum.density);
                    peakUnitDensity = Math.max(peakUnitDensity, sum.unitDensity);
                    sumLight += GalaxyDensityField.luma(sum.lightR, sum.lightG, sum.lightB);
                    sumUnit += GalaxyDensityField.luma(sum.unitR, sum.unitG, sum.unitB);
                    for (int i = 0; i < count; i++) {
                        fieldPeaks[i] = Math.max(fieldPeaks[i], sum.fieldUnitDensity[i]);
                    }
                }
            }
        }

        float voxels = (float) grid.voxelCount();
        float meanUnit = (float) (sumUnit / voxels);
        float meanLight = (float) (sumLight / voxels);
        if (statsOut != null) {
            statsOut[0] = peakLight;
            statsOut[1] = peakUnit;
            statsOut[2] = meanLight;
            statsOut[3] = meanUnit;
            statsOut[4] = peakDensity;
            statsOut[5] = peakUnitDensity;
            for (int i = 0; i < VolumeLayout.MAX_FIELDS; i++) {
                statsOut[6 + i] = i < count ? fieldPeaks[i] : 0f;
            }
        }

        float targetMeanRayIntegral = occlusive ? 0f : (float) VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA;
        float targetPeak = occlusive ? (float) VolumeCalibration.OCCLUSIVE_TARGET_PEAK : 0f;
        float reference = peakUnit;

        // Pass 2: write, with the normalisation the reduce pass measured.
        ByteBuffer bytes = BufferUtils.newByteBuffer(grid.nx * grid.ny * grid.nz * 4);
        for (int z = 0; z < grid.nz; z++) {
            float pz = origin[2] + z * cell[2];
            for (int y = 0; y < grid.ny; y++) {
                float py = origin[1] + y * cell[1];
                for (int x = 0; x < grid.nx; x++) {
                    float px = origin[0] + x * cell[0];
                    GalaxyDensityField.sampleFields(shapes, count, px, py, pz, cell[0], sum, one);
                    float brightLight = GalaxyDensityField.brightness(sum.lightR, sum.lightG, sum.lightB);
                    float chromaScale = 1f / Math.max(brightLight, 1.0e-12f);
                    float cr = sum.lightR * chromaScale;
                    float cg = sum.lightG * chromaScale;
                    float cb = sum.lightB * chromaScale;

                    float alpha;
                    if (occlusive) {
                        // Each field is normalised by the peak of its own unit density, so that an
                        // intensity stays a linear knob whatever the shape of the others.
                        alpha = 0f;
                        for (int i = 0; i < count; i++) {
                            if (targetPeak > 0f && fieldPeaks[i] > 0f) {
                                alpha += sum.fieldDensity[i] * targetPeak / fieldPeaks[i];
                            } else {
                                alpha += sum.fieldDensity[i];
                            }
                        }
                    } else {
                        // Normalised by the peak at unit intensity, and calibrated so that the mean
                        // ray integral is the target one; see galgen.volume.comp.glsl.
                        alpha = brightLight;
                        if (targetMeanRayIntegral > 0f && meanUnit > 0f && reference > 0f) {
                            alpha *= targetMeanRayIntegral * reference / meanUnit;
                        }
                        alpha /= Math.max(reference, 1.0e-12f);
                    }

                    bytes.put(toByte(cr));
                    bytes.put(toByte(cg));
                    bytes.put(toByte(cb));
                    bytes.put(toByte(alpha));
                }
            }
        }
        bytes.flip();
        return bytes;
    }

    /** Clamps to [0,1] and quantises to 8 bits the way {@code imageStore} on an rgba8 image does. **/
    private static byte toByte(float v) {
        int i = Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
        return (byte) i;
    }

    /**
     * Hands the voxels to GL: refills the slot's texture when it fits the grid, and allocates a new
     * one otherwise. Must run on the render thread.
     *
     * @param voxels      The bytes {@link #voxelise} produced, in grid order.
     * @param grid        The grid they describe.
     * @param lateralHalf Half-extent of the sampled box along x and z, in galaxy space.
     * @param verticalHalf Half-extent of the sampled box along y, in galaxy space.
     * @param existing    The slot's current texture, or null.
     *
     * @return The volume, or null if the texture could not be built.
     */
    private VolumeTexture upload(ByteBuffer voxels,
                                 VoxelGrid grid,
                                 float lateralHalf,
                                 float verticalHalf,
                                 VolumeTexture existing) {
        boolean reused = VolumeLayout.canReuse(existing, grid);
        Texture3D texture = null;
        boolean created = false;
        try {
            if (reused) {
                texture = existing.texture();
                // glTexSubImage3D reads through the currently bound texture.
                Gdx.gl.glBindTexture(GL30.GL_TEXTURE_3D, texture.getTextureObjectHandle());
                Gdx.gl30.glTexSubImage3D(GL30.GL_TEXTURE_3D, 0, 0, 0, 0,
                                         grid.nx, grid.ny, grid.nz,
                                         VolumeType.RGBA.glFormat, VolumeType.RGBA.glType, voxels);
            } else {
                var data = new CustomTexture3DData(grid.nx, grid.ny, grid.nz, 0,
                                                   VolumeType.RGBA.glFormat,
                                                   VolumeType.RGBA.glInternalFormat,
                                                   VolumeType.RGBA.glType);
                data.getPixels().put(voxels);
                data.getPixels().flip();
                texture = new Texture3D(data);
                created = true;
                texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge,
                                Texture.TextureWrap.ClampToEdge);
                // Linear: a raymarcher samples between voxels and a nearest filter would show the slabs.
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            }

            SysUtils.checkForOpenGLErrors("CPUGalaxyVolumeGenerator.generate(" + grid + ")"
                                                  + (reused ? ", reusing the slot's texture" : ""));
        } catch (RuntimeException e) {
            // A reused texture is owned by the slot and may still be bound somewhere, so it is left
            // alone; one allocated here has never been handed out and would leak.
            if (created && texture != null) {
                texture.dispose();
            }
            throw e;
        }

        float[] box = VolumeLayout.boxFor(lateralHalf, verticalHalf);
        return new VolumeTexture(texture, grid.nx, grid.ny, grid.nz, VolumeType.RGBA,
                                 new Vector3(box[0], box[1], box[2]),
                                 new Vector3(box[3], box[4], box[5]),
                                 null);
    }
}
