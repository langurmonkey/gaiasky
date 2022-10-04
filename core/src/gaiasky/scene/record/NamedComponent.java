package gaiasky.scene.record;

import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.math.MathUtilsd;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public abstract class NamedComponent implements IComponent, Disposable {
    protected String name;

    @Override
    public void initialize(String name) {
        if (name != null)
            this.name = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    protected Map<String, Object> createModelParameters(long quality, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("quality", quality);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected double gaussian(Random rand, double mean, double sigma) {
        return rand.nextGaussian() * sigma + mean;
    }

    protected double gaussian(Random rand, double mean, double sigma, double min) {
        return Math.max(min, rand.nextGaussian() * sigma + mean);
    }

    protected double gaussian(Random rand, double mean, double sigma, double min, double max) {
        return MathUtilsd.clamp(rand.nextGaussian() * sigma + mean, min, max);
    }
}
