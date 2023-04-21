/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.contrib.postprocess.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.util.gdx.contrib.postprocess.PostProcessor;
import gaiasky.util.gdx.contrib.postprocess.PostProcessorEffect;
import gaiasky.util.gdx.contrib.postprocess.filters.Combine;
import gaiasky.util.gdx.contrib.postprocess.filters.LensDirt;
import gaiasky.util.gdx.contrib.postprocess.filters.LensFlareFilter;
import gaiasky.util.gdx.contrib.postprocess.utils.PingPongBuffer;
import gaiasky.util.gdx.contrib.utils.GaiaSkyFrameBuffer;

public class LensFlare extends PostProcessorEffect {
    private final PingPongBuffer pingPongBuffer;
    private final LensFlareFilter flare;
    private final LensDirt dirt;
    private final Combine combine;
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

        if (useLensDirt) {
            pingPongBuffer = PostProcessor.newPingPongBuffer(width, height, PostProcessor.getFramebufferFormat(), false);
            dirt = new LensDirt(true);
            combine = new Combine();
        } else {
            pingPongBuffer = null;
            dirt = null;
            combine = null;
        }
        this.useLensDirt = useLensDirt;
    }

    public void setViewport(int width, int height) {
        flare.setViewportSize(width, height);
    }

    public void setLightPositions(int nLights, float[] vec) {
        flare.setLightPositions(nLights, vec);
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
            combine.rebind();
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
            combine.setOutput(dest).setInput(sourceTexture, pingPongBuffer.getResultTexture()).render();
        } else {
            restoreViewport(dest);
            flare.setInput(src).setOutput(dest).render();
        }
    }

    @Override
    public void dispose() {
        flare.dispose();
        if (combine != null) {
            combine.dispose();
        }
        if (dirt != null) {
            dirt.dispose();
        }
        if (combine != null) {
            combine.dispose();
        }
    }
}
