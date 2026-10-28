/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Settings.GraphicsQuality;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * {@link VoxelGrid#forSpan(double, double, GraphicsQuality)} sizes the volume texture of a
 * procedural galaxy. The invariants pinned here are the ones the generator and the volume
 * renderer rely on: flat galaxies get a thin grid, the budget is never exceeded, and the grid
 * grows with quality but stays bounded.
 */
public class VoxelGridTest {

    private static final double ANDROMEDA_SPAN_PC = 40_000;
    private static final double FLAT_ASPECT = 0.08;

    @Test
    public void flatGalaxiesAreThin() {
        var grid = VoxelGrid.forSpan(ANDROMEDA_SPAN_PC, FLAT_ASPECT, GraphicsQuality.NORMAL);
        Assert.assertEquals(grid.nx, grid.nz);
        Assert.assertTrue(grid.ny < grid.nx);
        // Voxels are cubic-ish: the vertical axis covers the same fraction of the aspect.
        Assert.assertEquals(FLAT_ASPECT, grid.spanYPc / grid.spanPc, 1e-9);
    }

    @Test
    public void sphericalGalaxiesAreIsotropic() {
        // Below the budget, a sphere maps to a cube.
        var grid = VoxelGrid.forSpan(10_000, 1.0, GraphicsQuality.ULTRA);
        Assert.assertEquals(grid.nx, grid.ny);
        Assert.assertEquals(grid.nx, grid.nz);
        // When the budget clamps the grid, the axes stay within one alignment step.
        var clamped = VoxelGrid.forSpan(ANDROMEDA_SPAN_PC, 1.0, GraphicsQuality.NORMAL);
        Assert.assertTrue(Math.abs(clamped.nx - clamped.ny) <= 4);
    }

    @Test
    public void budgetIsNeverExceeded() {
        double[] spans = {1_000, 10_000, 40_000, 200_000, 1_000_000};
        for (var quality : GraphicsQuality.values()) {
            for (double span : spans) {
                for (double aspect : new double[]{0.02, 0.08, 0.5, 1.0}) {
                    var grid = VoxelGrid.forSpan(span, aspect, quality);
                    Assert.assertTrue("budget exceeded: " + grid + " at " + quality,
                                      grid.voxelCount() <= quality.voxelBudget);
                }
            }
        }
    }

    @Test
    public void qualityIncreasesResolution() {
        var low = VoxelGrid.forSpan(ANDROMEDA_SPAN_PC, FLAT_ASPECT, GraphicsQuality.LOW);
        var normal = VoxelGrid.forSpan(ANDROMEDA_SPAN_PC, FLAT_ASPECT, GraphicsQuality.NORMAL);
        var high = VoxelGrid.forSpan(ANDROMEDA_SPAN_PC, FLAT_ASPECT, GraphicsQuality.HIGH);
        Assert.assertTrue(low.voxelSizePc() > normal.voxelSizePc());
        Assert.assertTrue(normal.voxelSizePc() > high.voxelSizePc());
    }

    @Test
    public void smallGalaxiesStaySmall() {
        // A dwarf galaxy must not be oversampled to the budget: resolution follows the span.
        var dwarf = VoxelGrid.forSpan(2_000, FLAT_ASPECT, GraphicsQuality.ULTRA);
        var giant = VoxelGrid.forSpan(200_000, FLAT_ASPECT, GraphicsQuality.ULTRA);
        Assert.assertTrue(dwarf.voxelCount() < giant.voxelCount());
        Assert.assertTrue(dwarf.nx >= 32);
    }

    @Test
    public void slotGridUnionsItsFields() {
        // The emissive slot holds gas and bulge. Their boxes differ by an order of magnitude in the
        // vertical, and the union has to contain both or the bulge is cut in half.
        var gas = new BillboardDataset();
        gas.distribution = BillboardDataset.Distribution.SPIRAL;
        gas.baseRadius = 1.0f;
        gas.armSigma = 0.1872f;
        gas.heightScale = 0.02f;

        var bulge = new BillboardDataset();
        bulge.distribution = BillboardDataset.Distribution.SPHERE;
        bulge.baseRadius = 0.12f;

        Assert.assertEquals(0.06, gas.volumeVerticalHalfExtent(), 1e-6);
        Assert.assertEquals(0.12, bulge.volumeVerticalHalfExtent(), 1e-6);
        Assert.assertEquals(0.12, bulge.volumeLateralHalfExtent(), 1e-6);

        var union = BillboardDataset.volumeGrid(List.of(gas, bulge), 20_400, GraphicsQuality.NORMAL);
        var gasOnly = BillboardDataset.volumeGrid(List.of(gas), 20_400, GraphicsQuality.NORMAL);
        Assert.assertTrue(union.spanYPc > gasOnly.spanYPc);
        Assert.assertEquals(gasOnly.spanPc, union.spanPc, 1e-9);
    }
}
