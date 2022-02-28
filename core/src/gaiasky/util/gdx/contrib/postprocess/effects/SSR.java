package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.filters.RaymarchingFilter;
import gaiasky.util.gdx.contrib.postprocess.filters.SSRFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Screen Space Reflections effect. Uses the color, depth, normal and reflection mask
 * textures to implement screen space reflections.
 */
public class SSR extends Raymarching {

    public SSR(float viewportWidth, float viewportHeight) {
        super();
        filter = new SSRFilter((int) viewportWidth, (int) viewportHeight);
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Get depth buffer texture from main frame buffer
        //filter.setDepthTexture(main.getDepthBufferTexture());
        // Normal buffer
        ((SSRFilter) filter).setNormalTexture(main.getNormalBufferTexture());
        // Reflection mask
        ((SSRFilter) filter).setReflectionTexture(main.getReflectionBufferTexture());
        // Position buffer
        ((SSRFilter) filter).setPositionTexture(main.getPositionBufferTexture());
        // Set input, output and render
        filter.setInput(src).setOutput(dest).render();
    }
}
