/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.postprocess.PostProcessor;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.CombineFilter;
import gaiasky.render.postprocess.filters.LensDirtFilter;
import gaiasky.render.postprocess.filters.LensFlareFilter;
import gaiasky.render.postprocess.util.PingPongBuffer;
import gaiasky.render.util.GaiaSkyFrameBuffer;

public class LensFlare extends PostProcessorEffect {
    private final PingPongBuffer pingPongBuffer;
    private final LensFlareFilter flare;
    private LensDirtFilter dirt;
    private final CombineFilter combineFilter;
    private final boolean useLensDirt;

    /**
     * Creates a new lens flare effect with the given parameters.
     *
     * @param width       The viewport width.
     * @param height      The viewport height.
     * @param intensity   The intensity of the effect.
     * @param type        The type, 0 for simple, 1 for complex.
     */
    public LensFlare(int width, int height, float intensity, int type, boolean useLensDirt) {
        this.useLensDirt = useLensDirt;
        this.flare = new LensFlareFilter(width, height, intensity, type, useLensDirt);
        // AMD APUs have trouble converting from regular buffers to float buffers,
        // leading to artifacts in the lens flare when not using a float buffer!
        // Use RGB888 to force internal format GL_RGB16F, omitting the alpha channel.
        this.pingPongBuffer = PostProcessor.newPingPongBuffer(width,
                                                              height,
                                                              Pixmap.Format.RGB888,
                                                              false,
                                                              false,
                                                              false,
                                                              false);
        this.combineFilter = new CombineFilter();

        disposables.addAll(flare, pingPongBuffer, combineFilter);

        if (useLensDirt) {
            this.dirt = new LensDirtFilter();
            disposables.add(dirt);
        }
    }

    public void setViewport(int width, int height) {
        flare.setViewportSize(width, height);
    }

    public void setLightPositions(int nLights, float[] positions, float[] intensities) {
        flare.setLightPositions(nLights, positions, intensities);
    }

    public void setIntensity(float intensity) {
        flare.setIntensity(intensity);
    }

    public void setColor(float[] color) {
        flare.setColor(color);
    }

    public void setLensDirtTexture(Texture tex) {
        if (useLensDirt) {
            dirt.setLensDirtTexture(tex);
        }
    }

    public void setLensStarburstTexture(Texture tex) {
        if (useLensDirt) {
            dirt.setLensStarburstTexture(tex);
        }
    }

    public void setStarburstOffset(float offset) {
        if (useLensDirt) {
            dirt.setStarburstOffset(offset);
        }
    }

    @Override
    public void updateShaders() {
        super.updateShaders();
        flare.updateProgram();
        if (useLensDirt) {
            dirt.updateProgram();
        }
    }

    @Override
    public void rebind() {
        flare.rebind();
        combineFilter.rebind();
        pingPongBuffer.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        Texture sourceTexture = src.getColorBufferTexture();

        pingPongBuffer.begin();
        {
            if (useLensDirt) {
                // Apply flare.
                flare.setInput(sourceTexture).setOutput(pingPongBuffer.getSourceBuffer()).render();
                // Apply dirt.
                dirt.setInput(pingPongBuffer.getSourceBuffer()).setOutput(pingPongBuffer.getResultBuffer()).render();
            } else {
                // Apply flare only.
                flare.setInput(sourceTexture).setOutput(pingPongBuffer.getResultBuffer()).render();
            }

        }
        pingPongBuffer.end();

        if (blendingWasEnabled) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
        }

        restoreViewport(dest);

        // Mix original with flare.
        combineFilter.setInput(sourceTexture, pingPongBuffer.getResultTexture()).setOutput(dest).render();
    }
}
