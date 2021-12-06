/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph.component;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.sudoplay.joise.module.Module;
import com.sudoplay.joise.module.*;
import com.sudoplay.joise.module.ModuleBasisFunction.BasisType;
import com.sudoplay.joise.module.ModuleBasisFunction.InterpolationType;
import com.sudoplay.joise.module.ModuleFractal.FractalType;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Trio;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Contains the parameters and functions for procedural elevation
 */
public class NoiseComponent extends NamedComponent {
    private static final Log logger = Logger.getLogger(NoiseComponent.class);

    public double[] scale = new double[] { 1.0, 1.0, 1.0 };
    public double power = 1.0;
    public int octaves = 4;
    public double frequency = 2.34;
    public double[] range = new double[] { 0.0, 1.0 };
    public BasisType type = BasisType.SIMPLEX;
    public FractalType fractalType = FractalType.RIDGEMULTI;
    public long seed = 0L;

    // Parallel processing, since noise modules are not thread-safe
    private Module[] baseNoise, secondaryNoise;

    public NoiseComponent() {
        super();
    }

    private Module getNoiseModule(long seed, boolean secondary) {
        ModuleFractal fractal = new ModuleFractal();
        fractal.setAllSourceBasisTypes(type);
        fractal.setAllSourceInterpolationTypes(InterpolationType.CUBIC);
        fractal.setNumOctaves(octaves);
        fractal.setFrequency(frequency);
        fractal.setType(fractalType);
        fractal.setSeed(seed);

        ModuleAutoCorrect autoCorrect = new ModuleAutoCorrect();
        autoCorrect.setSource(fractal);
        if (secondary) {
            autoCorrect.setRange(range[0], range[1]);
        } else {
            autoCorrect.setRange(-0.5, 1.0);
        }
        autoCorrect.setSamples(10000);
        autoCorrect.calculate3D();

        ModuleClamp clamp = new ModuleClamp();
        clamp.setSource(autoCorrect);
        clamp.setRange(0.0, 1.0);

        ModulePow pow = new ModulePow();
        pow.setSource(clamp);
        pow.setPower(power);

        ModuleScaleDomain scaleDomain = new ModuleScaleDomain();
        scaleDomain.setSource(pow);
        scaleDomain.setScaleX(scale[0]);
        scaleDomain.setScaleY(scale[1]);
        scaleDomain.setScaleZ(scale[2]);

        return scaleDomain;
    }

    final int N_GEN = Settings.settings.performance.getNumberOfThreads();

    private void initNoise(long seed, boolean secondary) {
        baseNoise = new Module[N_GEN];
        if (secondary)
            secondaryNoise = new Module[N_GEN];
        for (int i = 0; i < N_GEN; i++) {
            baseNoise[i] = getNoiseModule(seed, true);
            if (secondary)
                secondaryNoise[i] = getNoiseModule(seed + 23443, false);
        }
    }

    public Pixmap generateData(int N, int M) {
        initNoise(seed, false);
        Pixmap pixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);

        // Sample 3D noise using spherical coordinates on the surface of the sphere
        double piTimesTwo = 2.0 * Math.PI;
        double piOverTwo = Math.PI / 2.0;
        double phi = piOverTwo * -1.0;
        int y = 0;

        double theta_step = piTimesTwo / N;
        while (phi <= piOverTwo) {
            final double cosPhi = Math.cos(phi);
            final double sinPhi = Math.sin(phi);
            final int yf = y;
            IntStream.range(0, N).parallel().forEach(x -> {
                double theta = x * theta_step;
                double n;
                synchronized (baseNoise[x % N_GEN]) {
                    n = baseNoise[x % N_GEN].get(cosPhi * Math.cos(theta), cosPhi * Math.sin(theta), sinPhi);
                }

                float nf = (float) n;
                pixmap.drawPixel(x, yf, Color.rgba8888(nf, nf, nf, 1f));
            });
            phi += (Math.PI / (M - 1));
            y += 1;
        }

        return pixmap;
    }

    public Trio<float[][], float[][], Pixmap> generateElevation(int N, int M, float heightScale) {
        // Construct RAM height map from noise algorithms
        initNoise(seed, true);
        Pixmap pixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
        float[][] elevation = new float[N][M];
        float[][] moisture = new float[N][M];

        // Sample 3D noise using spherical coordinates on the surface of the sphere
        float pi_times_two = (float) (2 * Math.PI);
        float pi_div_two = (float) (Math.PI / 2.0f);
        float phi = pi_div_two * -1.0f;
        int y = 0;

        float theta_step = pi_times_two / N;
        while (phi <= pi_div_two) {
            final double cosPhi = Math.cos(phi);
            final double sinPhi = Math.sin(phi);
            final int yf = y;
            IntStream.range(0, N).parallel().forEach(x -> {
                float theta = x * theta_step;
                double n, m;
                synchronized (baseNoise[x % N_GEN]) {
                    n = baseNoise[x % N_GEN].get(cosPhi * Math.cos(theta), cosPhi * Math.sin(theta), sinPhi);
                    m = secondaryNoise[x % N_GEN].get(cosPhi * Math.cos(theta), cosPhi * Math.sin(theta), sinPhi);
                }
                elevation[x][yf] = (float) (n * heightScale);
                moisture[x][yf] = (float) m;

                float nf = (float) n;
                // Pixamp
                pixmap.drawPixel(x, yf, Color.rgba8888(nf, nf, nf, 1f));
            });
            phi += (Math.PI / (M - 1));
            y += 1;
        }

        return new Trio<>(elevation, moisture, pixmap);
    }

    public void setType(String noiseType) {
        try {
            this.type = BasisType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.type = BasisType.SIMPLEX;
        }
    }

    public void setFractaltype(String fractalType) {
        try {
            this.fractalType = FractalType.valueOf(fractalType.toUpperCase());
        } catch (Exception e) {
            this.fractalType = FractalType.RIDGEMULTI;
        }
    }

    public void setScale(Double scale) {
        this.scale[0] = scale;
        this.scale[1] = scale;
        this.scale[2] = scale;
    }

    public void setScale(double[] noiseScale) {
        this.scale = noiseScale;
    }

    /**
     * Sets the number of octaves
     *
     * @param octaves The octaves
     */
    public void setOctaves(Long octaves) {
        this.octaves = Math.min(9, octaves.intValue());
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public void setPower(Double power) {
        this.power = power;
    }

    public void setRange(double[] range) {
        this.range = range;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public void copyFrom(NoiseComponent other) {
        this.seed = other.seed;
        this.scale = Arrays.copyOf(other.scale, other.scale.length);
        this.type = other.type;
        this.fractalType = other.fractalType;
        this.frequency = other.frequency;
        this.octaves = other.octaves;
        this.range = Arrays.copyOf(other.range, other.scale.length);
        this.power = other.power;
    }

    public void randomizeAll(Random rand) {
        randomizeAll(rand, rand.nextBoolean());
    }

    public void randomizeAll(Random rand, boolean rocky) {
        // Seed
        setSeed(rand.nextLong());
        // Size
        double baseSize = Math.abs(gaussian(rand, 1.0, 0.4, 0.05));
        if (rand.nextBoolean()) {
            // Single scale
            setScale(baseSize);
        } else {
            // Different scales
            setScale(new double[] { baseSize + Math.abs(gaussian(rand, 0.0, 0.2)), baseSize + Math.abs(gaussian(rand, 0.0, 0.2)), baseSize + Math.abs(gaussian(rand, 0.4, 0.2)) });
        }
        // Type (all but WHITE)
        setType(ModuleBasisFunction.BasisType.values()[rand.nextInt(4)].name());
        // Fractal type
        setFractaltype(ModuleFractal.FractalType.values()[rand.nextInt(6)].name());
        // Frequency
        setFrequency(gaussian(rand, 2.5, 5.0, 1.0));
        // Octaves [1,9]
        setOctaves(Math.abs(rand.nextLong()) % 8 + 1L);
        // Range
        double minRange = rocky ? 0.1 : gaussian(rand, -0.5, 0.3);
        double maxRange = minRange + 1.0 + rand.nextDouble() * 0.3;
        setRange(new double[] { minRange, maxRange });
        // Power
        setPower(rocky ? 1.0 : gaussian(rand, 5.0, 3.0, 0.2));
    }

    public void print(Log log) {
        log.debug("Seed: " + seed);
        log.debug("Scale: " + Arrays.toString(scale));
        log.debug("Noise type: " + type);
        log.debug("Fractal type: " + fractalType);
        log.debug("Frequency: " + frequency);
        log.debug("Octaves: " + octaves);
        log.debug("Range: " + Arrays.toString(range));
        log.debug("Power: " + power);
    }
}
