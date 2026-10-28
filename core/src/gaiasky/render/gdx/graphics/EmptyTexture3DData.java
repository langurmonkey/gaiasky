/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture3DData;

/**
 * A 3D texture that GL allocates but nobody fills from the CPU.
 * <p>
 * {@code CustomTexture3DData} keeps a host-side pixel buffer, which is the right thing for data that
 * comes from a file but not for a volume a compute shader is about to overwrite voxel by voxel: the
 * largest grid the voxel budget allows is tens of megabytes of host memory that would be allocated,
 * zeroed, uploaded and immediately replaced. This hands {@code glTexImage3D} a null pointer instead,
 * so the bytes GL allocates are the only bytes involved.
 */
public final class EmptyTexture3DData implements Texture3DData {
    private final int width;
    private final int height;
    private final int depth;
    private final int glFormat;
    private final int glInternalFormat;
    private final int glType;

    public EmptyTexture3DData(int width, int height, int depth, int glFormat, int glInternalFormat, int glType) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.glFormat = glFormat;
        this.glInternalFormat = glInternalFormat;
        this.glType = glType;
    }

    @Override
    public boolean isPrepared() {
        return true;
    }

    @Override
    public void prepare() {
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
        return depth;
    }

    @Override
    public int getInternalFormat() {
        return glInternalFormat;
    }

    @Override
    public int getGLType() {
        return glType;
    }

    @Override
    public boolean useMipMaps() {
        return false;
    }

    @Override
    public void consume3DData() {
        // Null pointer: allocate, do not upload.
        Gdx.gl30.glTexImage3D(GL30.GL_TEXTURE_3D, 0, glInternalFormat, width, height, depth, 0, glFormat, glType, null);
    }

    @Override
    public boolean isManaged() {
        return false;
    }
}
