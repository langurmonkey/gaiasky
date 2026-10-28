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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.GaiaSky;
import gaiasky.render.gdx.shader.ComputeShaderProgram;
import gaiasky.render.gdx.shader.loader.ShaderTemplatingLoader;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Logger;
import gaiasky.util.SysUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL43;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;

/**
 * Builds the volume texture of a procedurally generated galaxy: gas and bulge in the emissive slot,
 * dust in the occlusive one.
 * <p>
 * The heavy lifting is in {@code assets/shader/compute/galtype.comp.glsl}, which evaluates the
 * analytic density field of {@code assets/shader/lib/galaxy.glsl} once per voxel and stores the
 * result. This class is the orchestration around it: allocate the texture, zero the statistics
 * buffer, run the reduce pass, run the write pass against the statistics the first one produced,
 * and hand back a {@link VolumeTexture} shaped like any other volume the renderer knows.
 * <p>
 * Two dispatches per slot. Both are whole-grid, which is what makes this simple; if a large grid
 * turns out to be a visible hitch on integrated GPUs, {@code u_gridOffset} in the shader already
 * lets a slot be split into z-slabs across frames without touching the field.
 * <p>
 * Not available without compute shaders (GL 4.3+). {@link #isSupported()} says so, and
 * {@link #generate} returns null rather than throwing, so the caller can fall back.
 */
public class GalaxyVolumeGenerator implements Disposable {
    private static final Logger.Log logger = Logger.getLogger(GalaxyVolumeGenerator.class);

    private static final String SHADER_FILE = "shader/compute/galtype.comp.glsl";
    /** Image unit the shader writes to ({@code layout(binding = 0) uniform image3D u_volume}). **/
    private static final int IMAGE_UNIT = 0;
    /** SSBO binding of the statistics ({@code layout(std430, binding = 1) buffer Stats}). **/
    private static final int STATS_BINDING = 1;
    /** Number of uints in the statistics buffer: two peaks and two sums. **/
    private static final int STATS_UINTS = 4;
    /** Must match {@code GX_SUM_SCALE} in the compute shader. **/
    private static final float SUM_SCALE = 32.0f;

    /** Pass ids, matching {@code PASS_REDUCE} and {@code PASS_WRITE} in the compute shader. **/
    private static final int PASS_REDUCE = 0;
    private static final int PASS_WRITE = 1;

    private static GalaxyVolumeGenerator instance;

    private ComputeShaderProgram program;
    private int statsBuffer = -1;
    /** Set when the program cannot be built, so that a broken driver costs one attempt and not one per galaxy. **/
    private boolean unavailable;

    private GalaxyVolumeGenerator() {
    }

    public static synchronized GalaxyVolumeGenerator instance() {
        if (instance == null) {
            instance = new GalaxyVolumeGenerator();
        }
        return instance;
    }

    /** @return Whether volume generation can run at all in this runtime. **/
    public static boolean isSupported() {
        return GaiaSky.settings().runtime.compute && Gdx.gl30 != null;
    }

    /**
     * Generates the volume texture of one slot.
     *
     * @param datasets              The datasets that go into the slot, in slot order. At most
     *                              {@link BillboardDataset#MAX_FIELDS} are used.
     * @param grid                  Grid dimensions, normally from
     *                              {@link BillboardDataset#volumeGrid(List, double, gaiasky.util.Settings.GraphicsQuality)}.
     * @param targetMeanRayIntegral Mean luma the slot should have at unit intensity. See
     *                              {@link VolumeCalibration}; zero skips the calibration.
     *
     * @return The volume, or null if generation is not possible (no compute support, shader
     * compilation failure, degenerate grid).
     */
    public VolumeTexture generate(List<BillboardDataset> datasets, VoxelGrid grid, float targetMeanRayIntegral) {
        if (!isSupported() || unavailable || datasets == null || datasets.isEmpty() || grid == null) {
            return null;
        }
        if (!ensureProgram()) {
            return null;
        }

        int count = Math.min(datasets.size(), BillboardDataset.MAX_FIELDS);
        float[] fields = packFields(datasets, count);

        // The box has to hold every field in the slot, not just the first: a bulge can be taller than
        // the gas disk it sits in.
        double lateralHalf = 0.0;
        double verticalHalf = 0.0;
        for (int i = 0; i < count; i++) {
            lateralHalf = Math.max(lateralHalf, datasets.get(i).volumeLateralHalfExtent());
            verticalHalf = Math.max(verticalHalf, datasets.get(i).volumeVerticalHalfExtent());
        }
        if (lateralHalf <= 0.0 || verticalHalf <= 0.0) {
            logger.error("Cannot generate volume: empty extent");
            return null;
        }

        Texture3D texture = null;
        try {
            texture = new Texture3D(new EmptyTexture3DData(grid.nx, grid.ny, grid.nz,
                                                           VolumeType.RGBA.glFormat,
                                                           VolumeType.RGBA.glInternalFormat,
                                                           VolumeType.RGBA.glType));
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge,
                            Texture.TextureWrap.ClampToEdge);
            // Linear: a raymarcher samples between voxels and a nearest filter would show the slabs.
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

            ensureStatsBuffer();
            resetStats();

            GL43.glBindImageTexture(IMAGE_UNIT, texture.getTextureObjectHandle(), 0, true, 0,
                                    GL43.GL_WRITE_ONLY, VolumeType.RGBA.glInternalFormat);
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, STATS_BINDING, statsBuffer);

            for (int pass = PASS_REDUCE; pass <= PASS_WRITE; pass++) {
                program.begin();
                program.setUniform("u_pass", pass);
                program.setUniform("u_fieldCount", count);
                program.setUniform("u_fields[0]", fields);
                program.setUniform("u_gridOrigin", gridOrigin(lateralHalf, verticalHalf, grid));
                program.setUniform("u_gridCell", gridCell(lateralHalf, verticalHalf, grid));
                program.setUniform("u_gridDims[0]", new int[]{grid.nx, grid.ny, grid.nz});
                program.setUniform("u_gridOffset[0]", new int[]{0, 0, 0});
                program.setUniform("u_targetMeanRayIntegral", targetMeanRayIntegral);
                program.setUniform("u_gain", 1f);
                program.end3D(grid.nx, grid.ny, grid.nz);
            }

            GL43.glBindImageTexture(IMAGE_UNIT, 0, 0, true, 0, GL43.GL_WRITE_ONLY, VolumeType.RGBA.glInternalFormat);
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, STATS_BINDING, 0);

            SysUtils.checkForOpenGLErrors("GalaxyVolumeGenerator.generate(" + grid + ")");

            // The box is in units of the object's size, with the largest axis spanning 1 - the
            // convention every volume the loader produces follows, and the one the raymarcher scales
            // by u_bodySize.
            float[] box = boxFor(lateralHalf, verticalHalf);
            return new VolumeTexture(texture, grid.nx, grid.ny, grid.nz, VolumeType.RGBA,
                                     new Vector3(box[0], box[1], box[2]),
                                     new Vector3(box[3], box[4], box[5]),
                                     null);
        } catch (Exception e) {
            logger.error(e, "Error generating galaxy volume");
            if (texture != null) {
                texture.dispose();
            }
            return null;
        } finally {
            GL43.glBindImageTexture(IMAGE_UNIT, 0, 0, true, 0, GL43.GL_WRITE_ONLY, VolumeType.RGBA.glInternalFormat);
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, STATS_BINDING, 0);
        }
    }

    /**
     * Reads back the statistics of the last generation, for logging and calibration checks.
     *
     * @param grid The grid that was generated.
     *
     * @return {@code {peakLight, peakUnit, meanLight, meanUnit}}, or null if there is nothing to read.
     * The peaks are the largest channel of the summed light, the means are luma averages over all
     * voxels - the quantity {@link VolumeCalibration} is stated in.
     */
    public float[] readStats(VoxelGrid grid) {
        if (statsBuffer < 0 || grid == null) {
            return null;
        }
        IntBuffer stats = BufferUtils.createIntBuffer(STATS_UINTS);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, statsBuffer);
        GL43.glGetBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, stats);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        float voxelCount = (float) grid.voxelCount() * SUM_SCALE;
        return new float[]{
                Float.intBitsToFloat(stats.get(0)),
                Float.intBitsToFloat(stats.get(1)),
                stats.get(2) / voxelCount,
                stats.get(3) / voxelCount
        };
    }

    /** Packs the slot's datasets into the flat uniform the shader unpacks. **/
    public static float[] packFields(List<BillboardDataset> datasets, int count) {
        float[] fields = new float[BillboardDataset.MAX_FIELDS * BillboardDataset.FIELD_STRIDE];
        for (int i = 0; i < count; i++) {
            datasets.get(i).packFields(fields, i * BillboardDataset.FIELD_STRIDE);
        }
        return fields;
    }

    /**
     * Centre of voxel (0, 0, 0) in galaxy space.
     * <p>
     * The box is centred on the origin, so this is the near corner plus half a cell - the shader
     * computes voxel centres as {@code origin + voxel * cell}, which is what keeps the field's
     * symmetry (and the spiral's winding) aligned with the texture's.
     */
    public static float[] gridOrigin(double lateralHalf, double verticalHalf, VoxelGrid grid) {
        float[] cell = gridCell(lateralHalf, verticalHalf, grid);
        return new float[]{
                (float) -lateralHalf + 0.5f * cell[0],
                (float) -verticalHalf + 0.5f * cell[1],
                (float) -lateralHalf + 0.5f * cell[2]
        };
    }

    /** Galaxy-space size of one voxel on each axis. **/
    public static float[] gridCell(double lateralHalf, double verticalHalf, VoxelGrid grid) {
        return new float[]{
                (float) (2.0 * lateralHalf / grid.nx),
                (float) (2.0 * verticalHalf / grid.ny),
                (float) (2.0 * lateralHalf / grid.nz)
        };
    }

    /**
     * The volume's box in object-size units: {@code {-x,-y,-z, +x,+y,+z}}, centred, with the lateral
     * half-extent at exactly 0.5 so that the largest axis spans the object's size. The vertical one
     * follows the fields' aspect, so a flat galaxy gets a flat box.
     */
    public static float[] boxFor(double lateralHalf, double verticalHalf) {
        float hy = (float) (0.5 * verticalHalf / Math.max(lateralHalf, 1e-9));
        return new float[]{-0.5f, -hy, -0.5f, 0.5f, hy, 0.5f};
    }

    private boolean ensureProgram() {
        if (program != null && program.isCompiled()) {
            return true;
        }
        try {
            var file = Gdx.files.internal(SHADER_FILE);
            String code = ShaderTemplatingLoader.load(file);
            program = new ComputeShaderProgram("galaxy-volume", SHADER_FILE, code);
            if (!program.isCompiled()) {
                logger.error("Galaxy volume compute shader did not compile: " + program.getLog());
                unavailable = true;
                return false;
            }
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error(e, "Cannot load the galaxy volume compute shader");
            unavailable = true;
            return false;
        }
    }

    private void ensureStatsBuffer() {
        if (statsBuffer < 0) {
            statsBuffer = GL43.glGenBuffers();
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, statsBuffer);
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, (long) STATS_UINTS * Integer.BYTES, GL43.GL_DYNAMIC_COPY);
            GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        }
    }

    /** Zeroes the statistics, so that the reduce pass starts from nothing. **/
    private void resetStats() {
        IntBuffer zeros = BufferUtils.createIntBuffer(STATS_UINTS);
        zeros.put(new int[STATS_UINTS]).flip();
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, statsBuffer);
        GL43.glBufferSubData(GL43.GL_SHADER_STORAGE_BUFFER, 0, zeros);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
    }

    @Override
    public void dispose() {
        if (statsBuffer >= 0) {
            GL43.glDeleteBuffers(statsBuffer);
            statsBuffer = -1;
        }
        if (program != null) {
            program.dispose();
            program = null;
        }
    }
}
