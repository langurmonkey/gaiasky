package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

/**
 * Just copies the texture in the input to the output. Useful to visualize
 * secondary render targets in MRT frame buffers.
 */
public class DrawTexture extends PostProcessorEffect {
    private final Copy copy;

    public DrawTexture() {
        copy = new Copy();
    }

    @Override
    public void dispose() {
        if (copy != null) {
            copy.dispose();
        }
    }

    @Override
    public void rebind() {
        copy.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        copy.setInput(src).setOutput(dest).render();
    }
}
