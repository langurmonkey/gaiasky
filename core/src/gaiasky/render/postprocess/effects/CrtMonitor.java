/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import gaiasky.render.postprocess.PostProcessor;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.BlurFilter;
import gaiasky.render.postprocess.filters.CombineFilter;
import gaiasky.render.postprocess.filters.CrtScreenFilter;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public final class CrtMonitor extends PostProcessorEffect {
    private final CrtScreenFilter crt;
    private final CombineFilter combineFilter;
    private PingPongBuffer pingPongBuffer = null;
    private FrameBuffer buffer = null;
    private BlurFilter blurFilter;
    private boolean blending = false;
    private int sFactor, dFactor;

    // the effect is designed to work on the whole screen area, no small/mid size tricks!
    public CrtMonitor(int fboWidth, int fboHeight, boolean barrelDistortion, boolean performBlur, CrtScreenFilter.RgbMode mode, int effectsSupport) {

        if (performBlur) {
            // Use RGB888 to force internal format GL_RGB16F, omitting the alpha channel.
            this.pingPongBuffer = PostProcessor.newPingPongBuffer(fboWidth,
                                                                  fboHeight,
                                                                  Pixmap.Format.RGB888,
                                                                  false,
                                                                  false,
                                                                  false,
                                                                  false);
            this.blurFilter = new BlurFilter(fboWidth, fboHeight);
            this.blurFilter.setPasses(1);
            this.blurFilter.setAmount(1f);
            // blur.setType( BlurType.Gaussian3x3b ); // high defocus
            this.blurFilter.setType(BlurFilter.BlurType.Gaussian3x3); // modern machines defocus
            this.disposables.addAll(pingPongBuffer, blurFilter);
        } else {
            this.buffer = new FrameBuffer(PostProcessor.getFramebufferFormat(), fboWidth, fboHeight, false);
            this.disposables.addAll(buffer);
        }

        combineFilter = new CombineFilter();
        crt = new CrtScreenFilter(barrelDistortion, mode, effectsSupport);
        disposables.addAll(combineFilter, crt);
    }

    public void enableBlending(int sFactor, int dFactor) {
        this.blending = true;
        this.sFactor = sFactor;
        this.dFactor = dFactor;
    }

    public void disableBlending() {
        this.blending = false;
    }

    // setters
    public void setTime(float elapsedSecs) {
        crt.setTime(elapsedSecs);
    }

    public void setColorOffset(float offset) {
        crt.setColorOffset(offset);
    }

    public void setChromaticDispersion(float redCyan, float blueYellow) {
        crt.setChromaticDispersion(redCyan, blueYellow);
    }

    public void setChromaticDispersionRC(float redCyan) {
        crt.setChromaticDispersionRC(redCyan);
    }

    public void setChromaticDispersionBY(float blueYellow) {
        crt.setChromaticDispersionBY(blueYellow);
    }

    public void setTint(float r, float g, float b) {
        crt.setTint(r, g, b);
    }

    public void setDistortion(float distortion) {
        crt.setDistortion(distortion);
    }

    // getters
    public CombineFilter getCombinePass() {
        return combineFilter;
    }

    public float getOffset() {
        return crt.getOffset();
    }

    public Vector2 getChromaticDispersion() {
        return crt.getChromaticDispersion();
    }

    public float getZoom() {
        return crt.getZoom();
    }

    public void setZoom(float zoom) {
        crt.setZoom(zoom);
    }

    public Color getTint() {
        return crt.getTint();
    }

    public void setTint(Color tint) {
        crt.setTint(tint);
    }

    public CrtScreenFilter.RgbMode getRgbMode() {
        return crt.getRgbMode();
    }

    public void setRgbMode(CrtScreenFilter.RgbMode mode) {
        crt.setRgbMode(mode);
    }

    @Override
    public void rebind() {
        crt.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        // the original scene
        Texture in = src.getColorBufferTexture();

        boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Texture out;

        if (blurFilter != null) {

            pingPongBuffer.begin();
            {
                // crt pass
                crt.setInput(in).setOutput(pingPongBuffer.getSourceBuffer()).render();

                // blur pass
                blurFilter.render(pingPongBuffer);
            }
            pingPongBuffer.end();

            out = pingPongBuffer.getResultTexture();
        } else {
            // crt pass
            crt.setInput(in).setOutput(buffer).render();

            out = buffer.getColorBufferTexture();
        }

        if (blending || blendingWasEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }

        if (blending) {
            Gdx.gl.glBlendFunc(sFactor, dFactor);
        }

        restoreViewport(dest);

        // do combine pass
        combineFilter.setOutput(dest).setInput(in, out).render();
    }

}
