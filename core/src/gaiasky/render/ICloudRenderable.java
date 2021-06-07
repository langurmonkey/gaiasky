/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.util.gdx.IntModelBatch;

/**
 * To be implemented by all entities wanting to render a clouds layer
 */
public interface ICloudRenderable extends IRenderable {

    /**
     * Renders the clouds
     *
     * @param modelBatch The model batch to use
     * @param alpha      The opacity
     * @param t          The time in seconds since the start
     */
    void renderClouds(IntModelBatch modelBatch, float alpha, double t);
}
