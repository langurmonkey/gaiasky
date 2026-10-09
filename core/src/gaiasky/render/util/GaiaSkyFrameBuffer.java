/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.badlogic.gdx.utils.BufferUtils;

public class GaiaSkyFrameBuffer extends FrameBuffer {

    // Indices for all buffers
    private int colorIndex = -1, depthIndex = -1, layerIndex = -1, normalIndex = -1, reflectionMaskIndex = -1;
    /**
     * Weighted blended OIT accumulation attachment:
     * <ul>
     *     <li>rgb = sum(E * w)</li>
*     <li>a = prod(1 - a), i.e. the transmittance, because alpha is blended
     *         multiplicatively with (ZERO, ONE_MINUS_SRC_ALPHA). This uses the raw coverage
     *         a, NOT a*w: w is a weight for the average and reaches 3e3, so a*w would make
     *         (1 - a*w) go negative and collapse the transmittance to zero (black)</li>
     * </ul>
     */
    private int accumIndex = -1;
    /**
     * Weighted blended OIT weight attachment: rgb = sum(a * w), the denominator of the
     * weighted average. Its alpha channel is blended multiplicatively as well and is unused.
     */
    private int weightIndex = -1;

    /**
     * Creates a buffer. Contains the builder and the indices for color, depth, layer, normal and reflection mask buffers.
     * If any of the indices is negative, the render target does not exist in this buffer.
     *
     * @param bufferBuilder The builder.
     * @param indices       The indices for color, depth, layer, normal and reflection mask buffers, followed
     *                      optionally by the OIT accum and weight indices.
     */
    public GaiaSkyFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder, int... indices) {
        super(bufferBuilder);
        if (indices.length > 0)
            colorIndex = indices[0];
        if (indices.length > 1)
            depthIndex = indices[1];
        if (indices.length > 2)
            layerIndex = indices[2];
        if (indices.length > 3)
            normalIndex = indices[3];
        if (indices.length > 4)
            reflectionMaskIndex = indices[4];
        if (indices.length > 5)
            accumIndex = indices[5];
        if (indices.length > 6)
            weightIndex = indices[6];
    }

    public Texture getColorBufferTexture() {
        if (colorIndex >= 0)
            return textureAttachments.get(colorIndex);
        else
            return null;
    }

    public Texture getDepthBufferTexture() {
        if (depthIndex >= 0)
            return textureAttachments.get(depthIndex);
        else
            return null;
    }

    public Texture getLayerBufferTexture() {
        if (layerIndex >= 0)
            return textureAttachments.get(layerIndex);
        else
            return null;
    }

    public Texture getNormalBufferTexture() {
        if (normalIndex >= 0)
            return textureAttachments.get(normalIndex);
        else
            return null;
    }

    public Texture getReflectionMaskBufferTexture() {
        if (reflectionMaskIndex >= 0)
            return textureAttachments.get(reflectionMaskIndex);
        else
            return null;
    }

    /**
     * @return the OIT accumulation buffer texture, whose rgb holds sum(E*w) and whose alpha holds
     * the transmittance prod(1 - a*w), or null if this buffer has no OIT attachments.
     */
    public Texture getAccumBufferTexture() {
        if (accumIndex >= 0)
            return textureAttachments.get(accumIndex);
        else
            return null;
    }

    /**
     * @return the OIT weight buffer texture, whose rgb holds sum(a*w), or null if this buffer has
     * no OIT attachments.
     */
    public Texture getWeightBufferTexture() {
        if (weightIndex >= 0)
            return textureAttachments.get(weightIndex);
        else
            return null;
    }

    public Texture getTextureAttachment(int index) {
        if (textureAttachments.size > index)
            return textureAttachments.get(index);
        else
            return null;
    }

    /**
     * Clears the weighted-blended-OIT attachments to their neutral values. Must be called with this
     * frame buffer bound.
     * <p>
     * The accum target needs (0, 0, 0, 1): rgb is a sum so it starts at zero, but alpha is a
     * <em>product</em> accumulated as dst *= (1 - a*w), so an empty (fully revealed) buffer is 1, not
     * 0. A plain glClear(GL_COLOR_BUFFER_BIT) with the scene clear colour (0, 0, 0, 0) would zero
     * the transmittance and make the whole scene black.
     * <p>
     * The weight target needs (0, 0, 0, 0) since it is a pure sum. Its alpha is unused.
     * <p>
     * Note this uses glClearBufferfv rather than glClearColor, so the scene clear colour set by
     * callers is left untouched.
     */
    public void clearOit() {
        if (accumIndex < 0)
            return;

        // Accum: sum(E*w) = 0, transmittance prod(1 - a*w) = 1.
        oitClearValue.put(0).put(0).put(0).put(1).position(0);
        Gdx.gl30.glClearBufferfv(GL30.GL_COLOR, accumIndex, oitClearValue);

        // Weight: sum(a*w) = 0. Alpha unused, cleared for hygiene.
        if (weightIndex >= 0) {
            oitClearValue.put(0).put(0).put(0).put(0).position(0);
            Gdx.gl30.glClearBufferfv(GL30.GL_COLOR, weightIndex, oitClearValue);
        }
    }

    /** Scratch buffer for {@link #clearOit()}, to avoid allocating every frame. */
    private final java.nio.FloatBuffer oitClearValue = BufferUtils.newFloatBuffer(4);
}
