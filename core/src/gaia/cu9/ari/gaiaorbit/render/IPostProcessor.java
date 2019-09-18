/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.PostProcessor;
import gaia.cu9.ari.gaiaorbit.util.gdx.contrib.postprocess.effects.*;

public interface IPostProcessor extends Disposable {
    class PostProcessBean {
        public PostProcessor pp;
        public CameraMotion camblur;
        public Bloom bloom;
        public Antialiasing antialiasing;
        public LensFlare2 lens;
        public Curvature curvature;
        public Fisheye fisheye;
        public LightGlow lightglow;
        public AccumulationBlur accumblur;
        public Levels levels;
        public DepthBuffer depthBuffer;

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

        public void dispose(boolean cleanAllBuffers){
            if (pp != null) {
                pp.dispose(cleanAllBuffers);
                if (bloom != null)
                    bloom.dispose();
                if (antialiasing != null)
                    antialiasing.dispose();
                if (lens != null)
                    lens.dispose();
                if (curvature != null)
                    curvature.dispose();
                if (fisheye != null)
                    fisheye.dispose();
                if (lightglow != null)
                    lightglow.dispose();
                if (accumblur != null)
                    accumblur.dispose();
                if (levels != null)
                    levels.dispose();
                if(camblur != null)
                    camblur.dispose();
                if (depthBuffer != null)
                    depthBuffer.dispose();
            }
        }

        public void dispose() {
            dispose(true);
        }

    }

    enum RenderType {
        screen(0),
        screenshot(1),
        frame(2);

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
