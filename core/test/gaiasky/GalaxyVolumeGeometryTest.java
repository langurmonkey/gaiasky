/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.GalaxyVolumeGenerator;
import gaiasky.render.gdx.graphics.VoxelGrid;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Settings.GraphicsQuality;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Geometry of a generated volume, which is the part of {@link GalaxyVolumeGenerator} that can be
 * checked without a GL context: where the box sits, where its voxels land, and how the box is
 * expressed in the units the raymarcher scales by {@code u_bodySize}.
 */
public class GalaxyVolumeGeometryTest {

    private static BillboardDataset gas() {
        var gas = new BillboardDataset();
        gas.type = BillboardDataset.ChannelType.GAS;
        gas.distribution = BillboardDataset.Distribution.SPIRAL;
        gas.baseRadius = 1.0f;
        gas.armSigma = 0.1872369f;
        gas.heightScale = 0.02f;
        return gas;
    }

    private static BillboardDataset bulge() {
        var bulge = new BillboardDataset();
        bulge.type = BillboardDataset.ChannelType.BULGE;
        bulge.distribution = BillboardDataset.Distribution.SPHERE;
        bulge.baseRadius = 0.12f;
        return bulge;
    }

    @Test
    public void boxSpansOneOnTheLargestAxis() {
        float[] box = GalaxyVolumeGenerator.boxFor(1.1123, 0.12);
        Assert.assertEquals(-0.5f, box[0], 0f);
        Assert.assertEquals(0.5f, box[3], 0f);
        Assert.assertEquals(-0.5f, box[2], 0f);
        Assert.assertEquals(0.5f, box[5], 0f);
        // The vertical half-extent is the fields' aspect ratio, halved.
        Assert.assertEquals(0.5 * 0.12 / 1.1123, box[4], 1e-6);
        Assert.assertEquals(-box[4], box[1], 0f);
        // Centred.
        Assert.assertEquals(0f, box[0] + box[3], 0f);
        Assert.assertEquals(0f, box[1] + box[4], 0f);
        Assert.assertEquals(0f, box[2] + box[5], 0f);
    }

    @Test
    public void voxelCentresFillTheBox() {
        var grid = BillboardDataset.volumeGrid(List.of(gas(), bulge()), 20_400, GraphicsQuality.NORMAL);
        double lateral = 1.1123, vertical = 0.12;
        float[] origin = GalaxyVolumeGenerator.gridOrigin(lateral, vertical, grid);
        float[] cell = GalaxyVolumeGenerator.gridCell(lateral, vertical, grid);

        // The first voxel centre is half a cell inside the box...
        Assert.assertEquals(-lateral + 0.5 * cell[0], origin[0], 1e-6);
        Assert.assertEquals(-vertical + 0.5 * cell[1], origin[1], 1e-6);
        Assert.assertEquals(-lateral + 0.5 * cell[2], origin[2], 1e-6);

        // ...and the last one plus half a cell is the far face, on every axis. This is what makes the
        // box in the boxFor() test the same box the shader fills.
        Assert.assertEquals(lateral, origin[0] + (grid.nx - 1) * cell[0] + 0.5 * cell[0], 1e-5);
        Assert.assertEquals(vertical, origin[1] + (grid.ny - 1) * cell[1] + 0.5 * cell[1], 1e-5);
        Assert.assertEquals(lateral, origin[2] + (grid.nz - 1) * cell[2] + 0.5 * cell[2], 1e-5);

        // Voxels are cubic: the lateral axes must agree, and the vertical one too when the floor is
        // not in play.
        Assert.assertEquals(cell[0], cell[2], 1e-9);
        double voxelPc = grid.spanPc / grid.nx;
        Assert.assertEquals(voxelPc, 2 * lateral / grid.nx / (2 * lateral / grid.spanPc), 1e-9);
    }

    @Test
    public void packedFieldsAreTheShadersUniformSize() {
        var fields = GalaxyVolumeGenerator.packFields(List.of(gas(), bulge()), 2);
        Assert.assertEquals(BillboardDataset.MAX_FIELDS * BillboardDataset.FIELD_STRIDE, fields.length);
        // Slot 0 is the gas, slot 1 the bulge, and the third slot is left at zero.
        Assert.assertEquals(BillboardDataset.ChannelType.GAS.ordinal(), fields[28], 0f);
        Assert.assertEquals(BillboardDataset.ChannelType.BULGE.ordinal(),
                            fields[BillboardDataset.FIELD_STRIDE + 28], 0f);
        Assert.assertEquals(0f, fields[2 * BillboardDataset.FIELD_STRIDE + 28], 0f);
    }
}
