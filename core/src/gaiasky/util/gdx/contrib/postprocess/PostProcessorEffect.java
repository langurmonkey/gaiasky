/*******************************************************************************
 * Copyright 2012 bmanuel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package gaiasky.util.gdx.contrib.postprocess;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * This interface defines the base class for the concrete implementation of post-processor effects. An effect is considered
 * enabled by default.
 *
 * @author bmanuel
 */
public abstract class PostProcessorEffect implements Disposable {
    protected boolean enabled = true;
    protected boolean enabledInCubemap = true;
    protected boolean enabledInVR = true;

    /**
     * Concrete objects shall be responsible to recreate or rebind its own resources whenever its needed, usually when the OpenGL
     * context is lost. E.g., frame buffer textures should be updated and shader parameters should be re-uploaded/rebound.
     */
    public abstract void rebind();

    /** Concrete objects shall implement its own rendering, given the source and destination buffers. */
    public abstract void render(final FrameBuffer src, final FrameBuffer dest, final GaiaSkyFrameBuffer main);

    /** Whether or not this effect is enabled and should be processed */
    public boolean isEnabled() {
        return enabled;
    }

    /** Sets this effect enabled or not */
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

    /** Convenience method to forward the call to the PostProcessor object while still being a non-publicly accessible method */
    protected void restoreViewport(FrameBuffer dest) {
        if (PostProcessor.currentPostProcessor != null)
            PostProcessor.currentPostProcessor.restoreViewport(dest);
    }
}
