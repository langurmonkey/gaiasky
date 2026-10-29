/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.GalaxyVolumeBuilder;
import gaiasky.scene.record.BillboardDataset;
import gaiasky.util.Settings.GraphicsQuality;
import org.junit.Assert;
import org.junit.Test;

/**
 * Sorting of a galaxy's datasets into the two volume slots. Wrong grouping is silent: gas in the
 * occlusive slot would absorb instead of emit, and a bulge left in its own slot would double the
 * textures and lose its light on top of the gas.
 */
public class GalaxyVolumePlanTest {

    private static BillboardDataset dataset(BillboardDataset.ChannelType type) {
        var ds = new BillboardDataset();
        ds.type = type;
        ds.distribution = type == BillboardDataset.ChannelType.BULGE
                ? BillboardDataset.Distribution.SPHERE
                : BillboardDataset.Distribution.SPIRAL;
        ds.baseRadius = type == BillboardDataset.ChannelType.BULGE ? 0.12f : 1.0f;
        ds.armSigma = 0.1872f;
        ds.heightScale = 0.02f;
        return ds;
    }

    private static BillboardDataset[] andromeda() {
        return new BillboardDataset[]{
                dataset(BillboardDataset.ChannelType.STAR),
                dataset(BillboardDataset.ChannelType.GAS),
                dataset(BillboardDataset.ChannelType.BULGE),
                dataset(BillboardDataset.ChannelType.DUST),
                dataset(BillboardDataset.ChannelType.HII)
        };
    }

    @Test
    public void gasAndBulgeShareTheEmissiveSlot() {
        var plan = GalaxyVolumeBuilder.plan(andromeda(), 20_400, GraphicsQuality.NORMAL);
        Assert.assertEquals(2, plan.size());

        var emissive = plan.get(0);
        Assert.assertEquals(GalaxyVolumeBuilder.SLOT_EMISSIVE, emissive.slot());
        Assert.assertEquals(2, emissive.datasets().size());
        Assert.assertEquals(BillboardDataset.ChannelType.GAS, emissive.datasets().get(0).type);
        Assert.assertEquals(BillboardDataset.ChannelType.BULGE, emissive.datasets().get(1).type);
        Assert.assertEquals((float) VolumeCalibrationTarget.EMISSIVE, emissive.targetMeanLuma(), 1e-12);
    }

    @Test
    public void dustIsTheOcclusiveSlot() {
        var occlusive = GalaxyVolumeBuilder.plan(andromeda(), 20_400, GraphicsQuality.NORMAL).get(1);
        Assert.assertEquals(GalaxyVolumeBuilder.SLOT_OCCLUSIVE, occlusive.slot());
        Assert.assertEquals(1, occlusive.datasets().size());
        Assert.assertEquals(BillboardDataset.ChannelType.DUST, occlusive.datasets().get(0).type);
        Assert.assertEquals((float) VolumeCalibrationTarget.OCCLUSIVE, occlusive.targetMeanLuma(), 1e-12);
    }

    @Test
    public void starsAndHiiStayBillboards() {
        var stars = dataset(BillboardDataset.ChannelType.STAR);
        var hii = dataset(BillboardDataset.ChannelType.HII);
        stars.generation = BillboardDataset.Generation.VOLUME;
        hii.generation = BillboardDataset.Generation.VOLUME;
        // The field model would smear them into a cloud, so the channel decides, not the descriptor.
        Assert.assertFalse(stars.isVolume());
        Assert.assertFalse(hii.isVolume());
        Assert.assertEquals(-1, stars.volumeSlot());
    }

    @Test
    public void legacyDescriptorsKeepTheirBillboards() {
        var gas = dataset(BillboardDataset.ChannelType.GAS);
        gas.generation = BillboardDataset.Generation.BILLBOARD;
        Assert.assertTrue(GalaxyVolumeBuilder.plan(new BillboardDataset[]{gas}, 20_400, GraphicsQuality.NORMAL).isEmpty());
    }

    @Test
    public void emissiveGridHoldsTheBulge() {
        var plan = GalaxyVolumeBuilder.plan(andromeda(), 20_400, GraphicsQuality.NORMAL);
        var gasOnly = GalaxyVolumeBuilder.collect(andromeda(), GalaxyVolumeBuilder.SLOT_EMISSIVE).subList(0, 1);
        var union = plan.get(0).grid();
        var alone = BillboardDataset.volumeGrid(gasOnly, 20_400, GraphicsQuality.NORMAL);

        // Same lateral span, taller box: the bulge reaches above the gas slab.
        Assert.assertEquals(alone.spanPc, union.spanPc, 1e-9);
        Assert.assertTrue(union.spanYPc > alone.spanYPc);
        // And the dust slot follows the dust slab, which is thinner than the gas one.
        Assert.assertTrue(plan.get(1).grid().spanYPc <= union.spanYPc);
    }

    @Test
    public void fieldsPerSlotAreCappedByTheShader() {
        var gas = dataset(BillboardDataset.ChannelType.GAS);
        var datasets = new BillboardDataset[]{gas, gas, gas, gas};
        Assert.assertEquals(BillboardDataset.MAX_FIELDS,
                            GalaxyVolumeBuilder.collect(datasets, GalaxyVolumeBuilder.SLOT_EMISSIVE).size());
    }

    /** Mirrors the two {@link gaiasky.render.gdx.graphics.VolumeCalibration} targets, to catch a drift. **/
    private static final class VolumeCalibrationTarget {
        static final double EMISSIVE = gaiasky.render.gdx.graphics.VolumeCalibration.EMISSIVE_TARGET_MEAN_LUMA;
        static final double OCCLUSIVE = gaiasky.render.gdx.graphics.VolumeCalibration.OCCLUSIVE_TARGET_MEAN_LUMA;
    }
}
