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
import gaiasky.render.postprocess.filters.VignettingFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class Vignette extends PostProcessorEffect {
    private final VignettingFilter vignettingFilter;
    private final boolean controlSaturation;
    private final float oneOnW;
    private final float oneOnH;

    public Vignette(int viewportWidth, int viewportHeight, boolean controlSaturation) {
        this.controlSaturation = controlSaturation;
        oneOnW = 1f / (float) viewportWidth;
        oneOnH = 1f / (float) viewportHeight;
        vignettingFilter = new VignettingFilter(controlSaturation);
        disposables.add(vignettingFilter);
    }

    public boolean doesSaturationControl() {
        return controlSaturation;
    }

    public void setCoords(float x, float y) {
        vignettingFilter.setCoords(x, y);
    }

    public void setX(float x) {
        vignettingFilter.setX(x);
    }

    public void setY(float y) {
        vignettingFilter.setY(y);
    }

    public void setLutTexture(Texture texture) {
        vignettingFilter.setLut(texture);
    }

    public void setLutIndexVal(int index, int value) {
        vignettingFilter.setLutIndexVal(index, value);
    }

    public void setLutIndexOffset(float value) {
        vignettingFilter.setLutIndexOffset(value);
    }

    /** Specify the center, in screen coordinates. */
    public void setCenter(float x, float y) {
        vignettingFilter.setCenter(x * oneOnW, 1f - y * oneOnH);
    }

    public float getIntensity() {
        return vignettingFilter.getIntensity();
    }

    public void setIntensity(float intensity) {
        vignettingFilter.setIntensity(intensity);
    }

    public float getLutIntensity() {
        return vignettingFilter.getLutIntensity();
    }

    public void setLutIntensity(float value) {
        vignettingFilter.setLutIntensity(value);
    }

    public int getLutIndexVal(int index) {
        return vignettingFilter.getLutIndexVal(index);
    }

    public Texture getLut() {
        return vignettingFilter.getLut();
    }

    public float getCenterX() {
        return vignettingFilter.getCenterX();
    }

    public float getCenterY() {
        return vignettingFilter.getCenterY();
    }

    public float getCoordsX() {
        return vignettingFilter.getX();
    }

    public float getCoordsY() {
        return vignettingFilter.getY();
    }

    public float getSaturation() {
        return vignettingFilter.getSaturation();
    }

    public void setSaturation(float saturation) {
        vignettingFilter.setSaturation(saturation);
    }

    public float getSaturationMul() {
        return vignettingFilter.getSaturationMul();
    }

    public void setSaturationMul(float saturationMul) {
        vignettingFilter.setSaturationMul(saturationMul);
    }

    public boolean isGradientMappingEnabled() {
        return vignettingFilter.isGradientMappingEnabled();
    }

    @Override
    public void rebind() {
        vignettingFilter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        restoreViewport(dest);
        vignettingFilter.setInput(src).setOutput(dest).render();
    }

}
