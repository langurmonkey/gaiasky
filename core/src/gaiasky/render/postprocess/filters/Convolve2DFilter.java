/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.postprocess.filters;

import gaiasky.render.postprocess.util.PingPongBuffer;

public final class Convolve2DFilter extends MultipassFilter {
    public final int radius;
    public final int length; // NxN taps filter, w/ N=length

    public final float[] weights, offsetsHor, offsetsVert;

    private final Convolve1DFilter hor;
    private final Convolve1DFilter vert;

    public Convolve2DFilter(int radius) {
        this.radius = radius;
        length = (radius * 2) + 1;

        hor = new Convolve1DFilter(length);
        vert = new Convolve1DFilter(length, hor.weights);

        weights = hor.weights;
        offsetsHor = hor.offsets;
        offsetsVert = vert.offsets;
    }

    public void dispose() {
        hor.dispose();
        vert.dispose();
    }

    public void upload() {
        rebind();
    }

    @Override
    public void rebind() {
        hor.rebind();
        vert.rebind();
    }

    @Override
    public void render(PingPongBuffer buffer) {
        hor.setInput(buffer.capture()).render();
        vert.setInput(buffer.capture()).render();
    }
}
