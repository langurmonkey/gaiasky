package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.SSRFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Screen Space Reflections effect. Uses the color, depth, normal and reflection mask
 * textures to implement screen space reflections.
 */
public class SSR extends PostProcessorEffect {
    private final SSRFilter ssr;

    public SSR() {
        ssr = new SSRFilter();
    }

    public void setZfarK(float zfar, float k) {
        ssr.setZfarK(zfar, k);
    }

    @Override
    public void dispose() {
        if (ssr != null) {
            ssr.dispose();
        }
    }

    @Override
    public void rebind() {
        ssr.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);

        ssr.setDepthTexture(main.getDepthBufferTexture());
        ssr.setNormalTexture(main.getNormalBufferTexture());
        ssr.setReflectionTexture(main.getReflectionBufferTexture());

        ssr.setInput(src).setOutput(dest).render();
    }
}
