package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.XBRZUpscale;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class XBRZ extends PostProcessorEffect {

    private XBRZUpscale filter;

    public XBRZ() {
        super();
        filter = new XBRZUpscale();
    }

    public void setInputSize(int w, int h) {
        filter.setInputSize(w, h);
    }
    public void setOutputSize(int w, int h) {
        filter.setOutputSize(w, h);
    }


    @Override
    public void dispose() {
        if (filter != null) {
            filter.dispose();
            filter = null;
        }
    }

    @Override
    public void rebind() {
        if (filter != null) {
            filter.rebind();
        }
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        // Set input, output and render
        filter.setInput(src).setOutput(dest).render();
    }
}

