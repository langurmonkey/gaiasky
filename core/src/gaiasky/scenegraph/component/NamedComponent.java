package gaiasky.scenegraph.component;

import gaiasky.util.math.MathUtilsd;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NamedComponent implements IComponent {
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
