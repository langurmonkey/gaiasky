/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.BiomeFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Biome extends PostProcessorEffect {
    private final BiomeFilter filter;

    public Biome(int viewportWidth, int viewportHeight, int targets, String shader) {
        filter = new BiomeFilter(viewportWidth, viewportHeight, targets, shader);
        disposables.add(filter);

    }
    public Biome(int viewportWidth, int viewportHeight, int targets) {
        this(viewportWidth, viewportHeight, targets, "biome");
    }

    public void setViewportSize(int width, int height) {
        filter.setViewportSize(width, height);
    }

    public void setColor(float[] color) {
        filter.setColor(color[0], color[1], color[2], color[3]);
    }

    public void setScale(double[] scale) {
        filter.setScale((float) scale[0], (float) scale[1], (float) scale[2]);
    }

    public void setScale(float scaleX, float scaleY, float scaleZ) {
        filter.setScale(scaleX, scaleY, scaleZ);
    }

    public void setScale(float scale) {
        filter.setScale(scale);
    }

    public void setBaseLevel(float baseLevel) {
        filter.setBaseLevel(baseLevel);
    }

    public void setSeed(float seed) {
        filter.setSeed(seed);
    }

    public void setAmplitude(double amplitude) {
        filter.setAmplitude((float) amplitude);
    }

    public void setAmplitude(float amplitude) {
        filter.setAmplitude(amplitude);
    }

    public void setPersistence(double persistence) {
        filter.setPersistence((float) persistence);
    }

    public void setPersistence(float persistence) {
        filter.setPersistence(persistence);
    }

    public void setFrequency(double frequency) {
        filter.setFrequency((float) frequency);
    }

    public void setFrequency(float frequency) {
        filter.setFrequency(frequency);
    }

    public void setLacunarity(double lacunarity) {
        filter.setLacunarity((float) lacunarity);
    }

    public void setLacunarity(float lacunarity) {
        filter.setLacunarity(lacunarity);
    }

    public void setPower(double power) {
        filter.setPower((float) power);
    }

    public void setPower(float power) {
        filter.setPower(power);
    }

    public void setOctaves(int octaves) {
        filter.setOctaves(octaves);
    }

    public void setTurbulence(boolean turbulence) {
        filter.setTurbulence(turbulence);
    }

    public void setRidge(boolean ridge) {
        filter.setRidge(ridge);
    }

    public void setNumTerraces(int numTerraces) {
        filter.setNumTerraces(numTerraces);
    }

    public void setTerraceExp(float terraceExp) {
        filter.setTerraceExp(terraceExp);
    }

    public void setChannels(int channels) {
        filter.setChannels(channels);
    }

    public void setType(BiomeFilter.NoiseType type) {
        filter.setType(type);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void render(FrameBuffer src, FrameBuffer dest) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
    }
}
