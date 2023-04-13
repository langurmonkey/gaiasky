package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.ChromaticAberrationFilter;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class ChromaticAberration extends PostProcessorEffect {

    private final ChromaticAberrationFilter filter;

    public ChromaticAberration(float amount) {
        filter = new ChromaticAberrationFilter(amount);
    }

    public void setAberrationAmount(float amount) {
        filter.setAberrationAmount(amount);
    }

    public float getAberrationAmount() {
        return filter.getAberrationAmount();
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void dispose() {
        filter.dispose();
    }
}
