/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.Texture3D;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.ProceduralSurfaceFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.render.util.NoiseType;

public final class ProceduralSurface extends PostProcessorEffect {
    private final ProceduralSurfaceFilter filter;

    public ProceduralSurface(int viewportWidth,
                             int viewportHeight,
                             boolean normalMap,
                             boolean emissiveMap) {
        filter = new ProceduralSurfaceFilter(viewportWidth, viewportHeight, normalMap, emissiveMap);
        disposables.add(filter);
    }

    public void setViewportSize(int width,
                                int height) {
        filter.setViewportSize(width, height);
    }

    public void setColor(float[] color) {
        filter.setColor(color[0], color[1], color[2], color[3]);
    }

    public void setScale(double[] scale) {
        filter.setScale((float) scale[0], (float) scale[1], (float) scale[2]);
    }

    public void setScale(float scaleX,
                         float scaleY,
                         float scaleZ) {
        filter.setScale(scaleX, scaleY, scaleZ);
    }

    public void setScale(float scale) {
        filter.setScale(scale);
    }

    public void setBaseLevel(float baseLevel) {
        filter.setBaseLevel(baseLevel);
    }

    public void setRemap(boolean remap) {
        filter.setRemap(remap);
    }

    public void setSeed(float seed) {
        filter.setSeed(seed);
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

    public void setOctaves(int octaves) {
        filter.setOctaves(octaves);
    }

    public void setSmoothing(boolean smoothing) {
        filter.setSmoothing(smoothing);
    }

    public void setTurbulence(boolean turbulence) {
        filter.setTurbulence(turbulence);
    }

    public void setRidge(boolean ridge) {
        filter.setRidge(ridge);
    }

    public void setChannels(int channels) {
        filter.setChannels(channels);
    }

    public void setType(NoiseType type) {
        filter.setType(type);
    }

    public void setLutTexture(Texture3D lut) {
        filter.setLutTexture(lut);
    }

    public void setLutHueShift(float lutHueShift) {
        filter.setLutHueShift(lutHueShift);
    }

    public void setLutSaturation(float lutSaturation) {
        filter.setLutSaturation(lutSaturation);
    }

    public void setLatitudeInfluence(float latitudeInfluence) {
        filter.setLatitudeInfluence(latitudeInfluence);
    }

    public void setPlainsHeight(float plainsHeight) {
        filter.setPlainsHeight(plainsHeight);
    }

    public void setPlainsSlope(float plainsSlope) {
        filter.setPlainsSlope(plainsSlope);
    }

    public void setWarpStrength(float warpStrength) {
        filter.setWarpStrength(warpStrength);
    }

    public void setWarpFrequency(float warpFrequency) {
        filter.setWarpFrequency(warpFrequency);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    public void render(FrameBuffer src,
                       FrameBuffer dest) {
        restoreViewport(dest);
        filter.setInput(src).setOutput(dest).render();
    }

    @Override
    public void render(FrameBuffer src,
                       FrameBuffer dest,
                       GaiaSkyFrameBuffer full,
                       GaiaSkyFrameBuffer half) {
    }
}