package gaiasky.scenegraph.component;

import com.sudoplay.joise.module.ModuleBasisFunction;
import com.sudoplay.joise.module.ModuleFractal;
import gaiasky.util.math.MathUtilsd;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class NamedComponent implements IComponent {
    protected String name;
    protected Long id;

    @Override
    public void initialize(String name, Long id) {
        if (name != null)
            this.name = name.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        this.id = id;
    }

    protected Map<String, Object> createModelParameters(long quality, double diameter, boolean flip) {
        Map<String, Object> params = new HashMap<>();
        params.put("quality", quality);
        params.put("diameter", diameter);
        params.put("flip", flip);
        return params;
    }

    protected NoiseComponent randomizeNoiseComponent(Random rand) {
        NoiseComponent nc = new NoiseComponent();
        // Seed
        nc.setSeed(rand.nextLong());
        // Size
        if (rand.nextBoolean()) {
            // Single size
            nc.setSize(Math.abs(gaussian(rand, 0.5, 1.5, 0.01)));
        } else {
            // Different sizes
            double baseSize = Math.abs(gaussian(rand, 0.5, 0.5, 0.05));
            nc.setSize(new double[] { baseSize + Math.abs(gaussian(rand, 0.0, 0.2)), baseSize + Math.abs(gaussian(rand, 0.5, 0.2)), baseSize + Math.abs(gaussian(rand, 0.0, 0.2)) });
        }
        // Type (all but WHITE)
        nc.setType(ModuleBasisFunction.BasisType.values()[rand.nextInt(4)].name());
        // Fractal type
        nc.setFractaltype(ModuleFractal.FractalType.values()[rand.nextInt(6)].name());
        // Frequency
        nc.setFrequency(gaussian(rand, 2.5, 5.0, 1.0));
        // Octaves [1,9]
        nc.setOctaves(Math.abs(rand.nextLong()) % 8 + 1L);
        // Range
        double minRange = rand.nextBoolean() ? gaussian(rand, -1.0, 0.4) : 0.0;
        double maxRange = gaussian(rand, 0.8, 0.2);
        if (minRange >= maxRange) {
            minRange = maxRange - 1.0;
        }
        nc.setRange(new double[] { minRange, maxRange });
        // Power
        nc.setPower(gaussian(rand, 5.0, 4.0, 0.2));

        return nc;
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
