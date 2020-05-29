/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.desktop.render.DesktopPostProcessor;
import gaiasky.util.Logger;
import gaiasky.util.gdx.contrib.postprocess.PostProcessor;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.effects.*;

import java.util.*;

public interface IPostProcessor extends Disposable {

    class PostProcessBean {
        protected static Logger.Log logger = Logger.getLogger(PostProcessBean.class);

        public PostProcessor pp;
        public Map<Class<? extends PostProcessorEffect>, List<PostProcessorEffect>> effects = new HashMap<>();

        /**
         * Adds a new non-singleton effect to the post processor
         *
         * @param effect The effect
         */
        public void add(PostProcessorEffect effect) {
            addEffect(effect, false);
        }

        /**
         * Sets the given singleton effect to the post processor. This replaces any previous effect of the same type.
         *
         * @param effect The effect
         */
        public void set(PostProcessorEffect effect) {
            addEffect(effect, true);
        }

        /**
         * Adds a new post-processing effect to this post-processor
         *
         * @param effect    The effect
         * @param singleton Whether this must be a singleton (overwrites existing)
         */
        private void addEffect(PostProcessorEffect effect, boolean singleton) {
            if (effects != null) {
                List<PostProcessorEffect> l = effects.get(effect.getClass());
                if (l != null) {
                    if (singleton) {
                        for (PostProcessorEffect ppe : l)
                            pp.removeEffect(ppe);
                        l.clear();
                    }
                    l.add(effect);
                } else {
                    l = new ArrayList<>();
                    l.add(effect);
                    effects.put(effect.getClass(), l);
                }
                pp.addEffect(effect);
            } else {
                logger.error("Effects list not initialized!");
            }
        }

        /**
         * Gets the first effect of the given type
         *
         * @param clazz The class
         * @return The effect
         */
        public PostProcessorEffect get(Class<? extends PostProcessorEffect> clazz) {
            List<PostProcessorEffect> l = effects.get(clazz);
            if (l != null) {
                return l.get(0);
            }
            return null;
        }

        /**
         * Gets all effects of the given type
         *
         * @param clazz The class
         * @return The list of effects
         */
        public List<PostProcessorEffect> getAll(Class<? extends PostProcessorEffect> clazz) {
            List<PostProcessorEffect> l = effects.get(clazz);
            if (l != null) {
                return l;
            }
            return null;
        }

        /**
         * Removes all effects from the given class
         *
         * @param clazz The class
         */
        public void remove(Class<? extends PostProcessorEffect> clazz) {
            List<PostProcessorEffect> l = getAll(clazz);
            if (l != null) {
                for (PostProcessorEffect ppe : l) {
                    ppe.setEnabled(false);
                    pp.removeEffect(ppe);
                }
                l.clear();
                effects.remove(clazz);
            }
        }

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

        public void dispose(boolean cleanAllBuffers) {
            if (pp != null) {
                pp.dispose(cleanAllBuffers);
                if (effects != null) {
                    Set<Class<? extends PostProcessorEffect>> keys = effects.keySet();
                    for (Class<? extends PostProcessorEffect> key : keys) {
                        List<PostProcessorEffect> l = effects.get(key);
                        if (l != null)
                            for (PostProcessorEffect effect : l)
                                effect.dispose();
                    }
                }
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
