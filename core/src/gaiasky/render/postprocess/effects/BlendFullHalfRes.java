/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.effects;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.render.postprocess.PostProcessorEffect;
import gaiasky.render.postprocess.filters.BlendFullHalfResFilter;
import gaiasky.render.util.GaiaSkyFrameBuffer;
import gaiasky.util.Constants;

public class BlendFullHalfRes extends PostProcessorEffect {
    /**
     * Anything nearer than this is "near scene" to the full/half composite: about 670 AU, which is
     * within the solar system and far inside the kpc distance of the volumes this pass composites.
     * It is a physical distance on purpose - see {@code fullhalfresblend.frag.glsl}, whose test used
     * to be an absolute 1e5 internal units, i.e. this distance on the desktop and 0.067 AU at scale
     * 1e4.
     */
    private static final double NEAR_SCENE_M = 1.0e14;

    private final BlendFullHalfResFilter filter;

    public BlendFullHalfRes() {
        filter = new BlendFullHalfResFilter();
        disposables.add(filter);
    }

    @Override
    public void rebind() {
        filter.rebind();
    }

    @Override
    public void render(FrameBuffer src, FrameBuffer dest, GaiaSkyFrameBuffer full, GaiaSkyFrameBuffer half) {
        // Z-far and K.
        var cam = GaiaSky.instance.getICamera();
        filter.setZFarK((float) cam.getFar(), Constants.getCameraK());
        filter.setNearScene((float) (NEAR_SCENE_M * Constants.M_TO_U));

        restoreViewport(dest);
        filter.setInput(full, half).setOutput(dest).render();
    }
}
