/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.scene.component.Volume;
import gaiasky.scene.record.GalaxyAttributes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Routing of a procedural volume's {@link Volume#fields} into the raymarcher's two slots. The
 * descriptor has no slot attribute: the channel decides, and this pins down which channel goes
 * where.
 */
public class VolumeFieldsTest {

    private static GalaxyAttributes field(GalaxyAttributes.ChannelType type) {
        var f = new GalaxyAttributes();
        f.type = type;
        return f;
    }

    @Test
    public void noFieldsMeansNotProcedural() {
        var volume = new Volume();
        Assert.assertFalse(volume.isProcedural());
        Assert.assertTrue(volume.emissiveFields().isEmpty());
        Assert.assertTrue(volume.occlusiveFields().isEmpty());
    }

    @Test
    public void dustIsTheOnlyOccludingChannel() {
        for (var type : GalaxyAttributes.ChannelType.values()) {
            Assert.assertEquals(type == GalaxyAttributes.ChannelType.DUST, type.occludes());
            Assert.assertEquals(type == GalaxyAttributes.ChannelType.DUST, field(type).isOcclusive());
        }
    }

    @Test
    public void channelsRouteToTheirSlotPreservingOrder() {
        var volume = new Volume();
        volume.setFields(new Object[]{
                field(GalaxyAttributes.ChannelType.GAS),
                field(GalaxyAttributes.ChannelType.DUST),
                field(GalaxyAttributes.ChannelType.BULGE),
                field(GalaxyAttributes.ChannelType.DUST)
        });

        Assert.assertTrue(volume.isProcedural());

        var emissive = volume.emissiveFields();
        Assert.assertEquals(2, emissive.size());
        Assert.assertEquals(GalaxyAttributes.ChannelType.GAS, emissive.get(0).type);
        Assert.assertEquals(GalaxyAttributes.ChannelType.BULGE, emissive.get(1).type);

        var occlusive = volume.occlusiveFields();
        Assert.assertEquals(2, occlusive.size());
        Assert.assertEquals(GalaxyAttributes.ChannelType.DUST, occlusive.get(0).type);
        Assert.assertEquals(GalaxyAttributes.ChannelType.DUST, occlusive.get(1).type);
        // Two distinct entries, not the same one twice.
        Assert.assertNotSame(occlusive.get(0), occlusive.get(1));
    }

    @Test
    public void anAgalaxyMayCarryUpToMaxFieldsPerSlot() {
        // Both slots can be filled to MAX_FIELDS, which is what the packed uniform is sized for.
        int max = GalaxyAttributes.MAX_FIELDS;
        var objects = new Object[2 * max];
        for (int i = 0; i < max; i++) {
            objects[i] = field(GalaxyAttributes.ChannelType.GAS);
            objects[max + i] = field(GalaxyAttributes.ChannelType.DUST);
        }
        var volume = new Volume();
        volume.setFields(objects);
        Assert.assertEquals(max, volume.emissiveFields().size());
        Assert.assertEquals(max, volume.occlusiveFields().size());
    }

    @Test
    public void theTwoSlotsAreEmissiveAndOcclusive() {
        // The slot numbers the initializer binds to are the ones the raymarcher expects: 0 emits,
        // 1 absorbs. Anything else is a bug that would show as a dust texture emitting light.
        Assert.assertEquals(0, Volume.SLOT_EMISSIVE);
        Assert.assertEquals(1, Volume.SLOT_OCCLUSIVE);
    }

    @Test
    public void generatedSlotsStartEmptyAndClearSafely() {
        var volume = new Volume();
        // Nothing generated yet.
        Assert.assertNull(volume.getGeneratedVolume(Volume.SLOT_EMISSIVE));
        Assert.assertNull(volume.getGeneratedVolume(Volume.SLOT_OCCLUSIVE));
        // Clearing an empty slot, disposing nothing, or asking for a slot that does not exist must
        // all be harmless: they run on the removal path of every volume, including the loaded ones.
        volume.setGeneratedVolume(Volume.SLOT_EMISSIVE, null);
        volume.setGeneratedVolume(Volume.SLOT_OCCLUSIVE, null);
        volume.setGeneratedVolume(42, null);
        volume.dispose(null);
        Assert.assertNull(volume.getGeneratedVolume(Volume.SLOT_EMISSIVE));
        Assert.assertNull(volume.getGeneratedVolume(42));
    }

    @Test
    public void disposalIsVisibleToDeferredGeneration() {
        var volume = new Volume();
        Assert.assertFalse(volume.isDisposed());
        volume.dispose(null);
        // With lazy texture initialisation the generation callback arrives after the entity may be
        // gone; it checks this before binding anything.
        Assert.assertTrue(volume.isDisposed());
    }

}
