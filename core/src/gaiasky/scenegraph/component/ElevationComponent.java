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

import java.util.stream.IntStream;

/**
 * Contains the parameters and functions for procedural elevation
 */
public class ElevationComponent {
    private static final Log logger = Logger.getLogger(ElevationComponent.class);

    // Size of the sampled area will be (noiseSize*2 x noiseSize)
    private double noiseSize = 10.0;
    private double noisePower = 1.0;
    private int octaves = 4;
    private double frequency = 2.34;
    private double[] noiseRange = new double[] { 0.0, 1.0 };
    private BasisType noiseType = BasisType.SIMPLEX;
    private FractalType fractalType = FractalType.RIDGEMULTI;

    // Parallel processing, since noise modules are not thread-safe
    private Module[] elevationNoise, moistureNoise;

    public ElevationComponent() {
        super();
    }

    private Module getModule(long seed) {
        ModuleFractal fractal = new ModuleFractal();
        fractal.setAllSourceBasisTypes(noiseType);
        fractal.setAllSourceInterpolationTypes(InterpolationType.CUBIC);
        fractal.setNumOctaves(octaves);
        fractal.setFrequency(frequency);
        fractal.setType(fractalType);
        fractal.setSeed(seed);

        ModuleAutoCorrect autoCorrect = new ModuleAutoCorrect();
        autoCorrect.setSource(fractal);
        autoCorrect.setRange(noiseRange[0], noiseRange[1]);
        autoCorrect.setSamples(10000);
        autoCorrect.calculate3D();

        ModuleClamp clamp = new ModuleClamp();
        clamp.setSource(autoCorrect);
        clamp.setRange(0.0, 1.0);

        ModulePow pow = new ModulePow();
        pow.setSource(clamp);
        pow.setPower(noisePower);

        ModuleScaleDomain scaleDomain = new ModuleScaleDomain();
        scaleDomain.setSource(clamp);
        scaleDomain.setScaleX(noiseSize);
        scaleDomain.setScaleY(noiseSize);
        scaleDomain.setScaleZ(noiseSize);

        return scaleDomain;
    }

    final int N_GEN = Settings.settings.performance.getNumberOfThreads();
    private void initNoise(long seed, int N, int M) {
        elevationNoise = new Module[N_GEN];
        moistureNoise = new Module[N_GEN];
        for(int i =0; i < N_GEN; i++){
            elevationNoise[i] = getModule(seed);
            moistureNoise[i] = getModule(seed + 23443);
        }
    }

    public Trio<float[][], float[][], Pixmap> generateElevation(int N, int M, float heightScale, long seed) {
        // Construct RAM height map from noise algorithms
        logger.info("Generating procedural " + N + "x" + M + " elevation data");
        initNoise(seed, N, M);
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
            IntStream.range(0, N).parallel().forEach(x->{
                float theta = x * theta_step;
                double n, m;
                synchronized(elevationNoise[x % N_GEN]) {
                    n = elevationNoise[x % N_GEN].get(cosPhi * Math.cos(theta), cosPhi * Math.sin(theta), sinPhi);
                    m = moistureNoise[x % N_GEN].get(cosPhi * Math.cos(theta), cosPhi * Math.sin(theta), sinPhi);
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

    public void setNoisetype(String noiseType) {
        try {
            this.noiseType = BasisType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.noiseType = BasisType.SIMPLEX;
        }
    }

    public void setFractaltype(String fractalType) {
        try {
            this.fractalType = FractalType.valueOf(fractalType.toUpperCase());
        } catch (Exception e) {
            this.fractalType = FractalType.RIDGEMULTI;
        }
    }

    /**
     * Only if height is {@link MaterialComponent#GEN_HEIGHT_KEYWORD}
     *
     * @param noiseSize Size of the sampling area
     */
    public void setNoisesize(Double noiseSize) {
        this.noiseSize = noiseSize;
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

    public void setNoisepower(Double power) {
        this.noisePower = power;
    }

    public void setNoiserange(double[] range) {
        this.noiseRange = range;
    }
}
