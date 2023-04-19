/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GLTexture;

public class TextureView extends GLTexture {
    private int width, height;

    public TextureView() {
        super(GL20.GL_TEXTURE_2D, 0);
    }

    public TextureView(int glHandle, int width, int height) {
        super(GL20.GL_TEXTURE_2D, glHandle);
        this.width = width;
        this.height = height;
    }

    public void setTexture(int glHandle, int width, int height) {
        this.glHandle = glHandle;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    protected void reload() {
    }
}
