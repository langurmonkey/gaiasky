/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.util.gdx.IntModelBatch;

import gaiasky.util.math.Vector3d;

/**
 * To be implemented by all entities wanting to render an atmosphere.
 *
 * @author Toni Sagrista
 */
public interface IAtmosphereRenderable extends IRenderable {

    /**
     * Renders the atmosphere.
     *
     * @param modelBatch The model batch to use.
     * @param alpha      The opacity.
     * @param t          The time in seconds since the start.
     * @param vroffset   Positional offset in vr mode, if any.
     */
    void renderAtmosphere(IntModelBatch modelBatch, float alpha, double t, Vector3d vroffset);
}
