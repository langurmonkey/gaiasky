/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.postprocess.effects.Noise;
import gaiasky.render.postprocess.effects.SurfaceGen;
import gaiasky.render.postprocess.filters.NoiseFilter.NoiseType;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Random;

public class NoiseComponent extends NamedComponent {
    public double[] scale = new double[]{1.0, 1.0, 1.0};
    public double power = 1.0;
    public int octaves = 4;
    public double amplitude = 1.0;
    public double persistence = 0.5;
    public double frequency = 2.34;
    public double lacunarity = 2.0;
    public double[] range = new double[]{0.0, 1.0};
    public NoiseType type = NoiseType.SIMPLEX;
    public float seed = 0f;
    public boolean turbulence = true;
    public boolean ridge = true;
    public int numTerraces = 0;
    public float terracesExp = 17.0f;

    public boolean genEmissionMap = false;

    public FrameBuffer fbNoise, fbBiome, fbSurface;

    /** Open windows with the resulting frame buffers. **/
    private static boolean DEBUG_UI_VIEW = false;

    public NoiseComponent() {
        super();
    }


    private FrameBuffer createFrameBuffer(int N, int M, int numColorTargets) {
        GLFrameBuffer.FrameBufferBuilder builder = new GLFrameBuffer.FrameBufferBuilder(N, M);

        for (int i = 0; i < numColorTargets; i++) {
            builder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);
        }

        return builder.build();
    }


    private float toFloatSeed(long seed) {
        var s = Long.toString(seed);
        return (float) (seed / FastMath.pow(10L, s.length()));
    }

    private Noise getNoiseEffect(int N, int M, int channels, int targets) {
        Noise noise = new Noise(N, M, targets);
        noise.setScale(scale);
        noise.setType(type);
        noise.setSeed(seed);
        noise.setOctaves(octaves);
        noise.setAmplitude(amplitude);
        noise.setPersistence(persistence);
        noise.setFrequency(frequency);
        noise.setLacunarity(lacunarity);
        noise.setPower(power);
        noise.setRange((float) range[0], (float) range[1]);
        noise.setTurbulence(turbulence);
        noise.setRidge(ridge);
        noise.setNumTerraces(numTerraces);
        noise.setTerraceExp(terracesExp);
        noise.setChannels(channels);
        return noise;
    }

    public FrameBuffer generateNoise(int N, int M, int channels, int targets, float[] color) {
        fbNoise = fbNoise != null ? fbNoise : createFrameBuffer(N, M, targets);

        Noise noise = getNoiseEffect(N, M, channels, targets);
        noise.setColor(color);
        fbNoise.begin();
        noise.render(null, fbNoise);
        fbNoise.end();

        return fbNoise;
    }

    /**
     * Generates the biome, which is a set of two textures in a frame buffer. The first render target in the frame
     * buffer is the elevation, the second is the moisture, and the third the emission.
     *
     * @param N        The width in pixels.
     * @param M        The height in pixels.
     *
     * @return The biome frame buffer, with two render targets.
     */
    public synchronized FrameBuffer generateBiome(int N, int M) {
        // Biome noise (height, moisture).
        fbBiome = fbBiome != null ? fbBiome : createFrameBuffer(N, M, genEmissionMap ? 2 : 1);

        // 3 channels: height, moisture, emission.
        Noise biomeNoise = getNoiseEffect(N, M, 2, genEmissionMap ? 2 : 1);
        fbBiome.begin();
        biomeNoise.render(null, fbBiome);
        fbBiome.end();

        return fbBiome;
    }

    /**
     * <p>
     * Generates the surface textures with this noise component. The main render target contains the diffuse texture,
     * the second render target contains the specular texture, and the third render target optionally contains the
     * normal texture.
     * </p><p>
     * Note that for this function to succeed, {@link NoiseComponent#generateBiome(int, int)} must have been
     * called beforehand, and {@link NoiseComponent#fbBiome} must be available.
     * </p>
     *
     * @param N                 The width in pixels.
     * @param M                 The height in pixels.
     * @param biomeLut          The biome look up table (LUT) path.
     * @param biomeHueShift     The LUT hue shift as an angle in degrees.
     * @param biomeSaturation   The LUT saturation value.
     * @param generateNormalMap Whether to generate a normal map.
     *
     * @return The frame buffer with all the render targets.
     */
    public synchronized FrameBuffer generateSurface(int N, int M,
                                                    String biomeLut,
                                                    float biomeHueShift,
                                                    float biomeSaturation,
                                                    boolean generateNormalMap) {

        // Gen surface with 2 color targets (diffuse, specular).
        // We use 3 color targets if we need to generate the normal map.
        Texture lut = new Texture(Settings.settings.data.dataFileHandle(biomeLut));
        lut.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fbSurface = fbSurface != null ? fbSurface : createFrameBuffer(N, M, generateNormalMap ? 3 : 2);

        SurfaceGen surfaceGen = new SurfaceGen(generateNormalMap);
        surfaceGen.setLutTexture(lut);
        surfaceGen.setLutHueShift(biomeHueShift);
        surfaceGen.setLutSaturation(biomeSaturation);
        fbSurface.begin();
        surfaceGen.render(fbBiome, fbSurface, null);
        fbSurface.end();

        if (DEBUG_UI_VIEW) {

            // Create UI views.
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Biome", fbBiome.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Diffuse", fbSurface.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Specular", fbSurface.getTextureAttachments().get(1), 1f);
            if (generateNormalMap)
                EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Normal", fbSurface.getTextureAttachments().get(2), 1f);
            DEBUG_UI_VIEW = false;
        }

        return fbSurface;

    }

    public void setType(String noiseType) {
        try {
            this.type = NoiseType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.type = NoiseType.SIMPLEX;
        }
    }

    public void setFractalType(String ignoredFractalType) {
        // Void.
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
     * Sets the number of octaves.
     *
     * @param octaves The octaves.
     */
    public void setOctaves(Long octaves) {
        this.octaves = FastMath.min(9, octaves.intValue());
    }

    public void setAmplitude(Double amplitude) {
        this.amplitude = amplitude;
    }

    public void setPersistence(Double persistence) {
        this.persistence = persistence;
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

    public void setNumTerraces(Long numTerraces) {
        this.numTerraces = numTerraces.intValue();
    }

    public void setTerracesExp(Double terracesExp) {
        this.terracesExp = terracesExp.floatValue();
    }

    public void setTerraceSmoothness(Double terracesExp) {
        setTerracesExp(terracesExp);
    }

    public void setRange(double[] range) {
        this.range = range;
    }

    public void setSeed(Long seed) {
        this.seed = toFloatSeed(seed);
    }

    public void setSeed(Double seed) {
        this.seed = seed.floatValue();
    }

    public void setTurbulence(Boolean t) {
        this.turbulence = t;
    }

    public void setRidge(Boolean t) {
        this.ridge = t;
    }


    public void copyFrom(NoiseComponent other) {
        this.seed = other.seed;
        this.scale = Arrays.copyOf(other.scale, other.scale.length);
        this.type = other.type;
        this.amplitude = other.amplitude;
        this.persistence = other.persistence;
        this.frequency = other.frequency;
        this.lacunarity = other.lacunarity;
        this.octaves = other.octaves;
        this.numTerraces = other.numTerraces;
        this.terracesExp = other.terracesExp;
        this.range = Arrays.copyOf(other.range, other.range.length);
        this.power = other.power;
        this.turbulence = other.turbulence;
        this.ridge = other.ridge;
        this.genEmissionMap = other.genEmissionMap;
    }

    public void randomizeAll(Random rand) {
        randomizeAll(rand, false);
    }

    public void randomizeAll(Random rand, boolean clouds) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        if (clouds) {
            // Type for clouds: PERLIN, SIMPLEX or CURL.
            int d = rand.nextInt(3);
            d = d == 2 ? 3 : d;
            setType(NoiseType.values()[d].name());
        } else {
            // PERLIN, SIMPLEX, VORONOI or CURL
            int d = rand.nextInt(3);
            setType(NoiseType.values()[d].name());
        }
        // Scale.
        double scaleFac = type == NoiseType.CURL ? 2.5 : 1;
        double baseSize = FastMath.abs(gaussian(rand, 4.0, 1.0, 0.5));
        if (clouds) {
            // XY small, Z large.
            double xyScale = gaussian(rand, 5.0, 4.0, 3.0);
            setScale(new double[]{
                    FastMath.abs(xyScale),
                    FastMath.abs(xyScale),
                    FastMath.abs(gaussian(rand, 11.0, 2.0, xyScale + 2.0))});
        } else if (rand.nextBoolean()) {
            // Single scale.
            setScale(baseSize);
        } else {
            // Different scales.
            setScale(new double[]{baseSize + rand.nextDouble() * 0.5, baseSize + rand.nextDouble() * 0.5, baseSize + 0.4 * rand.nextDouble()});
        }
        scale[0] *= scaleFac;
        scale[1] *= scaleFac;
        scale[2] *= scaleFac;
        // Amplitude.
        setAmplitude(gaussian(rand, 1.0, 0.3, 0.8, 1.2));
        // Persistence.
        setPersistence(gaussian(rand, 0.5, 0.07, 0.3));
        // Frequency.
        if (clouds) {
            setFrequency(gaussian(rand, 1.0, 2.0, 0.6));
        } else {
            setFrequency(gaussian(rand, 0.5, 2.0, 0.01));
        }
        // Lacunarity.
        setLacunarity(gaussian(rand, 2.0, 2.0, 1.5));
        // Octaves [1,8].
        if (type == NoiseType.VORONOI) {
            setOctaves((long) rand.nextInt(1, 3));
        } else {
            if (clouds) {
                setOctaves(rand.nextLong(4, 9));
            } else {
                setOctaves(rand.nextLong(1, 9));
            }
        }
        // Terraces.
        if (!clouds && rand.nextBoolean()) {
            setNumTerraces(rand.nextLong(3, 7));
            setTerracesExp((double) rand.nextLong(1, 13) * 2.0 - 1);
        } else {
            setNumTerraces(0L);
        }
        // Range.
        if (clouds) {
            double minRange = gaussian(rand, 0.0, 0.2, -0.2);
            double maxRange = FastMath.abs(rand.nextDouble(0.7, 1.7));
            setRange(new double[]{minRange, maxRange});
        } else {
            double minRange = gaussian(rand, 0.0, 0.3);
            double maxRange = 0.5 + FastMath.abs(rand.nextDouble());
            setRange(new double[]{minRange, maxRange});
        }
        // Power.
        if (clouds) {
            setPower(gaussian(rand, 1.0, 1.0, 0.5));
        } else {
            setPower(gaussian(rand, 2.0, 2.0, 0.2));
        }
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextBoolean());
        // Emission.
        genEmissionMap = rand.nextInt(10) == 9;
    }

    public void randomizeRockyPlanet(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type: PERLIN, SIMPLEX, CURL, VORONOI.
        setType(NoiseType.values()[rand.nextInt(4)].name());
        // Same scale for all.
        double scale = rand.nextDouble(8.0, 15.0);
        setScale(new double[]{scale, scale, scale});
        // Amplitude.
        setAmplitude(gaussian(rand, 1.0, 0.3, 0.8, 1.2));
        // Persistence.
        setPersistence(rand.nextDouble(0.4, 0.6));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.6));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves((long) rand.nextInt(4, 9));
        // Terraces.
        if (rand.nextBoolean()) {
            setNumTerraces(rand.nextLong(3, 7));
            setTerracesExp((double) rand.nextLong(1, 13) * 2.0 - 1);
        } else {
            setNumTerraces(0L);
        }
        // Range.
        setRange(new double[]{
                rand.nextDouble(0.1, 0.3),
                rand.nextDouble(0.7, 2.0)});
        // Power.
        setPower(rand.nextDouble(0.5, 1.1));
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextInt(3) < 2);
        // Emission.
        genEmissionMap = rand.nextInt(20) == 19;
    }

    public void randomizeEarthLike(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type: PERLIN, SIMPLEX, CURL, VORONOI.
        setType(NoiseType.values()[rand.nextInt(4)].name());
        // Same scale for all.
        double scale = rand.nextDouble(3.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Amplitude.
        setAmplitude(gaussian(rand, 1.0, 0.3, 0.8, 1.0));
        // Persistence.
        setPersistence(rand.nextDouble(0.2, 0.5));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.35));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves((long) rand.nextInt(5, 9));
        // Terraces.
        if (rand.nextBoolean()) {
            setNumTerraces(rand.nextLong(3, 7));
            setTerracesExp((double) rand.nextLong(1, 13) * 2.0 - 1);
        } else {
            setNumTerraces(0L);
        }
        // Range.
        setRange(new double[]{
                rand.nextDouble(-0.5, 0.0),
                rand.nextDouble(0.8, 1.5)});
        // Power.
        setPower(rand.nextDouble(0.5, 1.6));
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextInt(4) < 3);
        // Emission.
        genEmissionMap = rand.nextInt(4) == 3;
    }

    public void randomizeSnowPlanet(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type: PERLIN, SIMPLEX, CURL, VORONOI.
        setType(NoiseType.values()[rand.nextInt(4)].name());
        // Same scale for all.
        double scale = rand.nextDouble(4.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Amplitude.
        setAmplitude(gaussian(rand, 1.0, 0.3, 0.8, 1.0));
        // Persistence.
        setPersistence(rand.nextDouble(0.2, 0.5));
        // Frequency.
        setFrequency(rand.nextDouble(0.2, 0.65));
        // Lacunarity.
        setLacunarity(rand.nextDouble(2.0, 5.0));
        // Octaves [1,4].
        setOctaves((long) rand.nextInt(3, 8));
        // Terraces.
        if (rand.nextInt(5) == 4) {
            setNumTerraces(rand.nextLong(3, 7));
            setTerracesExp((double) rand.nextLong(1, 13) * 2.0 - 1);
        } else {
            setNumTerraces(0L);
        }
        // Range.
        setRange(new double[]{
                rand.nextDouble(-0.4, 0.0),
                rand.nextDouble(1.0, 1.5)});
        // Power.
        setPower(rand.nextDouble(0.5, 1.8));
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextInt(4) < 3);
        // Emission.
        genEmissionMap = rand.nextInt(15) == 14;
    }

    public void randomizeGasGiant(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        // Type: all but WHITE. VORONOI is rare.
        if (rand.nextInt(20) < 17) {
            // PERLIN, SIMPLEX or CURL
            int d = rand.nextInt(3);
            d = d == 2 ? 3 : d;
            setType(NoiseType.values()[d].name());
        } else {
            // VORONOI
            setType(NoiseType.VORONOI.name());
        }
        // Scale.
        double scaleFac = type == NoiseType.CURL ? 2.5 : 1;
        // XY small, Z large.
        setScale(new double[]{
                FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                FastMath.abs(gaussian(rand, 7.0, 2.0, 5.0))});
        scale[0] *= scaleFac;
        scale[1] *= scaleFac;
        scale[2] *= scaleFac;
        // Amplitude.
        setAmplitude(gaussian(rand, 1.0, 0.3, 0.8, 1.2));
        // Persistence.
        setPersistence(rand.nextDouble(0.4, 0.6));
        // Frequency.
        setFrequency(rand.nextDouble(0.5, 3.0));
        // Lacunarity.
        setLacunarity(rand.nextDouble(0.1, 3.0));
        // Octaves [1,4].
        setOctaves((long) rand.nextInt(1, 4));
        // Terraces.
        setNumTerraces(0L);
        // Range.
        setRange(new double[]{0.4, rand.nextDouble(0.9, 1.3)});
        // Power.
        setPower(0.1);
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextBoolean());
        // Emission.
        genEmissionMap = rand.nextInt(10) == 9;
    }

    public void print(Log log) {
        log.debug("Seed: " + seed);
        log.debug("Scale: " + Arrays.toString(scale));
        log.debug("Noise type: " + type);
        log.debug("Amplitude: " + amplitude);
        log.debug("Persistence: " + persistence);
        log.debug("Frequency: " + frequency);
        log.debug("Lacunarity: " + lacunarity);
        log.debug("Octaves: " + octaves);
        log.debug("Terraces: " + numTerraces);
        log.debug("Terraces exponent: " + terracesExp);
        log.debug("Range: " + Arrays.toString(range));
        log.debug("Power: " + power);
        log.debug("Turbulence: " + turbulence);
        log.debug("Ridge: " + ridge);
        log.debug("Emission: " + genEmissionMap);
    }

    @Override
    public void dispose() {
        if (fbSurface != null) {
            fbSurface.dispose();
            fbSurface = null;
        }
        if (fbBiome != null) {
            fbBiome.dispose();
            fbBiome = null;
        }
        if (fbNoise != null) {
            fbNoise.dispose();
            fbNoise = null;
        }
    }
}

