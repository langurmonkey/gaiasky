/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.graphics.glutils.CustomTexture3DData;
import com.badlogic.gdx.math.Vector3;

import java.nio.ByteBuffer;

/**
 * A loaded 3D volume: the voxels, the box they live in, and the occupancy grid that says where in
 * that box there is anything at all.
 *
 * @param texture   The volume's voxels.
 * @param width     The grid's extent on the texture's first axis.
 * @param height    The grid's extent on the texture's second axis.
 * @param depth     The grid's extent on the texture's third axis.
 * @param type      The storage format, which fixes where the density channel is.
 * @param boundsMin The box, in texture order. See {@code VolumeTextureLoader.glBounds}.
 * @param boundsMax The other corner of the box.
 * @param occupancy The brick occupancy grid of the same box, or null when there is none.
 */
public record VolumeTexture(Texture3D texture, int width, int height, int depth, VolumeType type, Vector3 boundsMin,
                            Vector3 boundsMax, Texture3D occupancy) {

    /** The fallback of {@link #occupancyDescriptor}: one texel, holding the maximum there is. */
    private static TextureDescriptor<Texture3D> fullOccupancy;

    /**
     * The occupancy texture to bind to a volume slot: the volume's own, or a 1x1x1 volume of ones
     * when it has none.
     * <p>
     * A slot has no occupancy grid when its volume carries no density to measure (a colour-only
     * volume) or was loaded before the grid existed. The shader's uniform is unconditional - it has
     * no per-slot flag to switch it off - so the fallback has to be something the empty-brick test
     * can read and be told "not empty" by. A single texel of 1, clamped to the whole box, is
     * exactly that, and says the do-nothing thing for one R8 fetch per step.
     */
    public static TextureDescriptor<Texture3D> occupancyDescriptor(TextureDescriptor<Texture3D> occupancy) {
        if (occupancy != null && occupancy.texture != null) {
            return occupancy;
        }
        if (fullOccupancy == null) {
            var data = new CustomTexture3DData(1, 1, 1, 0,
                                               VolumeType.DENSITY.glFormat,
                                               VolumeType.DENSITY.glInternalFormat,
                                               VolumeType.DENSITY.glType);
            ByteBuffer pixels = data.getPixels();
            pixels.put((byte) 255);
            pixels.flip();

            Texture3D texture = new Texture3D(data);
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge,
                            Texture.TextureWrap.ClampToEdge);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

            fullOccupancy = new TextureDescriptor<>();
            fullOccupancy.texture = texture;
        }
        return fullOccupancy;
    }

    /** The volume's own occupancy grid, or the fallback, ready to hand to the texture binder. */
    public TextureDescriptor<Texture3D> occupancyDescriptor() {
        TextureDescriptor<Texture3D> descriptor = null;
        if (occupancy != null) {
            descriptor = new TextureDescriptor<>();
            descriptor.texture = occupancy;
        }
        return occupancyDescriptor(descriptor);
    }
}
