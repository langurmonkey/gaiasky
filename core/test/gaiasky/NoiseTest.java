package gaiasky;


import gaiasky.util.noise.FbmOpts;
import gaiasky.util.noise.INoise;
import gaiasky.util.noise.NoiseSimplex;
import gaiasky.util.noise.Vec3;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

/**
 * Computes the mean and standard deviation of the simplex FBM output
 * distribution, sampled over the unit sphere. These two constants are
 * meant to be baked once (offline, here) and then hardcoded/uploaded as
 * u_continentMean / u_continentStd uniforms, so that the GPU can derive
 * the land/ocean threshold analytically via the inverse error function
 * instead of doing a CPU-side percentile pass at planet-init time.
 * <p>
 * Re-run this whenever you change octaves, persistence, lacunarity,
 * scale, or power in the FbmOpts you use for the continent layer -- the
 * distribution shape depends on those, but NOT on the seed or frequency,
 * so you do not need to re-bake per-planet.
 */
public class NoiseTest {

    // Number of sample points used to estimate the distribution.
    // Higher = more accurate mean/std, at the cost of test runtime.
    private static final int N = 200_000;

    @Test
    public void computeContinentFbmMeanAndStd() {
        INoise noise = new NoiseSimplex();

        FbmOpts opts = new FbmOpts();
        opts.seed = 0.0;
        opts.amplitude = 1.0;
        opts.frequency = 1.0;
        opts.scale = new Vec3(1.0, 1.0, 1.0);
        opts.octaves = 6;
        opts.lacunarity = 2.0;
        opts.persistence = 0.5;
        opts.power = 1.0;
        opts.turbulence = false;
        opts.ridge = false;

        double[] stats = sampleFbmStats(noise, opts, N);
        double mean = stats[0];
        double std = stats[1];
        double min = stats[2];
        double max = stats[3];

        System.out.println("=== Continent FBM distribution stats (N=" + N + ") ===");
        System.out.println(" octaves=" + opts.octaves
                                   + " persistence=" + opts.persistence
                                   + " lacunarity=" + opts.lacunarity
                                   + " power=" + opts.power);
        System.out.println(" mean = " + mean);
        System.out.println(" std  = " + std);
        System.out.println(" min  = " + min);
        System.out.println(" max  = " + max);
        System.out.println();
        System.out.println(" GLSL uniforms to hardcode/upload:");
        System.out.println("   u_continentMean = " + (float) mean + "f;");
        System.out.println("   u_continentStd  = " + (float) std + "f;");

        // Sanity checks: FBM output here should stay within a plausible
        // range and the std should be a sane, non-degenerate positive value.
        Assert.assertTrue("mean out of expected range", mean > -1.0 && mean < 1.0);
        Assert.assertTrue("std should be positive", std > 0.0);
        Assert.assertTrue("std looks degenerate (too small)", std > 1e-4);

        // Cross-check: verify the analytic threshold actually reproduces
        // roughly the target water coverage against the empirical samples.
        verifyThresholdAgainstEmpiricalPercentile(noise, opts, mean, std, 0.7);
        verifyThresholdAgainstEmpiricalPercentile(noise, opts, mean, std, 0.3);
        verifyThresholdAgainstEmpiricalPercentile(noise, opts, mean, std, 0.5);
    }

    /**
     * Samples FBM at N random points on the unit sphere and returns
     * {mean, std, min, max}.
     */
    private double[] sampleFbmStats(INoise noise, FbmOpts opts, int n) {
        Random rnd = new Random(42);
        double sum = 0.0;
        double sumSq = 0.0;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < n; i++) {
            Vec3 p = randomOnUnitSphere(rnd);
            double v = noise.fbm(p, opts);
            sum += v;
            sumSq += v * v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        double mean = sum / n;
        double variance = (sumSq / n) - (mean * mean);
        double std = Math.sqrt(Math.max(variance, 0.0));

        return new double[] { mean, std, min, max };
    }

    /**
     * Uniform random point on the unit sphere (Marsaglia method).
     */
    private Vec3 randomOnUnitSphere(Random rnd) {
        double x1, x2, s;
        do {
            x1 = rnd.nextDouble() * 2.0 - 1.0;
            x2 = rnd.nextDouble() * 2.0 - 1.0;
            s = x1 * x1 + x2 * x2;
        } while (s >= 1.0 || s == 0.0);

        double factor = 2.0 * Math.sqrt(1.0 - s);
        double x = x1 * factor;
        double y = x2 * factor;
        double z = 1.0 - 2.0 * s;
        return new Vec3(x, y, z);
    }

    /**
     * Mirrors the GLSL erfinv-based threshold derivation, to confirm the
     * analytic threshold reproduces the requested water coverage against
     * the actual (empirical) sample distribution.
     */
    private void verifyThresholdAgainstEmpiricalPercentile(INoise noise, FbmOpts opts,
                                                           double mean, double std,
                                                           double waterCoverage) {
        double t = mean + std * Math.sqrt(2.0) * erfInv(2.0 * waterCoverage - 1.0);

        // Re-sample independently (different RNG stream) and measure what
        // fraction actually falls below t.
        Random rnd = new Random(1234);
        int n = 50_000;
        int belowCount = 0;
        for (int i = 0; i < n; i++) {
            Vec3 p = randomOnUnitSphere(rnd);
            double v = noise.fbm(p, opts);
            if (v < t) belowCount++;
        }
        double empiricalCoverage = (double) belowCount / n;

        System.out.println(" target waterCoverage=" + waterCoverage
                                   + " -> threshold t=" + t
                                   + " -> empirical coverage=" + empiricalCoverage);

        // Gaussian approximation error should be small but is not exact,
        // especially in the tails; allow a reasonable tolerance.
        Assert.assertEquals(waterCoverage, empiricalCoverage, 0.05);
    }

    /**
     * Inverse error function approximation (Winitzki), matching the GLSL
     * version used in the shader so the CPU-computed constants and the
     * shader threshold computation stay consistent.
     */
    private double erfInv(double x) {
        double w = -Math.log((1.0 - x) * (1.0 + x));
        double p;
        if (w < 5.0) {
            w -= 2.5;
            p = 2.81022636e-08;
            p = 3.43273939e-07 + p * w;
            p = -3.5233877e-06 + p * w;
            p = -4.39150654e-06 + p * w;
            p = 0.00021858087 + p * w;
            p = -0.00125372503 + p * w;
            p = -0.00417768164 + p * w;
            p = 0.246640727 + p * w;
            p = 1.50140941 + p * w;
        } else {
            w = Math.sqrt(w) - 3.0;
            p = -0.000200214257;
            p = 0.000100950558 + p * w;
            p = 0.00134934322 + p * w;
            p = -0.00367342844 + p * w;
            p = 0.00573950773 + p * w;
            p = -0.0076224613 + p * w;
            p = 0.00943887047 + p * w;
            p = 1.00167406 + p * w;
            p = 2.83297682 + p * w;
        }
        return p * x;
    }
}
