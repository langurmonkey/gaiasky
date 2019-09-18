/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.util.gdx.contrib.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;

/**
 * Exposes a builder accepting a buffer builder.
 */
public class GaiaSkyFrameBuffer extends FrameBuffer {

    public GaiaSkyFrameBuffer(GLFrameBufferBuilder<? extends GLFrameBuffer<Texture>> bufferBuilder) {
        super(bufferBuilder);
    }

    public Texture getDepthBufferTexture() {
        // Last texture attachment
        if (textureAttachments.size > 1)
            return textureAttachments.get(1);
        else
            return null;
    }

    public Texture getOwnDepthBufferTexture() {
        if (textureAttachments.size > 2)
            return textureAttachments.get(2);
        else
            return null;
    }

    public Texture getColorBufferTexture() {
        return super.getColorBufferTexture();
    }

    public Texture getColorBufferTexture1() {
        return super.getColorBufferTexture();
    }

    public Texture getColorBufferTexture2() {
        // Second texture attachment
        if (textureAttachments.size > 1)
            return textureAttachments.get(1);
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
