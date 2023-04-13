package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Simple chromatic aberration filter.
 */
public final class ChromaticAberrationFilter extends Filter<ChromaticAberrationFilter> {
    private float aberrationAmount;

    /**
     * Creates a crhomatic aberration filter with the given aberration amount.
     *
     * @param amount      The aberration amount in [0..1].
     */
    public ChromaticAberrationFilter(float amount) {
        super(ShaderLoader.fromFile("screenspace", "chromaticaberration"));
        this.aberrationAmount = amount;
        rebind();
    }

    /**
     * Updates the chromatic aberration amount.
     *
     * @param amount      The aberration amount in [0..1].
     */
    public void setAberrationAmount(float amount) {
        this.aberrationAmount = MathUtils.clamp(amount, 0f, 0.5f);
        setParam(Param.AberrationAmount, aberrationAmount);
        super.updateProgram(ShaderLoader.fromFile("screenspace", "chromaticaberration"));
    }

    public float getAberrationAmount() {
        return aberrationAmount;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.AberrationAmount, aberrationAmount);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        AberrationAmount("u_aberrationAmount", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int arrayElementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = arrayElementSize;
        }

        @Override
        public String mnemonic() {
            return this.mnemonic;
        }

        @Override
        public int arrayElementSize() {
            return this.elementSize;
        }
    }
}
