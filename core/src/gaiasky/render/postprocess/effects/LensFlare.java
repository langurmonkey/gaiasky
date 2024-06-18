/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
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
    private PingPongBuffer pingPongBuffer;
    private final LensFlareFilter flare;
    private LensDirtFilter dirt;
    private CombineFilter combineFilter;
    private final boolean useLensDirt;

    /**
     * Creates a new lens flare effect with the given parameters.
     *
     * @param width       The viewport width.
     * @param height      The viewport height.
     * @param intensity   The intensity of the effect.
     * @param type        The type, 0 for simple, 1 for complex.
     * @param useLensDirt Use lens dirt effect. Warning: very slow!
     */
    public LensFlare(int width, int height, float intensity, int type, boolean useLensDirt) {
        flare = new LensFlareFilter(width, height, intensity, type, useLensDirt);

        disposables.add(flare);
        if (useLensDirt) {
            pingPongBuffer = PostProcessor.newPingPongBuffer(width, height, PostProcessor.getFramebufferFormat(), false);
            dirt = new LensDirtFilter(true);
            combineFilter = new CombineFilter();
            disposables.addAll(pingPongBuffer, dirt, combineFilter);
        }
        this.useLensDirt = useLensDirt;
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
    public void rebind() {
        flare.rebind();
        if (useLensDirt) {
            combineFilter.rebind();
            pingPongBuffer.rebind();
        }
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer main) {
        if (useLensDirt) {
            boolean blendingWasEnabled = PostProcessor.isStateEnabled(GL20.GL_BLEND);
            Gdx.gl.glDisable(GL20.GL_BLEND);

            Texture sourceTexture = src.getColorBufferTexture();

            pingPongBuffer.begin();
            {
                // Apply flare.
                flare.setInput(sourceTexture).setOutput(pingPongBuffer.getSourceBuffer()).render();
                // Apply dirt.
                dirt.setInput(pingPongBuffer.getSourceBuffer()).setOutput(pingPongBuffer.getResultBuffer()).render();

            }
            pingPongBuffer.end();

            if (blendingWasEnabled) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            restoreViewport(dest);

            // Mix original with flare.
            combineFilter.setOutput(dest).setInput(sourceTexture, pingPongBuffer.getResultTexture()).render();
        } else {
            restoreViewport(dest);
            flare.setInput(src).setOutput(dest).render();
        }
    }
}
