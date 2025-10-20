/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.graphics;

import com.badlogic.gdx.graphics.GL30;

public enum VolumeType {
    DENSITY(GL30.GL_RED, GL30.GL_R8, GL30.GL_UNSIGNED_BYTE),
    COLOR(GL30.GL_RGB, GL30.GL_RGB8, GL30.GL_UNSIGNED_BYTE);

    public final int glFormat;
    public final int glInternalFormat;
    public final int glType;

    VolumeType(int glFormat, int glInternalFormat, int glType) {
        this.glFormat = glFormat;
        this.glInternalFormat = glInternalFormat;
        this.glType = glType;
    }
}
