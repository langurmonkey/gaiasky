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
 * A backend that turns a slot's fields into a voxel texture: one that dispatches the compute shader
 * ({@link GalaxyVolumeGenerator}) and one that evaluates the same field in Java
 * ({@link CPUGalaxyVolumeGenerator}), for hardware without compute shaders.
 * <p>
 * The two differ in how they produce the voxels and in how precisely they reproduce the noise, not
 * in what they store: both write the same RGBA8 layout, both calibrate against
 * {@link VolumeCalibration}, and both hand back a {@link VolumeTexture} the material can bind. What
 * the volume looks like is therefore a property of the fields, not of the backend.
 */
public interface VolumeGenerator {

    /**
     * @return The backend this runtime will use: the compute one when it is available, the CPU one
     *         otherwise. Null is not possible - the CPU voxeliser needs nothing but a GL context.
     */
    static VolumeGenerator forRuntime() {
        return GalaxyVolumeGenerator.isSupported()
                ? GalaxyVolumeGenerator.instance()
                : CPUGalaxyVolumeGenerator.instance();
    }

    /** @return Short name of the backend, for the logs. **/
    String name();

    /** @return Whether this backend can run in the current runtime. **/
    boolean isAvailable();

    /**
     * Generates the volume texture of one slot, refilling {@code existing} when it fits the grid.
     *
     * @param fields   The fields that go into the slot, in slot order.
     * @param grid     Grid dimensions.
     * @param existing The slot's current texture, or null to always allocate a new one.
     *
     * @return The volume, which may wrap {@code existing}, or null if generation is not possible.
     */
    VolumeTexture generate(List<? extends GalaxyAttributes> fields,
                           VoxelGrid grid,
                           VolumeTexture existing);

    /**
     * Reads back the statistics of the last generation, for logging and calibration checks. See
     * {@link GalaxyVolumeGenerator#readStats(VoxelGrid)} for the meaning of each entry.
     *
     * @param grid The grid that was generated.
     *
     * @return The statistics, or null if there is nothing to read.
     */
    float[] readStats(VoxelGrid grid);

    /**
     * Reduces a grid to what this backend can afford. The compute path fills any grid the quality
     * settings ask for; the CPU one evaluates the field on the processor, so it is given a smaller
     * one.
     *
     * @param grid The grid the quality settings derive.
     *
     * @return The grid to generate.
     */
    default VoxelGrid constrainGrid(VoxelGrid grid) {
        return grid;
    }
}
