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
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.postprocess.effects.Biome;
import gaiasky.render.postprocess.effects.SurfaceGen;
import gaiasky.render.postprocess.filters.BiomeFilter.NoiseType;
import gaiasky.util.Logger.Log;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public final class NoiseComponent extends NamedComponent {
    public double[] scale = new double[]{1.0, 1.0, 1.0};
    public int octaves = 4;
    public double amplitude = 1.0;
    public double persistence = 0.5;
    public double frequency = 2.34;
    public double lacunarity = 2.0;
    public NoiseType type = NoiseType.SIMPLEX;
    public float seed = 0f;
    public boolean turbulence = true;
    public boolean ridge = true;
    public boolean smoothing = true;

    /** Base level for noise. Strip everything below, then remap or clamp result. **/
    public float baseLevel = 0.2f;
    /** Remap after base level operation. **/
    public boolean remap = false;

    public boolean genEmissiveMap = false;

    public FrameBuffer fbNoise, fbBiome, fbMask, fbSurface;

    /** Open windows with the resulting frame buffers. **/
    private static boolean DEBUG_UI_VIEW = false;

    public NoiseComponent() {
        super();
    }


    private FrameBuffer createFrameBuffer(int N,
                                          int M,
                                          int numColorTargets) {
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

    private Biome getNoiseEffect(int N,
                                 int M,
                                 int channels,
                                 int targets,
                                 String shader) {
        Biome biome = new Biome(N, M, targets, shader);
        biome.setScale(scale);
        biome.setType(type);
        biome.setBaseLevel(baseLevel);
        biome.setRemap(remap);
        biome.setSeed(seed);
        biome.setOctaves(octaves);
        biome.setAmplitude(amplitude);
        biome.setPersistence(persistence);
        biome.setFrequency(frequency);
        biome.setLacunarity(lacunarity);
        biome.setSmoothing(smoothing);
        biome.setTurbulence(turbulence);
        biome.setRidge(ridge);
        biome.setChannels(channels);
        return biome;
    }

    public FrameBuffer generateClouds(int N,
                                      int M,
                                      float[] color) {
        int targets = 1;
        int channels = 1;
        fbNoise = fbNoise != null ? fbNoise : createFrameBuffer(N, M, targets);

        Biome biome = getNoiseEffect(N, M, channels, targets, "biome-clouds");
        biome.setColor(color);
        biome.render(null, fbNoise);

        return fbNoise;
    }

    /**
     * Generates the mask, which is a single low-resolution texture for the water/land areas.
     *
     * @param N The width in pixels.
     * @param M The height in pixels.
     *
     * @return The mask buffer.
     */
    public synchronized FrameBuffer generateMask(int N,
                                                 int M) {
        // Biome noise (height, moisture).
        fbMask = fbMask != null ? fbMask : createFrameBuffer(N, M, 1);

        // 1 channels: water/land.
        // Emissive map is an additional render target.
        Biome biomeNoise = getNoiseEffect(N, M, 1, 1, "biome");
        fbMask.begin();
        biomeNoise.render(null, fbMask);
        fbMask.end();

        return fbMask;
    }

    /**
     * Generates the biome, which is a set of two textures in a frame buffer. The first render target in the frame
     * buffer is the elevation, the second is the moisture, and the third the emission.
     *
     * @param N The width in pixels.
     * @param M The height in pixels.
     *
     * @return The biome frame buffer, with two render targets.
     */
    public synchronized FrameBuffer generateBiome(int N,
                                                  int M) {
        // Biome noise (height, moisture).
        fbBiome = fbBiome != null ? fbBiome : createFrameBuffer(N, M, genEmissiveMap ? 2 : 1);

        // 2 channels: height, moisture, temperature (optional).
        // Emissive map is an additional render target.
        Biome biomeNoise = getNoiseEffect(N, M, 2, genEmissiveMap ? 2 : 1, "biome");
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
    public synchronized FrameBuffer generateSurface(int N,
                                                    int M,
                                                    String biomeLut,
                                                    float biomeHueShift,
                                                    float biomeSaturation,
                                                    boolean generateNormalMap) {

        // Gen surface with 2 color targets (diffuse, specular).
        // We use 3 color targets if we need to generate the normal map.
        Texture lut = new Texture(GaiaSky.settings().data.dataFileHandle(biomeLut));
        lut.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        fbSurface = fbSurface != null ? fbSurface : createFrameBuffer(N, M, generateNormalMap ? 3 : 2);

        var surfaceGen = new SurfaceGen(generateNormalMap, genEmissiveMap);
        surfaceGen.setLutTexture(lut);
        // If we remapped the result after the base level operation, the base level is automatically 0.
        surfaceGen.setBaseLevel(remap ? 0 : baseLevel);
        surfaceGen.setLutHueShift(biomeHueShift);
        surfaceGen.setLutSaturation(biomeSaturation);
        if (genEmissiveMap) {
            surfaceGen.setEmissiveTexture(fbBiome.getTextureAttachments().get(1));
        }
        fbSurface.begin();
        surfaceGen.render(fbBiome, fbSurface, null, null);
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
            this.type = NoiseType.valueOf(noiseType.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            this.type = NoiseType.SIMPLEX;
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

    public void setSeed(Long seed) {
        this.seed = toFloatSeed(seed);
    }

    public void setSeed(Double seed) {
        this.seed = seed.floatValue();
    }

    public void setSmoothing(Boolean t) {
        this.smoothing = t;
    }

    public void setTurbulence(Boolean t) {
        this.turbulence = t;
    }

    public void setRidge(Boolean t) {
        this.ridge = t;
    }

    public void setBaseLevel(Double value) {
        this.baseLevel = value.floatValue();
    }

    public void setRemap(Boolean remap) {
        this.remap = remap;
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
        this.turbulence = other.turbulence;
        this.ridge = other.ridge;
        this.genEmissiveMap = other.genEmissiveMap;
        this.baseLevel = other.baseLevel;
        this.remap = other.remap;
    }

    /**
     * Randomizes the noise component for terrain generation.
     *
     * @param rand The RNG.
     */
    public void randomizeForTerrain(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));

        // Turbulence.
        boolean turbulence = rand.nextInt(3) == 2;
        setTurbulence(turbulence);
        // Ridge.
        setRidge(turbulence && rand.nextBoolean());
        // Smoothing.
        setSmoothing(rand.nextBoolean());

        // Type.
        setType(NoiseType.values()[rand.nextInt(2)].name());
        // Scale.
        double baseSize = FastMath.abs(gaussian(rand, 4.0, 1.0, 0.5));
        if (rand.nextBoolean()) {
            // Same scale to all
            setScale(baseSize);
        } else {
            // Different scales.
            setScale(new double[]{baseSize + rand.nextDouble() * 0.5, baseSize + rand.nextDouble() * 0.5, baseSize + 0.4 * rand.nextDouble()});
        }

        // Persistence.
        setPersistence(gaussian(rand, 0.5, 0.07, 0.3));
        // Frequency.
        setFrequency(gaussian(rand, 0.5, 2.0, 0.01));
        // Lacunarity.
        setLacunarity(gaussian(rand, 2.0, 2.0, 1.5));
        // Octaves.
        setOctaves(5L);
        // Base level.
        setBaseLevel(gaussian(rand, 0.1, 0.04, 0.01, turbulence ? 0.25 : 0.4));
        //Remap.
        setRemap(rand.nextBoolean());
        // Emission.
        genEmissiveMap = rand.nextInt(10) == 9;

    }

    /**
     * Randomizes the noise component for cloud generation.
     *
     * @param rand The RNG.
     */
    public void randomizeForClouds(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Turbulence.
        boolean turbulence = rand.nextBoolean();
        boolean ridge = turbulence && rand.nextBoolean();
        setTurbulence(turbulence);
        // Ridge.
        setRidge(ridge);
        // Smoothing.
        setSmoothing(rand.nextBoolean());

        // Type.
        setType(NoiseType.values()[rand.nextInt(2)].name());
        // Scale.
        // XY small, Z large sometimes.
        double xyScale = FastMath.abs(gaussian(rand, 3.0, 1.0, 1.5, 6.0));
        double zScale = FastMath.abs(gaussian(rand, 12.0, 1.0, 10.0, 14.0));
        setScale(new double[]{
                xyScale,
                xyScale,
                rand.nextBoolean() ? zScale : xyScale});

        // Persistence.
        setPersistence(uniform(rand, 0.65, 0.9));
        // Frequency.
        setFrequency(uniform(rand, 0.2, 0.6));
        // Lacunarity.
        setLacunarity(uniform(rand, 3.0, 5.0));
        // Octaves.
        setOctaves(6L);
        // Base level.
        if (turbulence && ridge) {
            setBaseLevel(gaussian(rand, 0.6, 0.15, 0.4, 0.8));
        } else if (turbulence) {
            setBaseLevel(gaussian(rand, 0.3, 0.15, 0.18, 0.45));
        } else {
            setBaseLevel(gaussian(rand, 0.5, 0.15, 0.36, 0.7));
        }
        //Remap.
        setRemap(rand.nextBoolean());
    }

    /**
     * Randomizes the noise component parameters to generate a rocky planet.
     *
     * @param rand The RNG.
     */
    public void randomizeRockyPlanet(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        setType(NoiseType.values()[rand.nextInt(2)].name());
        // Same scale for all.
        double scale = rand.nextDouble(8.0, 15.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.4, 0.6));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.6));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves(5L);
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextInt(3) < 2);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Emission.
        genEmissiveMap = rand.nextInt(20) == 19;
        // Base level.
        setBaseLevel(gaussian(rand, 0.05, 0.01, 0.0, 0.1));
        //Remap.
        setRemap(rand.nextBoolean());
    }

    /**
     * Randomizes the noise component parameters for a high-level land/sea mask.
     *
     * @param rand The RNG.
     */
    public void randomizeMask(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Simplex.
        setType(NoiseType.SIMPLEX.name());
        // Scale.
        double scale = rand.nextDouble(3.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.2, 0.5));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.15));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves((long) rand.nextInt(3, 6));
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextInt(4) < 3);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
    }

    /**
     * Randomizes the noise component parameters to generate an Earth-like planet.
     *
     * @param rand The RNG.
     */
    public void randomizeEarthLike(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        setType(NoiseType.values()[rand.nextInt(2)].name());
        // Same scale for all.
        double scale = rand.nextDouble(3.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.2, 0.5));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.35));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves(5L);
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextInt(4) < 3);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Emission.
        genEmissiveMap = rand.nextInt(4) == 3;
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        //Remap.
        setRemap(rand.nextBoolean());
    }

    /**
     * Randomizes the noise component parameters to generate a Snow planet.
     *
     * @param rand The RNG.
     */
    public void randomizeSnowPlanet(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type: PERLIN, SIMPLEX
        setType(NoiseType.values()[rand.nextInt(2)].name());
        // Same scale for all.
        double scale = rand.nextDouble(4.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.2, 0.5));
        // Frequency.
        setFrequency(rand.nextDouble(0.2, 0.65));
        // Lacunarity.
        setLacunarity(rand.nextDouble(2.0, 5.0));
        // Octaves [1,4].
        setOctaves(5L);
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextInt(4) < 3);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Emission.
        genEmissiveMap = rand.nextInt(15) == 14;
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        //Remap.
        setRemap(rand.nextBoolean());
    }

    /**
     * Randomizes the noise component parameters to generate a Gas giant.
     *
     * @param rand The RNG.
     */
    public void randomizeGasGiant(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        // PERLIN, SIMPLEX
        int d = rand.nextInt(2);
        setType(NoiseType.values()[d].name());
        // Scale.
        double scaleFac = 1;
        // XY small, Z large.
        setScale(new double[]{
                FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                FastMath.abs(gaussian(rand, 7.0, 2.0, 5.0))});
        scale[0] *= scaleFac;
        scale[1] *= scaleFac;
        scale[2] *= scaleFac;
        // Persistence.
        setPersistence(rand.nextDouble(0.4, 0.6));
        // Frequency.
        setFrequency(rand.nextDouble(0.5, 3.0));
        // Lacunarity.
        setLacunarity(rand.nextDouble(0.1, 3.0));
        // Octaves [1,4].
        setOctaves(4L);
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextBoolean());
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Emission.
        genEmissiveMap = rand.nextInt(10) == 9;
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        //Remap.
        setRemap(rand.nextBoolean());
    }

    public void print(Log log) {
        log.debug("Seed: " + seed);
        log.debug("Scale: " + Arrays.toString(scale));
        log.debug("Noise type: " + type);
        log.debug("Base level" + baseLevel);
        log.debug("Remap?" + remap);
        log.debug("Persistence: " + persistence);
        log.debug("Frequency: " + frequency);
        log.debug("Lacunarity: " + lacunarity);
        log.debug("Octaves: " + octaves);
        log.debug("Smoothing: " + smoothing);
        log.debug("Turbulence: " + turbulence);
        log.debug("Ridge: " + ridge);
        log.debug("Emission: " + genEmissiveMap);
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

