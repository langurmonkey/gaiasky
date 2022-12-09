package gaiasky.util.gdx.contrib.postprocess.filters;

import gaiasky.util.gdx.contrib.utils.ShaderLoader;

public final class Threshold extends Filter<Threshold> {

    public enum Param implements Parameter {
        // @formatter:off
        Texture("u_texture0", 0),
        Threshold("u_threshold", 0);
        // @formatter:on

        private final String mnemonic;
        private final int elementSize;

        Param(String mnemonic, int elementSize) {
            this.mnemonic = mnemonic;
            this.elementSize = elementSize;
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

    public Threshold() {
        super(ShaderLoader.fromFile("screenspace", "threshold"));
        rebind();
    }

    private float threshold = 0;

    public void setThreshold(float threshold) {
        this.threshold = threshold;
        setParam(Param.Threshold, threshold);
    }

    public float getThreshold() {
        return threshold;
    }

    @Override
    protected void onBeforeRender() {
        inputTexture.bind(u_texture0);
    }

    @Override
    public void rebind() {
        setParams(Param.Texture, u_texture0);
        setParam(Param.Threshold, threshold);
        endParams();
    }
}
