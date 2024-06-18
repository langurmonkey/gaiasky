/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.postprocess.util.FullscreenMesh;
import gaiasky.render.util.ShaderLoader;
import gaiasky.util.gdx.loader.PFMData;
import gaiasky.util.gdx.loader.WarpMeshReader.WarpMesh;

public final class WarpingMeshFilter extends Filter<WarpingMeshFilter> {
    private final FullscreenMesh mesh;
    private Texture blendTexture;
    private final Vector2 viewport;
    private int blend;

    public WarpingMeshFilter(PFMData warpData, float rw, float rh) {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        this.blend = 0;
        this.mesh = new FullscreenMesh(warpData.data, warpData.width, warpData.height);
        this.viewport = new Vector2(rw, rh);
        rebind();
    }

    public WarpingMeshFilter(PFMData warpData, Texture blend) {
        super(ShaderLoader.fromFile("screenspace", "geometrywarp"));
        this.mesh = new FullscreenMesh(warpData.data, warpData.width, warpData.height);
        this.viewport = new Vector2();
        rebind();
        setBlendTexture(blend);
    }

    public WarpingMeshFilter(WarpMesh warpMesh, int rw, int rh) {
        super(ShaderLoader.fromFile("screenspace-alpha", "geometrywarp-alpha"));
        this.mesh = new FullscreenMesh(warpMesh);
        this.viewport = new Vector2(rw, rh);
        rebind();
    }

    public void setBlendTexture(Texture tex) {
        this.blendTexture = tex;
        setParam(Param.Blend, u_texture1);

        this.blend = tex == null ? 0 : 1;
        setParam(Param.BlendState, this.blend);
    }

    public void setViewportSize(int width, int height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, viewport);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
        if (blendTexture != null)
            blendTexture.bind(u_texture1);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Param.Blend, u_texture1);
        setParams(Param.Viewport, viewport);
        setParams(Param.BlendState, blend);

        endParams();
    }

    @Override
    protected void realRender() {
        // gives a chance to filters to perform needed operations just before the rendering operation take place.
        onBeforeRender();

        program.bind();
        mesh.render(program);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Blend("u_texture1", 0),
        Viewport("u_viewport", 2),
        BlendState("u_blend", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String m, int elementSize) {
            this.mnemonic = m;
            this.elementSize = elementSize;
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
