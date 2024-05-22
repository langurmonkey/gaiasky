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

    private final Matrix4 combined, combinedGlobal;
    private final TextureDescriptor<Texture> td, tdGlobal;

    public ShadowMapImpl(Matrix4 combined, Texture td, Matrix4 combinedGlobal, Texture tdGlobal) {
        super();
        this.combined = combined;
        this.td = new TextureDescriptor<>(td, TextureFilter.Linear, TextureFilter.Linear, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
        this.combinedGlobal = combinedGlobal;
        this.tdGlobal = new TextureDescriptor<>(tdGlobal, TextureFilter.Linear, TextureFilter.Linear, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }


    public void setProjViewTrans(Matrix4 mat) {
        this.combined.set(mat);
    }

    @Override
    public Matrix4 getProjViewTrans() {
        return combined;
    }

    public void setProjViewTransGlobal(Matrix4 mat) {
        this.combinedGlobal.set(mat);
    }

    public Matrix4 getProjViewTransGlobal() {
        return combinedGlobal;
    }

    public void setDepthMap(Texture tex) {
        this.td.set(tex, TextureFilter.Linear, TextureFilter.Linear, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    @Override
    public TextureDescriptor<Texture> getDepthMap() {
        return td;
    }

    public void setDepthMapGlobal(Texture tex) {
        this.tdGlobal.set(tex, TextureFilter.Linear, TextureFilter.Linear, TextureWrap.ClampToEdge, TextureWrap.ClampToEdge);
    }

    public TextureDescriptor<Texture> getDepthMapGlobal() {
        return tdGlobal;
    }

}
