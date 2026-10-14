/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import com.badlogic.gdx.graphics.Texture3D;
import gaiasky.render.gdx.graphics.VolumeType;
import gaiasky.render.gdx.shader.Material;
import gaiasky.render.gdx.shader.attribute.Texture3DAttribute;
import org.junit.Assert;
import org.junit.Test;

/**
 * The storage format of a volume texture lives in {@link Texture3DAttribute#volumeType} and its
 * role as a depth-buffer occluder in {@link Texture3DAttribute#depthThreshold}. Both are consumed
 * by the renderer and both fail silently if dropped anywhere along the copy path, so they are
 * pinned here.
 */
public class Texture3DAttributeTest {

    private static final Texture3D NO_TEXTURE = null;

    private static Texture3DAttribute vol0(VolumeType type) {
        return new Texture3DAttribute(Texture3DAttribute.Volume0, NO_TEXTURE, type);
    }

    @Test
    public void defaultIsDensity() {
        // A slot created without an explicit type must keep the legacy density-only behaviour.
        Assert.assertEquals(VolumeType.DENSITY,
                            new Texture3DAttribute(Texture3DAttribute.Volume1).volumeType);
        Assert.assertEquals(VolumeType.DENSITY,
                            new Texture3DAttribute(Texture3DAttribute.Volume1, NO_TEXTURE).volumeType);
        Assert.assertEquals(VolumeType.DENSITY,
                            new Texture3DAttribute(Texture3DAttribute.Volume1, NO_TEXTURE, null).volumeType);
        // Unused slots are DENSITY by default too.
        Assert.assertEquals(VolumeType.DENSITY, new Texture3DAttribute(Texture3DAttribute.Volume3).volumeType);
    }

    @Test
    public void copyPreservesVolumeType() {
        // Material.copy() clones attributes through Attribute.copy(), so a type lost here would
        // silently turn every copied RGBA volume back into a density volume.
        Texture3DAttribute rgba = vol0(VolumeType.RGBA);
        Texture3DAttribute copy = (Texture3DAttribute) rgba.copy();

        Assert.assertEquals(VolumeType.RGBA, copy.volumeType);
        Assert.assertEquals(rgba.index, copy.index);
        Assert.assertEquals(rgba.hashCode(), copy.hashCode());
        Assert.assertEquals(0, rgba.compareTo(copy));
    }

    @Test
    public void volumeTypeParticipatesInHashAndOrder() {
        Texture3DAttribute density = vol0(VolumeType.DENSITY);
        Texture3DAttribute color = vol0(VolumeType.COLOR);
        Texture3DAttribute rgba = vol0(VolumeType.RGBA);

        // Different formats at the same slot must not compare equal, or a renderer keyed on the
        // material would treat the two textures as interchangeable.
        Assert.assertNotEquals(density.hashCode(), rgba.hashCode());
        Assert.assertNotEquals(0, density.compareTo(rgba));
        Assert.assertTrue(density.compareTo(color) < 0);
        Assert.assertTrue(color.compareTo(rgba) < 0);

        // ...but the same format at the same slot still compares equal.
        Texture3DAttribute rgba2 = vol0(VolumeType.RGBA);
        Assert.assertEquals(0, rgba.compareTo(rgba2));
        Assert.assertEquals(rgba.hashCode(), rgba2.hashCode());
    }

    @Test
    public void materialCarriesVolumeType() {
        Material original = new Material("volume-test");
        original.set(vol0(VolumeType.RGBA));

        Material copy = original.copy();
        Texture3DAttribute fromCopy = (Texture3DAttribute) copy.get(Texture3DAttribute.Volume0);
        Assert.assertNotNull("Material.copy() dropped the volume attribute", fromCopy);
        Assert.assertEquals(VolumeType.RGBA, fromCopy.volumeType);

        // Swapping the format at the same slot is a real material change: the attribute mask is
        // unchanged, but the value hash is not. (Note that Attributes.compareTo/equals only look
        // at the mask, so the value hash is the distinguishing property here.)
        Material other = new Material("volume-test");
        other.set(vol0(VolumeType.DENSITY));
        Assert.assertEquals(original.getMask(), other.getMask());
        Assert.assertNotEquals(original.attributesHash(), other.attributesHash());
        Assert.assertNotEquals(original.hashCode(), other.hashCode());
    }

    @Test
    public void depthThresholdDefaultsToNoOcclusion() {
        // A slot with no threshold must not write depth: a purely emissive volume (gas) is not an
        // occluder, and only the scene saying otherwise should make it one.
        Assert.assertEquals(0f, new Texture3DAttribute(Texture3DAttribute.Volume1).depthThreshold, 0f);
        Assert.assertEquals(0f, new Texture3DAttribute(Texture3DAttribute.Volume1, NO_TEXTURE).depthThreshold, 0f);
        Assert.assertEquals(0f, vol0(VolumeType.RGBA).depthThreshold, 0f);
    }

    @Test
    public void copyPreservesDepthThreshold() {
        Texture3DAttribute dust = vol0(VolumeType.RGBA);
        dust.depthThreshold = 0.02f;

        Texture3DAttribute copy = (Texture3DAttribute) dust.copy();
        Assert.assertEquals(0.02f, copy.depthThreshold, 0f);
        Assert.assertEquals(dust.hashCode(), copy.hashCode());
        Assert.assertEquals(0, dust.compareTo(copy));
    }

    @Test
    public void depthThresholdParticipatesInHashAndOrder() {
        // Two materials that differ only in whether the volume occludes must not hash the same,
        // or a renderer keyed on the material would treat them as interchangeable.
        Texture3DAttribute emitting = vol0(VolumeType.RGBA);
        Texture3DAttribute occluding = vol0(VolumeType.RGBA);
        occluding.depthThreshold = 0.05f;

        Assert.assertNotEquals(emitting.hashCode(), occluding.hashCode());
        Assert.assertTrue(emitting.compareTo(occluding) < 0);
        Assert.assertTrue(occluding.compareTo(emitting) > 0);
    }
}
