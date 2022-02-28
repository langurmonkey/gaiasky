/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.utils;

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

    public Texture getVelocityBufferTexture() {
        if (textureAttachments.size > 2)
            return textureAttachments.get(2);
        else
            return null;
    }

    public Texture getNormalBufferTexture() {
        if (textureAttachments.size > 3)
            return textureAttachments.get(3);
        else
            return null;
    }

    public Texture getReflectionBufferTexture() {
        if (textureAttachments.size > 4)
            return textureAttachments.get(4);
        else
            return null;
    }

    public Texture getColorBufferTexture() {
        return super.getColorBufferTexture();
    }

    public Texture getTextureAttachment(int index) {
        if (textureAttachments.size > index)
            return textureAttachments.get(index);
        else
            return null;
    }
}
