/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.Buffer;

public class TextureExt extends Texture {
    public TextureExt(TextureData data) {
        super(data);
    }

    public TextureExt(String internalPath) {
        super(internalPath);
    }

    /**
     * Draws the given {@link Pixmap} to the texture mipmap level at position x, y. No clipping is performed so you have to make sure that you
     * draw only inside the texture region.
     *
     * @param pixmap      The Pixmap.
     * @param x           The x coordinate in pixels.
     * @param y           The y coordinate in pixels.
     * @param mipmapLevel The mipmap level to draw.
     */
    public void draw(Pixmap pixmap, int x, int y, int mipmapLevel) {
        if (getTextureData().isManaged())
            throw new GdxRuntimeException("can't draw to a managed texture");

        bind();
        Gdx.gl.glTexSubImage2D(glTarget, mipmapLevel, x, y, pixmap.getWidth(), pixmap.getHeight(), pixmap.getGLFormat(), pixmap.getGLType(), pixmap.getPixels());
    }

    /**
     * Draws the given {@link Buffer} to the texture mipmap level at position x, y. No clipping is performed, so you have to make sure that you
     * draw only inside the texture region.
     *
     * @param buffer      The buffer.
     * @param x           The x coordinate in pixels.
     * @param y           The y coordinate in pixels.
     * @param mipmapLevel The mipmap level to draw.
     * @param glFormat    Specifies the format of the pixel data. The following symbolic values are accepted:
     *                    GL_RED, GL_RG, GL_RGB, GL_BGR, GL_RGBA, GL_BGRA, GL_DEPTH_COMPONENT, and GL_STENCIL_INDEX.
     * @param glType      Specifies the data type of the pixel data. The following symbolic values are accepted:
     *                    GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT, GL_UNSIGNED_INT, GL_INT, GL_FLOAT,
     *                    GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, GL_UNSIGNED_SHORT_5_6_5,
     *                    GL_UNSIGNED_SHORT_5_6_5_REV, GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV,
     *                    GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, GL_UNSIGNED_INT_8_8_8_8,
     *                    GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, and GL_UNSIGNED_INT_2_10_10_10_REV.
     */
    public void draw(Buffer buffer, int x, int y, int width, int height, int mipmapLevel, int glFormat, int glType) {
        if (getTextureData().isManaged())
            throw new GdxRuntimeException("can't draw to a managed texture");

        bind();
        Gdx.gl.glTexSubImage2D(glTarget, mipmapLevel, x, y, width, height, glFormat, glType, buffer);
    }

}
