/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import gaiasky.util.Settings.GraphicsQuality;

/**
 * Dimensions of the 3D texture that holds a procedurally generated galaxy volume.
 * <p>
 * The grid is never cubic. Flat distributions (disks, spirals, bars) are thin, while spherical
 * and ellipsoidal ones are isotropic, so the vertical axis follows the distribution's aspect
 * ratio. Resolution follows the galaxy's span in parsecs, and is scaled down uniformly when the
 * resulting voxel count exceeds the voxel budget of the current graphics quality.
 */
public final class VoxelGrid {
    /** Bytes per voxel: RGBA8. */
    public static final int BYTES_PER_VOXEL = 4;
    /** Lower bounds, so that a small galaxy still gets a usable texture. */
    private static final int MIN_LATERAL = 32;
    private static final int MIN_VERTICAL = 24;
    private static final int MAX_LATERAL = 1024;
    /** Dimensions are rounded up to a multiple of this, for alignment. */
    private static final int ALIGN = 4;

    /** Number of voxels along x (lateral). **/
    public final int nx;
    /** Number of voxels along y (vertical). **/
    public final int ny;
    /** Number of voxels along z (lateral). **/
    public final int nz;
    /** Lateral span of the box the grid covers, in parsecs. **/
    public final double spanPc;
    /** Vertical span of the box the grid covers, in parsecs. **/
    public final double spanYPc;

    private VoxelGrid(int nx, int ny, int nz, double spanPc, double spanYPc) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        this.spanPc = spanPc;
        this.spanYPc = spanYPc;
    }

    /**
     * Derives a grid for a galaxy of the given lateral span.
     *
     * @param spanPc  Lateral span of the galaxy, in parsecs.
     * @param aspectY Vertical-to-lateral aspect ratio of the box that encloses it.
     * @param quality Graphics quality, which sets the target voxel size and the voxel budget.
     * @return The grid.
     */
    public static VoxelGrid forSpan(double spanPc, double aspectY, GraphicsQuality quality) {
        double aspect = clamp(aspectY, 0.01, 1.0);
        double voxelPc = quality.voxelSizePc;
        long budget = quality.voxelBudget;

        int lateral = alignUp(clamp((int) Math.round(spanPc / voxelPc), MIN_LATERAL, MAX_LATERAL));
        int vertical = verticalFor(lateral, aspect);

        // Scale all axes by the same factor to keep voxels cubic, then shrink step by step until the
        // budget holds: the estimate can overshoot because of the alignment rounding.
        long total = (long) lateral * lateral * vertical;
        if (total > budget) {
            double s = Math.cbrt((double) budget / (double) total);
            lateral = Math.max(MIN_LATERAL, alignUp((int) Math.floor(lateral * s)));
            vertical = Math.max(MIN_VERTICAL, Math.min(lateral, alignUp((int) Math.floor(vertical * s))));
            total = (long) lateral * lateral * vertical;
        }
        while (total > budget) {
            // Shrink the longest axis first, so voxels stay as cubic as possible.
            if (lateral > vertical && lateral > MIN_LATERAL) {
                lateral = Math.max(MIN_LATERAL, lateral - ALIGN);
            } else if (vertical > MIN_VERTICAL) {
                vertical = Math.max(MIN_VERTICAL, vertical - ALIGN);
            } else if (lateral > MIN_LATERAL) {
                lateral = Math.max(MIN_LATERAL, lateral - ALIGN);
            } else {
                // Minimum grid reached: cannot shrink further.
                break;
            }
            total = (long) lateral * lateral * vertical;
        }
        return new VoxelGrid(lateral, vertical, lateral, spanPc, spanPc * aspect);
    }

    private static int verticalFor(int lateral, double aspect) {
        return alignUp(clamp((int) Math.round(lateral * aspect), MIN_VERTICAL, lateral));
    }

    private static int alignUp(int v) {
        return ((v + ALIGN - 1) / ALIGN) * ALIGN;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(v, max));
    }

    /** @return Total number of voxels. **/
    public long voxelCount() {
        return (long) nx * ny * nz;
    }

    /** @return Size of an RGBA8 texture with these dimensions, in bytes. **/
    public long bytes() {
        return voxelCount() * BYTES_PER_VOXEL;
    }

    /** @return Lateral voxel size, in parsecs. **/
    public double voxelSizePc() {
        return spanPc / nx;
    }

    @Override
    public String toString() {
        return String.format("%dx%dx%d (%.1f Mvox, %.1f MB, %.1f pc/voxel)",
                             nx, ny, nz,
                             voxelCount() / 1e6,
                             bytes() / 1048576.0,
                             voxelSizePc());
    }
}
