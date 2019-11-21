/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/**
 * Fisheye distortion filter
 *
 * @author tsagrista
 */

package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class FisheyeDistortion extends Filter<FisheyeDistortion> {
    private Vector2 viewport;

    public enum Param implements Parameter {
        // @formatter:off
        Texture0("u_texture0", 0),
        Viewport("u_viewport", 2);
        // @formatter:on

        private final String mnemonic;
        private int elementSize;

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



    public FisheyeDistortion(int width, int height) {
        super(ShaderLoader.fromFile("screenspace", "fisheye"));
        viewport = new Vector2(width, height);
        rebind();
    }

    public void setViewportSize(float width, float height) {
        this.viewport.set(width, height);
        setParam(Glow.Param.Viewport, this.viewport);
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture0, u_texture0);
        setParams(Glow.Param.Viewport, viewport);

        endParams();
    }
}
