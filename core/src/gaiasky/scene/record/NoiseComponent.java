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
    public double frequency = 2.34;
    public double persistence = 0.5;
    public double lacunarity = 2.0;
    public double[] range = new double[]{0.0, 1.0};
    public NoiseType type = NoiseType.SIMPLEX;
    public float seed = 0f;
    public boolean turbulence = true;
    public boolean ridge = true;

    public FrameBuffer fbNoise, fbHeight, fbMoisture, fbSurface;

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

    private Noise getNoiseEffect(int N, int M) {
        Noise noise = new Noise(N, M);
        noise.setScale(scale);
        noise.setType(type);
        noise.setSeed(seed);
        noise.setOctaves(octaves);
        noise.setFrequency(frequency);
        noise.setPersistence(persistence);
        noise.setLacunarity(lacunarity);
        noise.setPower(power);
        noise.setRange((float) range[0], (float) range[1]);
        noise.setTurbulence(turbulence);
        noise.setRidge(ridge);
        return noise;
    }

    public FrameBuffer generateNoise(int N, int M) {
       return generateNoise(N, M, new float[]{1f, 1f, 1f});
    }

    public FrameBuffer generateNoise(int N, int M, float[] color) {
        fbNoise = fbNoise != null ? fbNoise : createFrameBuffer(N, M, 1);

        Noise noise = getNoiseEffect(N, M);
        noise.setColor(color);
        fbNoise.begin();
        noise.render(null, fbNoise);
        fbNoise.end();

        return fbNoise;
    }

    public FrameBuffer[] generateSurfaceTextures(int N, int M, String biomeLut, float biomeHueShift) {
        // Height
        fbHeight = fbHeight != null ? fbHeight : createFrameBuffer(N, M, 1);

        Noise heightNoise = getNoiseEffect(N, M);
        fbHeight.begin();
        heightNoise.render(null, fbHeight);
        fbHeight.end();

        // Moisture
        fbMoisture = fbMoisture != null ? fbMoisture : createFrameBuffer(N, M, 1);

        Noise moistureNoise = getNoiseEffect(N, M);
        moistureNoise.setType(NoiseType.PERLIN);
        moistureNoise.setSeed(seed + 2.023f);
        moistureNoise.setRange(-0.2f, 1.0f);
        fbMoisture.begin();
        moistureNoise.render(null, fbMoisture);
        fbMoisture.end();

        // Gen surface.
        Texture lut = new Texture(Settings.settings.data.dataFileHandle(biomeLut));
        fbSurface = fbSurface != null ? fbSurface : createFrameBuffer(N, M, 3);

        SurfaceGen surfaceGen = new SurfaceGen();
        surfaceGen.setMoistureTexture(fbMoisture.getColorBufferTexture());
        surfaceGen.setLutTexture(lut);
        surfaceGen.setLutHueShift(biomeHueShift);
        fbSurface.begin();
        surfaceGen.render(fbHeight, fbSurface);
        fbSurface.end();

        if (DEBUG_UI_VIEW) {

            // Create UI views.
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Height", fbHeight.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Moisture", fbMoisture.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Diffuse", fbSurface.getColorBufferTexture(), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Specular", fbSurface.getTextureAttachments().get(1), 1f);
            EventManager.publish(Event.SHOW_TEXTURE_WINDOW_ACTION, this, "Normal", fbSurface.getTextureAttachments().get(2), 1f);
            DEBUG_UI_VIEW = false;
        }

        return new FrameBuffer[]{fbHeight, fbMoisture, fbSurface};

    }

    public synchronized FrameBuffer[] generateElevation(int N, int M, String biomeLut, float biomeHueShift) {
        // Generate in GPU.
        return generateSurfaceTextures(N, M, biomeLut, biomeHueShift);
    }

    public void setType(String noiseType) {
        try {
            this.type = NoiseType.valueOf(noiseType.toUpperCase());
        } catch (Exception e) {
            this.type = NoiseType.SIMPLEX;
        }
    }

    public void setFractalType(String fractalType) {
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

    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }

    public void setPersistence(Double persistence) {
        this.persistence = persistence;
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
        this.frequency = other.frequency;
        this.persistence = other.persistence;
        this.lacunarity = other.lacunarity;
        this.octaves = other.octaves;
        this.range = Arrays.copyOf(other.range, other.range.length);
        this.power = other.power;
        this.turbulence = other.turbulence;
        this.ridge = other.ridge;
    }

    public void randomizeAll(Random rand) {
        randomizeAll(rand, rand.nextBoolean(), false);
    }

    public void randomizeAll(Random rand, boolean rocky, boolean clouds) {
        // Seed.
        setSeed(rand.nextDouble(2.0));
        // Type.
        if(clouds) {
            // Type for clouds: PERLIN, SIMPLEX or CURL.
            int d = rand.nextInt(3);
            d = d == 2 ? 3 : d;
            setType(NoiseType.values()[d].name());
        } else {
            // Type: all but WHITE. VORONOI is rare.
            if(rand.nextInt(20) < 18) {
                // PERLIN, SIMPLEX or CURL
                int d = rand.nextInt(3);
                d = d == 2 ? 3 : d;
                setType(NoiseType.values()[d].name());
            } else {
                // VORONOI
                setType(NoiseType.VORONOI.name());
            }
        }
        // Scale.
        double scaleFac = type == NoiseType.CURL ? 2.5 : 1;
        double baseSize = FastMath.abs(gaussian(rand, 4.0, 1.0, 0.5));
        if (clouds) {
            // XY small, Z large.
            setScale(new double[]{
                    FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                    FastMath.abs(gaussian(rand, 1.0, 1.0, 0.2)),
                    FastMath.abs(gaussian(rand, 4.0, 2.0, 2.0))});
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
        // Frequency.
        if(clouds) {
            setFrequency(gaussian(rand, 1.0, 2.0, 0.6));
        } else {
            setFrequency(gaussian(rand, 0.5, 2.0, 0.01));
        }
        // Persistence.
        setPersistence(gaussian(rand, 0.5, 0.07, 0.3));
        // Lacunarity.
        setLacunarity(gaussian(rand, 2.0, 2.0, 1.5));
        // Octaves [1,8].
        if (type == NoiseType.VORONOI) {
            setOctaves((long) rand.nextInt(1, 3));
        } else {
            if(clouds) {
                setOctaves(rand.nextLong(4, 9));
            } else {
                setOctaves(rand.nextLong(1, 9));
            }
        }
        // Range.
        double minRange = rocky ? 0.1 : gaussian(rand, -0.5, 0.3);
        double maxRange = 0.5 + FastMath.abs(rand.nextDouble());
        setRange(new double[]{minRange, maxRange});
        // Power.
        if(clouds) {
            setPower(gaussian(rand, 1.0, 1.0, 0.5));
        } else {
            setPower(rocky ? 1.0 : gaussian(rand, 2.0, 2.0, 0.2));
        }
        // Turbulence.
        setTurbulence(true);
        // Ridge.
        setRidge(rand.nextBoolean());
    }

    public void print(Log log) {
        log.debug("Seed: " + seed);
        log.debug("Scale: " + Arrays.toString(scale));
        log.debug("Noise type: " + type);
        log.debug("Frequency: " + frequency);
        log.debug("Persistence: " + persistence);
        log.debug("Lacunarity: " + lacunarity);
        log.debug("Octaves: " + octaves);
        log.debug("Range: " + Arrays.toString(range));
        log.debug("Power: " + power);
        log.debug("Turbulence: " + turbulence);
        log.debug("Ridge: " + ridge);
    }

    @Override
    public void dispose() {
        if (fbSurface != null) {
            fbSurface.dispose();
            fbSurface = null;
        }
        if (fbMoisture != null) {
            fbMoisture.dispose();
            fbMoisture = null;
        }
        if (fbHeight != null) {
            fbHeight.dispose();
            fbHeight = null;
        }
        if (fbNoise != null) {
            fbNoise.dispose();
            fbNoise = null;
        }
    }
}
