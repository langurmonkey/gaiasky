/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.NoiseFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Noise extends PostProcessorEffect {
    private final NoiseFilter filter;

    public Noise(float viewportWidth, float viewportHeight) {
        this((int) viewportWidth, (int) viewportHeight);
    }

    public Noise(int viewportWidth, int viewportHeight) {
        filter = new NoiseFilter(viewportWidth, viewportHeight);
        disposables.add(filter);
    }

    public void setViewportSize(int width, int height) {
        filter.setViewportSize(width, height);
    }

    public void setRange(float a, float b) {
        filter.setRange(a, b);
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

    public void setSeed(float seed) {
        filter.setSeed(seed);
    }

    public void setFrequency(double persistence) {
        filter.setPersistence((float) persistence);
    }

    public void setFrequency(float persistence) {
        filter.setPersistence(persistence);
    }

    public void setPersistence(double persistence) {
        filter.setPersistence((float) persistence);
    }

    public void setPersistence(float persistence) {
        filter.setPersistence(persistence);
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

    public void setType(NoiseFilter.NoiseType type) {
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
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }
}
