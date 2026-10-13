/*
 * Copyright (c) 2025-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.graphics.GL30;

public enum VolumeType {
    DENSITY(GL30.GL_RED, GL30.GL_R8, GL30.GL_UNSIGNED_BYTE),
    COLOR(GL30.GL_RGB, GL30.GL_RGB8, GL30.GL_UNSIGNED_BYTE),
    RGBA(GL30.GL_RGBA, GL30.GL_RGBA8, GL30.GL_UNSIGNED_BYTE);

    /*
     * Volume shader color modes ({@code u_volumeNColorMode}): how a voxel's color is obtained.
     * These must match the branch conditions in the volume shaders.
     */
    /** Use the look-up ramp applied to the voxel density (the pre-RGBA behaviour). */
    public static final int COLOR_MODE_RAMP = 0;
    /** Use the per-voxel color stored in the texture (RGB), e.g. the RGBA format. */
    public static final int COLOR_MODE_TEXTURE = 1;
    /** Use the per-voxel color modulated by the look-up ramp. */
    public static final int COLOR_MODE_TEXTURE_RAMP = 2;

    public final int glFormat;
    public final int glInternalFormat;
    public final int glType;

    VolumeType(int glFormat, int glInternalFormat, int glType) {
        this.glFormat = glFormat;
        this.glInternalFormat = glInternalFormat;
        this.glType = glType;
    }

    /**
     * Colour mode to apply when a volume slot holds a texture of this format, i.e. which of the
     * three {@code u_volumeNColorMode} values the shader should use.
     * <p>
     * DENSITY (R8) stores no colour, so it falls back to the look-up ramp - identical to the
     * pre-RGBA render. COLOR and RGBA carry per-voxel colour and use it directly. Mode
     * {@link #COLOR_MODE_TEXTURE_RAMP} is not produced by default; it is available to callers that
     * want to modulate the baked colour by the ramp.
     */
    public int colorMode() {
        return this == DENSITY ? COLOR_MODE_RAMP : COLOR_MODE_TEXTURE;
    }
}
