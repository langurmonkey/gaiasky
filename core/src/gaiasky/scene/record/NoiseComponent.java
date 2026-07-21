/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer;
import gaiasky.render.postprocess.effects.Clouds;
import gaiasky.render.postprocess.effects.ProceduralSurface;
import gaiasky.render.util.NoiseType;
import gaiasky.util.Logger.Log;
import net.jafama.FastMath;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public final class NoiseComponent extends NamedComponent {
    public double[] scale = new double[]{1.0, 1.0, 1.0};
    public int octaves = 4;
    public double persistence = 0.5;
    public double frequency = 1.0;
    public double lacunarity = 2.0;
    public NoiseType type = NoiseType.SIMPLEX;
    public float seed = 0f;
    public boolean turbulence = true;
    public boolean ridge = true;
    public boolean smoothing = true;

    /** Base level for noise. Strip everything below, then remap or clamp result. **/
    public float baseLevel = 0.2f;
    /** Latitude influence on the temperature. Only for procedural surfaces (no clouds). **/
    public float latitudeInfluence = 0.8f;
    /** Remap after base level operation. **/
    public boolean remap = false;

    /** The height at which plains end, in the [0,1] range. **/
    public float plainsHeight = 0.0f;
    /** Slope of plains. Lower values produce flatter plains, while higher values produce steeper plains. **/
    public float plainsSlope = 0.2f;

    /** Strength of the domain warping. **/
    public float warpStrength = 0.0f;
    /** Frequency of the warp noise. **/
    public float warpFrequency = 1.0f;

    /** Generate emissive map (lights). **/
    public boolean genEmissiveMap = false;

    public FrameBuffer fbMask, fbMain;

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

    private Clouds getCloudsEffect(int N,
                                   int M,
                                   String shader) {
        Clouds biome = new Clouds(N, M, shader);
        biome.setScale(scale);
        biome.setType(type);
        biome.setBaseLevel(baseLevel);
        biome.setRemap(remap);
        biome.setSeed(seed);
        biome.setOctaves(octaves);
        biome.setPersistence(persistence);
        biome.setFrequency(frequency);
        biome.setLacunarity(lacunarity);
        biome.setSmoothing(smoothing);
        biome.setTurbulence(turbulence);
        biome.setRidge(ridge);
        biome.setPlainsHeight(plainsHeight);
        biome.setPlainsSlope(plainsSlope);
        biome.setWarpStrength(warpStrength);
        biome.setWarpFrequency(warpFrequency);
        return biome;
    }

    public FrameBuffer generateClouds(int N,
                                      int M,
                                      float[] color) {
        fbMain = fbMain != null ? fbMain : createFrameBuffer(N, M, 1);

        Clouds clouds = getCloudsEffect(N, M, "clouds");
        clouds.setColor(color);
        clouds.render(null, fbMain);

        return fbMain;
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
        Clouds biomeNoise = getCloudsEffect(N, M, "biome");
        fbMask.begin();
        biomeNoise.render(null, fbMask);
        fbMask.end();

        return fbMask;
    }

    /**
     * Generates the procedural surface (merged old biome and surface shaders).
     *
     * @param N                 The width in pixels.
     * @param M                 The height in pixels.
     * @param biomeLUT          LUT for the biome.
     * @param channels          The number of noise channels (1: elevation, 2: moisture, 3: temperature).
     * @param generateNormalMap Whether to generate a normal map.
     *
     * @return The frame buffer with all the render targets.
     */
    public synchronized FrameBuffer generateProceduralSurface(int N,
                                                              int M,
                                                              String biomeLUT,
                                                              float lutHueShift,
                                                              float lutSaturation,
                                                              int channels,
                                                              boolean generateNormalMap) {

        // Number of render targets.
        int targets = 4; // Biome, diffuse, specular, emissive.
        if (generateNormalMap) targets++; // Normal.

        // LUT 3D texture.
        var lut = MaterialComponent.getLUTManager().getLUT3D(biomeLUT);

        // Frame buffer.
        fbMain = fbMain != null ? fbMain : createFrameBuffer(N, M, targets);

        var effect = new ProceduralSurface(N, M, generateNormalMap, genEmissiveMap);
        effect.setLutTexture(lut);
        effect.setLutHueShift(lutHueShift);
        effect.setLutSaturation(lutSaturation);
        effect.setScale(scale);
        effect.setType(type);
        effect.setBaseLevel(baseLevel);
        effect.setRemap(remap);
        effect.setSeed(seed);
        effect.setOctaves(octaves);
        effect.setPersistence(persistence);
        effect.setFrequency(frequency);
        effect.setLacunarity(lacunarity);
        effect.setSmoothing(smoothing);
        effect.setTurbulence(turbulence);
        effect.setRidge(ridge);
        effect.setLatitudeInfluence(latitudeInfluence);
        effect.setPlainsHeight(plainsHeight);
        effect.setPlainsSlope(plainsSlope);
        effect.setWarpStrength(warpStrength);
        effect.setWarpFrequency(warpFrequency);
        effect.setChannels(channels);
        fbMain.begin();
        effect.render(null, fbMain);
        fbMain.end();

        return fbMain;
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

    public void setLatitudeInfluence(Double value) {
        this.latitudeInfluence = value.floatValue();
    }

    public void setPlainsHeight(Double value) {
        this.plainsHeight = value.floatValue();
    }

    public void setPlainsSlope(Double value) {
        this.plainsSlope = value.floatValue();
    }

    public void setWarpStrength(Double value) {
        this.warpStrength = value.floatValue();
    }

    public void setWarpFrequency(Double value) {
        this.warpFrequency = value.floatValue();
    }

    public void setGenEmissiveMap(Boolean value) {
        genEmissiveMap = value;
    }


    public void copyFrom(NoiseComponent other) {
        this.seed = other.seed;
        this.scale = Arrays.copyOf(other.scale, other.scale.length);
        this.type = other.type;
        this.persistence = other.persistence;
        this.frequency = other.frequency;
        this.lacunarity = other.lacunarity;
        this.octaves = other.octaves;
        this.turbulence = other.turbulence;
        this.ridge = other.ridge;
        this.genEmissiveMap = other.genEmissiveMap;
        this.baseLevel = other.baseLevel;
        this.remap = other.remap;
        this.latitudeInfluence = other.latitudeInfluence;
        this.plainsHeight = other.plainsHeight;
        this.plainsSlope = other.plainsSlope;
        this.warpFrequency = other.warpFrequency;
        this.warpStrength = other.warpStrength;
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
        setType(NoiseType.values()[rand.nextInt(3)].name());
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
        setPersistence(gaussian(rand, 0.6, 0.1, 0.3));
        // Frequency.
        setFrequency(gaussian(rand, 0.5, 2.0, 0.01));
        // Lacunarity.
        setLacunarity(gaussian(rand, 2.0, 2.0, 1.5));
        // Octaves.
        setOctaves(5L);
        // Base level.
        setBaseLevel(gaussian(rand, 0.1, 0.04, 0.01, turbulence ? 0.25 : 0.4));
        //Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Latitude influence.
        setLatitudeInfluence(uniform(rand, 0.4, 0.6));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.6));
        setPlainsSlope(uniform(rand, 0.05, 0.3));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.5, 1.5));
            setWarpFrequency(uniform(rand, 0.6, 2.0));
        }
        // Emission.
        setGenEmissiveMap(rand.nextInt(10) == 9);

    }

    /**
     * Randomizes the noise component for cloud generation.
     *
     * @param rand The RNG.
     */
    public void randomizeForClouds(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Turbulence (7/10 probability).
        int turbP = rand.nextInt(10);
        boolean turbulence = turbP < 7;
        // Ridge (2/7 probability).
        boolean ridge = turbulence && turbP < 2;
        setTurbulence(turbulence);
        // Ridge.
        setRidge(ridge);
        // Smoothing.
        setSmoothing(rand.nextBoolean());

        // Type.
        setType(NoiseType.values()[rand.nextInt(3)].name());

        // Scale.
        // XY small, Z large sometimes.
        double xyScale = FastMath.abs(gaussian(rand, 3.0, 1.0, 1.5, 6.0));
        double zScale = FastMath.abs(gaussian(rand, 12.0, 1.0, 10.0, 14.0));
        setScale(new double[]{
                xyScale,
                xyScale,
                !turbulence && rand.nextBoolean() ? zScale : xyScale});

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
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.2));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.5, 1.5));
            setWarpFrequency(uniform(rand, 0.6, 2.0));
        }
        //Remap.
        setRemap(rand.nextDouble() > 0.1);
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
        var type = rand.nextBoolean() ? NoiseType.CRATER : NoiseType.values()[rand.nextInt(3)];
        setType(type.name());
        // Same scale for all.
        double scale = rand.nextDouble(8.0, 15.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.6, 0.9));
        // Frequency.
        setFrequency(rand.nextDouble(0.01, 0.6));
        // Lacunarity.
        setLacunarity(rand.nextDouble(3.0, 5.0));
        // Octaves.
        setOctaves(type == NoiseType.CRATER ? 4L : 5L);
        // Turbulence.
        setTurbulence(rand.nextInt(4) == 3);
        // Ridge.
        setRidge(turbulence && rand.nextInt(3) < 2);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Base level.
        if (type == NoiseType.CRATER) {
            setBaseLevel(uniform(rand, 0.25, 0.45));
        } else {
            setBaseLevel(gaussian(rand, 0.2, 0.05, 0.0, 0.4));
        }
        // Latitude influence.
        setLatitudeInfluence(uniform(rand, 0.2, 0.55));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.6));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.01, 1.0));
            setWarpFrequency(uniform(rand, 0.5, 1.0));
        }
        //Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(20) == 19);
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
        var type = NoiseType.values()[rand.nextInt(3)];
        setType(type.name());
        // Same scale for all.
        double scale = rand.nextDouble(3.0, 8.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.6, 0.9));
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
        // Base level.
        setBaseLevel(type == NoiseType.VORONOI ? rand.nextDouble(0.35, 0.6) : rand.nextDouble(0.25, 0.65));
        // Latitude influence.
        setLatitudeInfluence(uniform(rand, 0.65, 0.85));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.45));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.0, 0.25));
            setWarpFrequency(uniform(rand, 1.0, 2.5));
        }
        //Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(4) == 3);
    }

    /**
     * Randomizes the noise component parameters to generate an alien planet.
     *
     * @param rand The RNG.
     */
    public void randomizeAlien(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        setType(NoiseType.values()[rand.nextInt(3)].name());
        // Scale.
        double scale = rand.nextDouble(3.0, 8.0);
        double scaleZ = rand.nextDouble(8.0, 14.0);
        setScale(new double[]{scale, scale, rand.nextInt(8) < 3 ? scale : scaleZ});
        // Persistence.
        setPersistence(rand.nextDouble(0.5, 0.9));
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
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        // Latitude influence.
        setLatitudeInfluence(0.5);
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.6));
        setPlainsSlope(uniform(rand, 0.05, 0.12));
        // Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(true);
    }

    /**
     * Randomizes the noise component parameters to generate a desert planet.
     *
     * @param rand The RNG.
     */
    public void randomizeDesert(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        setType(NoiseType.values()[rand.nextInt(3)].name());
        // Same scale for all.
        double scale = rand.nextDouble(5.0, 9.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.45, 0.8));
        // Frequency.
        setFrequency(rand.nextDouble(0.05, 0.25));
        // Lacunarity.
        setLacunarity(rand.nextDouble(4.0, 7.0));
        // Octaves.
        setOctaves(5L);
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(turbulence && rand.nextInt(10) < 3);
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Base level.
        setBaseLevel(0.0);
        // Latitude influence.
        setLatitudeInfluence(uniform(rand, 0.4, 0.65));
        // Plains.
        setPlainsHeight(uniform(rand, 0.1, 0.7));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(4) == 3);
    }

    /**
     * Randomizes the noise component parameters to generate a tropical island planet.
     *
     * @param rand The RNG.
     */
    public void randomizeTropical(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        var type = NoiseType.values()[rand.nextInt(3)];
        setType(type.name());
        // Same scale for all.
        double scale = rand.nextDouble(5.0, 9.0);
        setScale(new double[]{scale, scale, scale});
        // Persistence.
        setPersistence(rand.nextDouble(0.6, 0.95));
        // Frequency.
        setFrequency(rand.nextDouble(0.2, 0.55));
        // Lacunarity.
        setLacunarity(rand.nextDouble(4.0, 7.0));
        // Octaves.
        setOctaves(5L);
        // Turbulence.
        setTurbulence(rand.nextBoolean());
        // Ridge.
        setRidge(turbulence && rand.nextBoolean());
        // Smoothing.
        setSmoothing(rand.nextBoolean());
        // Base level.
        setBaseLevel(type == NoiseType.VORONOI ? rand.nextDouble(0.65, 0.9) : rand.nextDouble(0.3, 0.7));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.5));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.0, 0.15));
            setWarpFrequency(uniform(rand, 1.0, 1.5));
        }
        // Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(4) == 3);
    }

    /**
     * Randomizes the noise component parameters to generate a Snow planet.
     *
     * @param rand The RNG.
     */
    public void randomizeSnowPlanet(Random rand) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        setType(NoiseType.values()[rand.nextInt(3)].name());
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
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.5));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(15) == 14);
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
        int d = rand.nextInt(3);
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
        // Base level.
        setBaseLevel(gaussian(rand, 0.25, 0.1, 0.0, 0.5));
        // Latitude influence.
        setLatitudeInfluence(uniform(rand, 0.3, 0.55));
        // Plains.
        setPlainsHeight(uniform(rand, 0.0, 0.2));
        setPlainsSlope(uniform(rand, 0.05, 0.2));
        // Warp.
        if (rand.nextBoolean()) {
            setWarpStrength(uniform(rand, 0.6, 3.0));
            setWarpFrequency(uniform(rand, 1.0, 3.0));
        }
        //Remap.
        setRemap(rand.nextDouble() > 0.2);
        // Emission.
        setGenEmissiveMap(rand.nextInt(10) == 9);
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
        log.debug("Latitude influence: " + latitudeInfluence);
        log.debug("Plains height/slope: " + plainsHeight + "/" + plainsSlope);
    }

    @Override
    public void dispose() {
        if (fbMain != null) {
            fbMain.dispose();
            fbMain = null;
        }
        if (fbMask != null) {
            fbMask.dispose();
            fbMask = null;
        }
    }
}

