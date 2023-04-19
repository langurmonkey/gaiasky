/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g3d.environment.ShadowMap;
import com.badlogic.gdx.graphics.g3d.utils.TextureDescriptor;
import com.badlogic.gdx.math.Matrix4;

public class ShadowMapImpl implements ShadowMap {

    private final Matrix4 trans;
    private final TextureDescriptor<Texture> td;

    public ShadowMapImpl(Matrix4 trans, Texture tex) {
        super();
        this.trans = trans;
        this.td = new TextureDescriptor<>(tex);
    }

    @Override
    public Matrix4 getProjViewTrans() {
        return trans;
    }

    public void setProjViewTrans(Matrix4 mat) {
        this.trans.set(mat);
    }

    @Override
    public TextureDescriptor<Texture> getDepthMap() {
        return td;
    }

    public void setDepthMap(Texture tex) {
        this.td.set(tex, TextureFilter.Nearest, TextureFilter.Nearest, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

}
