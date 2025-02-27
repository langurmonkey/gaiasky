/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.ByteBuffer;

public final class PipelineState implements Disposable {

    private final ByteBuffer byteBuffer;

    PipelineState() {
        byteBuffer = BufferUtils.newByteBuffer(32);
    }

    public boolean isEnabled(int propertyName) {
        if (propertyName == GL20.GL_BLEND) {
            Gdx.gl20.glGetBooleanv(GL20.GL_BLEND, byteBuffer);
            var ret = (byteBuffer.get() == 1);
            byteBuffer.clear();
            return ret;
        }

        return false;
    }

    @Override
    public void dispose() {
    }
}
