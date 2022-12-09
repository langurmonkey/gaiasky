package gaiasky.util.gdx.contrib.postprocess.filters;

import com.badlogic.gdx.math.Vector2;
import gaiasky.util.gdx.contrib.utils.ShaderLoader;

/**
 * Fast approximate anti-aliasing filter.
 */
public final class FxaaFilter extends Filter<FxaaFilter> {
    private final Vector2 viewportInverse;

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        ViewportInverse("u_viewportInverse", 2);
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

    /**
     * Creates an FXAA filter with the given viewport size and quality.
     *
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     * @param quality        The quality in [0,1,2], from worst to best
     */
    public FxaaFilter(float viewportWidth, float viewportHeight, int quality) {
        this(new Vector2(viewportWidth, viewportHeight), quality);
    }

    /**
     * Creates an FXAA filter with the given viewport size and maximum quality (2)
     *
     * @param viewportWidth  The viewport width in pixels.
     * @param viewportHeight The viewport height in pixels.
     */
    public FxaaFilter(int viewportWidth, int viewportHeight) {
        this(new Vector2((float) viewportWidth, (float) viewportHeight), 2);
    }

    /**
     * Creates an FXAA filter with the given viewport size and quality.
     *
     * @param viewportSize The viewport size in pixels.
     * @param quality      The quality in [0,1,2], from worst to best
     */
    public FxaaFilter(Vector2 viewportSize, int quality) {
        super(ShaderLoader.fromFile("screenspace", "fxaa", "#define FXAA_PRESET " + (quality % 3 + 3)));
        this.viewportInverse = viewportSize;
        this.viewportInverse.x = 1f / this.viewportInverse.x;
        this.viewportInverse.y = 1f / this.viewportInverse.y;
        rebind();
    }

    /**
     * Updates the FXAA quality setting.
     * @param quality      The quality in [0,1,2], from worst to best
     */
    public void updateQuality(int quality) {
        super.updateProgram(ShaderLoader.fromFile("screenspace", "fxaa", "#define FXAA_PRESET " + (quality % 3 + 3)));
    }

    public void setViewportSize(float width, float height) {
        this.viewportInverse.set(1f / width, 1f / height);
        setParam(Param.ViewportInverse, this.viewportInverse);
    }

    public Vector2 getViewportSize() {
        return viewportInverse;
    }

    @Override
    public void rebind() {
        // reimplement super to batch every parameter
        setParams(Param.Texture, u_texture0);
        setParams(Param.ViewportInverse, viewportInverse);
        endParams();
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }
}
