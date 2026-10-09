/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.util;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;

public class GaiaSkyFrameBuffer extends FrameBuffer {

    // Indices for all buffers
    private int colorIndex = -1, depthIndex = -1, layerIndex = -1, normalIndex = -1, reflectionMaskIndex = -1;
    /** Weighted blended OIT accumulation buffer (emission * weight accumulated). */
    private int accumIndex = -1;
    /** Weighted blended OIT revealage buffer (product of (1 - alpha * weight)). */
    private int revealageIndex = -1;

    /**
     * Creates a buffer. Contains the builder and the indices for color, depth, layer, normal and reflection mask buffers.
     * If any of the indices is negative, the render target does not exist in this buffer.
     *
     * @param bufferBuilder The builder.
     * @param indices       The indices for color, depth, layer, normal and reflection mask buffers, followed
     *                      optionally by the OIT accum and revealage indices.
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
            revealageIndex = indices[6];
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

    /** @return the OIT accumulation buffer texture, or null if this buffer has no OIT attachments. */
    public Texture getAccumBufferTexture() {
        if (accumIndex >= 0)
            return textureAttachments.get(accumIndex);
        else
            return null;
    }

    /** @return the OIT revealage buffer texture, or null if this buffer has no OIT attachments. */
    public Texture getRevealageBufferTexture() {
        if (revealageIndex >= 0)
            return textureAttachments.get(revealageIndex);
        else
            return null;
    }

    public Texture getTextureAttachment(int index) {
        if (textureAttachments.size > index)
            return textureAttachments.get(index);
        else
            return null;
    }
}
