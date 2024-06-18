/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.render.util.ShaderLoader;

public final class GravitationalDistortionFilter extends Filter<GravitationalDistortionFilter> {
    private final Vector2 viewport;
    private final Vector2 massPosition;

    public GravitationalDistortionFilter(Vector2 viewportSize) {
        super(ShaderLoader.fromFile("screenspace", "gravitydistortion"));

        this.viewport = viewportSize;

        this.massPosition = new Vector2();

        rebind();
    }

    public GravitationalDistortionFilter(int viewportWidth, int viewportHeight) {
        this(new Vector2(viewportWidth, viewportHeight));
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Param.Viewport, this.viewport);
    }

    /**
     * The position of the mass that causes the distortion in pixels.
     *
     * @param x
     * @param y
     */
    public void setMassPosition(float x, float y) {
        this.massPosition.set(x, y);
        setParam(Param.MassPosition, this.massPosition);
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.Viewport, this.viewport);
        setParams(Param.MassPosition, this.massPosition);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Viewport("u_viewport", 2),
        MassPosition("u_massPosition", 2);
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
