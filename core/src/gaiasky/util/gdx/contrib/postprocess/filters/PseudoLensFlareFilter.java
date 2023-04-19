/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class PseudoLensFlareFilter extends Filter<PseudoLensFlareFilter> {
    private final Vector2 viewportInverse;
    private int ghosts;
    private float haloWidth;
    private Texture lensColorTexture;

    public PseudoLensFlareFilter(int width, int height) {
        super(ShaderLoader.fromFile("screenspace", "pseudolensflare"));
        viewportInverse = new Vector2(1f / width, 1f / height);
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.viewportInverse.set(1f / width, 1f / height);
        setParam(Param.ViewportInverse, this.viewportInverse);
    }

    public int getGhosts() {
        return ghosts;
    }

    public void setGhosts(int ghosts) {
        this.ghosts = ghosts;
        setParam(Param.Ghosts, ghosts);
    }

    public float getHaloWidth() {
        return haloWidth;
    }

    public void setHaloWidth(float haloWidth) {
        this.haloWidth = haloWidth;
        setParam(Param.HaloWidth, haloWidth);
    }

    public void setLensColorTexture(Texture tex) {
        this.lensColorTexture = tex;
        setParam(Param.LensColor, u_texture1);
    }

    @Override
    public void rebind() {
        // Re-implement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.LensColor, u_texture1);
        setParams(Param.ViewportInverse, viewportInverse);
        setParams(Param.Ghosts, ghosts);
        setParams(Param.HaloWidth, haloWidth);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        lensColorTexture.bind(u_texture1);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        LensColor("u_texture1", 0),
        ViewportInverse("u_viewportInverse", 2),
        Ghosts("u_ghosts", 0),
        HaloWidth("u_haloWidth", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }
}
