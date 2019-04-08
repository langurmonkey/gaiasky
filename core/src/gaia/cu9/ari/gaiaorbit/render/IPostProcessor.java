/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import com.bitfire.postprocessing.PostProcessor;
import com.bitfire.postprocessing.effects.*;

public interface IPostProcessor extends Disposable {
    class PostProcessBean {
        public PostProcessor pp;
        public Bloom bloom;
        public Antialiasing antialiasing;
        public LensFlare2 lens;
        public Curvature curvature;
        public Fisheye fisheye;
        public LightGlow lglow;
        public LightScattering lscatter;
        public MotionBlur motionblur;
        public Levels levels;
        //public HDR hdr;

        public boolean capture() {
            return pp.capture();
        }

        public boolean captureNoClear() {
            return pp.captureNoClear();
        }

        public void render() {
            pp.render();
        }

        public FrameBuffer captureEnd() {
            return pp.captureEnd();
        }

        public void render(FrameBuffer dest) {
            pp.render(dest);
        }

        public void dispose() {
            if (pp != null)
                pp.dispose(false);
        }

    }

    enum RenderType {
        screen(0), screenshot(1), frame(2);

        public int index;

        RenderType(int index) {
            this.index = index;
        }

    }

    void initialize(AssetManager manager);

    void doneLoading(AssetManager manager);

    PostProcessBean getPostProcessBean(RenderType type);

    void resize(int width, int height);

    void resizeImmediate(int width, int height);

    boolean isLightScatterEnabled();
}
