/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public abstract class PostProcessorEffect implements Disposable {
    protected boolean enabled = true;
    protected boolean enabledInCubemap = true;
    protected boolean enabledInVR = true;

    /**
     * Array of disposables.
     **/
    protected Array<Disposable> disposables = new Array<>(1);
    protected boolean disposed = false;

    /**
     * Concrete objects shall be responsible to recreate or rebind its own resources whenever its needed, usually when the OpenGL
     * context is lost. E.g., frame buffer textures should be updated and shader parameters should be re-uploaded/rebound.
     */
    public abstract void rebind();

    /**
     * Concrete objects shall implement its own rendering, given the source and destination buffers.
     */
    public abstract void render(final FrameBuffer src, final FrameBuffer dest, final GaiaSkyFrameBuffer main);

    /**
     * Whether this effect is enabled and should be processed.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets this effect enabled or not.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabledInCubemap() {
        return enabledInCubemap;
    }

    public boolean isEnabledInVR() {
        return enabledInVR;
    }

    public void setEnabledOptions(boolean enabledInCubemap, boolean enabledInVR) {
        this.enabledInCubemap = enabledInCubemap;
        this.enabledInVR = enabledInVR;
    }

    public void setEnabledInCubemap(boolean enabled) {
        this.enabledInCubemap = enabled;
    }

    public void setEnabledInVR(boolean enabled) {
        this.enabledInVR = enabled;
    }

    /**
     * Convenience method to forward the call to the PostProcessor object while still being a non-publicly accessible method.
     */
    protected void restoreViewport(FrameBuffer dest) {
        if (PostProcessor.currentPostProcessor != null)
            PostProcessor.currentPostProcessor.restoreViewport(dest);
    }

    /**
     * Default implementation uses the resources in the disposables list.
     **/
    public void dispose() {
        if (!disposed) {
            for (Disposable disposable : disposables) {
                if (disposable != null) {
                    disposable.dispose();
                }
            }
            disposables.clear();
            disposed = true;
        }
    }
}
