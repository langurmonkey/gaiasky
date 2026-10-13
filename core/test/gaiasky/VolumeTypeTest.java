/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky;

import gaiasky.render.gdx.graphics.VolumeType;
import org.junit.Assert;
import org.junit.Test;

/**
 * {@link VolumeType#colorMode()} maps a texture's storage format to the {@code u_volumeNColorMode}
 * value the volume shader expects. Getting this wrong would either leave an RGBA volume rendered
 * through the look-up ramp (mode 0) or feed the ramp a density read from the wrong channel, so the
 * mapping is pinned here.
 */
public class VolumeTypeTest {

    @Test
    public void modeConstantsMatchShaderContract() {
        // These values are hard-coded in the volume shaders (assets/shader/volume.fragment.glsl and
        // the data-dir override); changing them breaks both.
        Assert.assertEquals(0, VolumeType.COLOR_MODE_RAMP);
        Assert.assertEquals(1, VolumeType.COLOR_MODE_TEXTURE);
        Assert.assertEquals(2, VolumeType.COLOR_MODE_TEXTURE_RAMP);
    }

    @Test
    public void colorModeMapping() {
        // Density-only (R8) has no colour to sample: keep the ramp, i.e. today's image.
        Assert.assertEquals(VolumeType.COLOR_MODE_RAMP, VolumeType.DENSITY.colorMode());
        // Color and RGBA carry per-voxel colour and use it directly.
        Assert.assertEquals(VolumeType.COLOR_MODE_TEXTURE, VolumeType.COLOR.colorMode());
        Assert.assertEquals(VolumeType.COLOR_MODE_TEXTURE, VolumeType.RGBA.colorMode());
    }

    @Test
    public void onlyDensityUsesRamp() {
        // A regression guard: only DENSITY may fall back to the ramp, so a new format added to the
        // enum forces a conscious choice here rather than silently rendering through the ramp.
        for (VolumeType type : VolumeType.values()) {
            int expected = type == VolumeType.DENSITY ? VolumeType.COLOR_MODE_RAMP : VolumeType.COLOR_MODE_TEXTURE;
            Assert.assertEquals("unexpected colour mode for " + type, expected, type.colorMode());
        }
    }

    @Test
    public void glFormatsAreDistinct() {
        // The loader picks the GL format from these; a clash would make two formats indistinguishable.
        Assert.assertNotEquals(VolumeType.DENSITY.glFormat, VolumeType.RGBA.glFormat);
        Assert.assertNotEquals(VolumeType.COLOR.glFormat, VolumeType.RGBA.glFormat);
        Assert.assertNotEquals(VolumeType.DENSITY.glInternalFormat, VolumeType.RGBA.glInternalFormat);
    }
}
