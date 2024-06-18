/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import com.sudoplay.joise.module.Module;
import com.sudoplay.joise.module.*;
import com.sudoplay.joise.module.ModuleBasisFunction.BasisType;
import com.sudoplay.joise.module.ModuleBasisFunction.InterpolationType;
import com.sudoplay.joise.module.ModuleFractal.FractalType;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.postprocess.effects.Noise;
import gaiasky.render.postprocess.effects.SurfaceGen;
import gaiasky.render.postprocess.filters.NoiseFilter;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import gaiasky.util.Trio;
import net.jafama.FastMath;
import org.lwjgl.opengl.GL30;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

public class NoiseComponent extends NamedComponent {
    final int N_GEN = Settings.settings.performance.getNumberOfThreads();
    public double[] scale = new double[]{1.0, 1.0, 1.0};
    public double power = 1.0;
    public int octaves = 4;
    public double frequency = 2.34;
    public double persistence = 0.5;
    public double lacunarity = 2.0;
    public double[] range = new double[]{0.0, 1.0};
    public BasisType type = BasisType.SIMPLEX;
    public FractalType fractalType = FractalType.RIDGEMULTI;
    public long seed = 0L;
    // Use array for parallel processing.
    // Joise modules are not thread-safe.
    private Module[] baseNoise, secondaryNoise;

    private static boolean DEBUG_UI_VIEW = true;

    public NoiseComponent() {
        super();
    }

    private Module getNoiseModule(long seed, boolean secondary) {
        ModuleFractal fractal = new ModuleFractal();
        fractal.setAllSourceBasisTypes(type);
        fractal.setAllSourceInterpolationTypes(InterpolationType.CUBIC);
        fractal.setNumOctaves(octaves);
        fractal.setFrequency(frequency);
        fractal.setLacunarity(lacunarity);
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

    public synchronized Pixmap generateData(int N, int M, float[] rgba, String name) {
        initNoise(seed, false);
        Pixmap pixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);

        // Sample 3D noise using spherical coordinates on the surface of the sphere
        double piTimesTwo = 2.0 * FastMath.PI;
        double piDivTwo = FastMath.PI / 2.0;

        double phiStep = FastMath.PI / (M - 1);
        double thetaStep = piTimesTwo / N;
        IntStream.range(0, M).forEach(y -> {
            double phi = -piDivTwo + y * phiStep;
            final double cosPhi = FastMath.cos(phi);
            final double sinPhi = FastMath.sin(phi);
            final int yf = y;
            IntStream.range(0, N).parallel().forEach(x -> {
                double theta = x * thetaStep;
                double n = 0;
                int idx = x % N_GEN;
                if (baseNoise[idx] != null) {
                    synchronized (baseNoise[idx]) {
                        n = baseNoise[idx].get(cosPhi * FastMath.cos(theta), cosPhi * FastMath.sin(theta), sinPhi);
                    }
                }

                float nf = (float) n;
                float alpha = rgba[3];
                pixmap.drawPixel(x, yf, Color.rgba8888(nf * rgba[0] * alpha, nf * rgba[1] * alpha, nf * rgba[2] * alpha, 1f));
            });

            // Progress bar
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, (float) y / (float) (M - 1));
        });
        // Force end
        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);

        return pixmap;
    }

    private FrameBuffer createFrameBuffer(int N, int M, int numColorTargets) {
        GLFrameBuffer.FrameBufferBuilder builder = new GLFrameBuffer.FrameBufferBuilder(N, M);

        for(int i =0; i < numColorTargets; i++) {
            builder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        }

        return builder.build();
    }


    private float toFloatSeed(long seed) {
        var s = Long.toString(seed);
        return (float) (seed / FastMath.pow(10L, s.length()));
    }

    public void generateNoiseGPU(int N, int M, String biomeLut, float biomeHueShift) {
        // Height
        var fbHeight = createFrameBuffer(N, M, 1);
        float fSeed = toFloatSeed(seed);

        Noise heightNoise = new Noise(N, M);
        heightNoise.setScale(scale);
        heightNoise.setType(NoiseFilter.NoiseType.SIMPLEX);
        heightNoise.setSeed(fSeed);
        heightNoise.setOctaves(octaves);
        heightNoise.setFrequency(frequency);
        heightNoise.setPersistence(persistence);
        heightNoise.setLacunarity(lacunarity);
        heightNoise.setPower(power);
        heightNoise.setTurbulence(true);
        heightNoise.setRidge(true);
        fbHeight.begin();
        heightNoise.render(null, fbHeight);
        fbHeight.end();

        // Moisture
        var fbMoist = createFrameBuffer(N, M, 1);

        Noise moistureNoise = new Noise(N, M);
        moistureNoise.setScale(scale);
        moistureNoise.setType(NoiseFilter.NoiseType.PERLIN);
        moistureNoise.setSeed(fSeed + 2.023f);
        moistureNoise.setOctaves(octaves);
        heightNoise.setFrequency(frequency);
        heightNoise.setPersistence(persistence);
        heightNoise.setLacunarity(lacunarity);
        moistureNoise.setPower(power);
        moistureNoise.setRange(-0.2f, 1.0f);
        moistureNoise.setTurbulence(true);
        moistureNoise.setRidge(true);
        fbMoist.begin();
        moistureNoise.render(null, fbMoist);
        fbMoist.end();

        // Gen surface.
        Texture lut = new Texture(Settings.settings.data.dataFileHandle(biomeLut));
        var fbSurface = createFrameBuffer(N, M, 3);

        SurfaceGen surfaceGen = new SurfaceGen();
        surfaceGen.setMoistureTexture(fbMoist.getColorBufferTexture());
        surfaceGen.setLutTexture(lut);
        surfaceGen.setLutHueShift(biomeHueShift);
        fbSurface.begin();
        surfaceGen.render(fbHeight, fbSurface);
        fbSurface.end();

        if (DEBUG_UI_VIEW) {

            // Create UI views.
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Height", fbHeight.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Moisture", fbMoist.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Diffuse", fbSurface.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Specular", fbSurface.getTextureAttachments().get(1), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Normal", fbSurface.getTextureAttachments().get(2), 1f);
            DEBUG_UI_VIEW = false;
        }

    }

    public synchronized Trio<float[][], float[][], Pixmap> generateElevation(int N, int M, float heightScale, String biomeLut, float biomeHueShift, String name) {
        // Generate in GPU.
        //GaiaSky.postRunnable(() -> generateNoiseGPU(N, M, biomeLut, biomeHueShift));

        // Construct RAM height map from noise algorithms
        initNoise(seed, true);
        Pixmap pixmap = new Pixmap(N, M, Pixmap.Format.RGBA8888);
        float[][] elevation = new float[N][M];
        float[][] moisture = new float[N][M];

        // Sample 3D noise using spherical coordinates on the surface of the sphere
        float piTimesTwo = (float) (2 * FastMath.PI);
        float piDivTwo = (float) (Math.PI / 2.0f);

        float thetaStep = piTimesTwo / N;
        float phiStep = (float) (Math.PI / (M - 1));
        IntStream.range(0, M).forEach(y -> {
            double phi = -piDivTwo + y * phiStep;
            final double cosPhi = FastMath.cos(phi);
            final double sinPhi = FastMath.sin(phi);
            final int yf = y;
            IntStream.range(0, N).parallel().forEach(x -> {
                float theta = x * thetaStep;
                double n, m;
                synchronized (baseNoise[x % N_GEN]) {
                    n = baseNoise[x % N_GEN].get(cosPhi * FastMath.cos(theta), cosPhi * FastMath.sin(theta), sinPhi);
                    m = secondaryNoise[x % N_GEN].get(cosPhi * FastMath.cos(theta), cosPhi * FastMath.sin(theta), sinPhi);
                }
                elevation[x][yf] = (float) (n * heightScale);
                moisture[x][yf] = (float) m;

                float nf = (float) n;
                // Create pixmap.
                pixmap.drawPixel(x, yf, Color.rgba8888(nf, nf, nf, 1f));
            });

            // Progress bar.
            EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, (float) y / (float) (M - 1));
        });
        // Force end.
        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);
        return new Trio<>(elevation, moisture, pixmap);
    }

    public void setType(String noiseType) {
        try {
            this.type = BasisType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.type = BasisType.SIMPLEX;
        }
    }

    public void setFractalType(String fractalType) {
        try {
            this.fractalType = FractalType.valueOf(fractalType.toUpperCase());
        } catch (Exception e) {
            this.fractalType = FractalType.RIDGEMULTI;
        }
    }

    public void setFractaltype(String fractalType) {
        setFractalType(fractalType);
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
        this.octaves = FastMath.min(9, octaves.intValue());
    }

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public void setLacunarity(Double lacunarity) {
        this.lacunarity = lacunarity;
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
        this.lacunarity = other.lacunarity;
        this.octaves = other.octaves;
        this.range = Arrays.copyOf(other.range, other.scale.length);
        this.power = other.power;
    }

    public void randomizeAll(Random rand) {
        randomizeAll(rand, rand.nextBoolean(), false);
    }

    public void randomizeAll(Random rand, boolean rocky, boolean clouds) {
        // Seed
        setSeed(rand.nextLong());
        // Size
        double baseSize = FastMath.abs(gaussian(rand, 1.0, 1.0, 0.05));
        if (clouds) {
            // XY small, Z large
            setScale(new double[]{FastMath.abs(rand.nextDouble()) * 0.3, FastMath.abs(rand.nextDouble()) * 0.3, baseSize + 2.0 * rand.nextDouble()});
        } else if (rand.nextBoolean()) {
            // Single scale
            setScale(baseSize);
        } else {
            // Different scales
            setScale(new double[]{baseSize + rand.nextDouble() * 0.5, baseSize + rand.nextDouble() * 0.5, baseSize + 0.4 * rand.nextDouble()});
        }
        // Type (all but WHITE)
        setType(ModuleBasisFunction.BasisType.values()[rand.nextInt(4)].name());
        // Fractal type
        setFractaltype(ModuleFractal.FractalType.values()[rand.nextInt(6)].name());
        // Frequency
        setFrequency(gaussian(rand, 10.0, 5.0, 2.0));
        // Lacunarity
        setLacunarity(gaussian(rand, 10.0, 5.0, 6.0));
        // Octaves [1,9]
        setOctaves(Math.abs(rand.nextLong()) % 8 + 1L);
        // Range
        double minRange = rocky ? 0.1 : gaussian(rand, -0.5, 0.3);
        double maxRange = 0.5 + FastMath.abs(rand.nextDouble());
        setRange(new double[]{minRange, maxRange});
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

    @Override
    public void dispose() {
    }
}
