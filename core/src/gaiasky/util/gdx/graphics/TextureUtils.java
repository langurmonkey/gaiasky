/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import java.nio.Buffer;

public class TextureUtils {

    /**
     * Draw a two-dimensional buffer to a texture using <code>glTexSubImage2D()</code>.
     *
     * @param tex         The texture.
     * @param mipmapLevel Specifies the level-of-detail number. Level 0 is the base image level.
     *                    Level n is the nth mipmap reduction image.
     * @param x           Specifies a texel offset in the x direction within the texture array.
     * @param y           Specifies a texel offset in the y direction within the texture array.
     * @param width       Specifies the width of the texture subimage.
     * @param height      Specifies the height of the texture subimage.
     * @param glFormat    Specifies the format of the pixel data. The following symbolic values are accepted:
     *                    GL_RED, GL_RG, GL_RGB, GL_BGR, GL_RGBA, GL_BGRA, GL_DEPTH_COMPONENT, and GL_STENCIL_INDEX.
     * @param glType      Specifies the data type of the pixel data. The following symbolic values are accepted:
     *                    GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT, GL_UNSIGNED_INT, GL_INT, GL_FLOAT,
     *                    GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV,
     *                    GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, GL_UNSIGNED_SHORT_5_5_5_1,
     *                    GL_UNSIGNED_SHORT_1_5_5_5_REV, GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV,
     *                    GL_UNSIGNED_INT_10_10_10_2, and GL_UNSIGNED_INT_2_10_10_10_REV.
     * @param pixels      Specifies a pointer to the image data in memory as a {@link java.nio.Buffer}.
     */
    public static void texSubImage2D(Texture tex, int mipmapLevel, int x, int y, int width, int height, int glFormat, int glType, Buffer pixels) {
        tex.bind();
        Gdx.gl.glTexSubImage2D(tex.glTarget, mipmapLevel, x, y, width, height, glFormat, glType, pixels);
    }
}
