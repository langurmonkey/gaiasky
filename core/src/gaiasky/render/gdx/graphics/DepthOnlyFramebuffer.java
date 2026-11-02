/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.GLOnlyTextureData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

/**
 * A framebuffer with a single 24-bit depth texture and no color attachment, for holding depth that is
 * not the scene's own: a volume's dust front, or a copy of the scene depth taken at a particular point
 * in the frame (see {@code SceneRenderer#snapshotDepth}).
 * <p>
 * The depth can be read back and can be blitted in or out, which is what a depth attachment of the
 * scene buffers allows too - so {@code glBlitFramebuffer} works between this and them.
 */
public class DepthOnlyFramebuffer implements Disposable {
    /** The depth texture, for reading the stored depth. **/
    public final Texture texture;
    /** The framebuffer object handle, for blitting into and out of it. **/
    public final int framebufferHandle;
    public final int width, height;

    /**
     * Creates the framebuffer and leaves the default one bound.
     *
     * @param width  The width in pixels.
     * @param height The height in pixels.
     *
     * @throws IllegalStateException If the driver reports the framebuffer as incomplete.
     */
    public DepthOnlyFramebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        var gl = Gdx.gl30;

        var data = new GLOnlyTextureData(width, height, 0, GL30.GL_DEPTH_COMPONENT24, GL30.GL_DEPTH_COMPONENT, GL30.GL_UNSIGNED_INT);
        texture = new Texture(data);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);

        framebufferHandle = gl.glGenFramebuffer();
        gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferHandle);
        gl.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL20.GL_DEPTH_ATTACHMENT, GL20.GL_TEXTURE_2D,
                                  texture.getTextureObjectHandle(), 0);
        // No color attachment, so both draw and read buffers must be NONE.
        var none = BufferUtils.newIntBuffer(1);
        none.put(0, GL30.GL_NONE);
        gl.glDrawBuffers(1, none);
        gl.glReadBuffer(GL30.GL_NONE);
        var status = gl.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        gl.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            dispose();
            throw new IllegalStateException("The depth-only framebuffer is incomplete: 0x" + Integer.toHexString(status));
        }
    }

    @Override
    public void dispose() {
        texture.dispose();
        Gdx.gl30.glDeleteFramebuffer(framebufferHandle);
    }
}
