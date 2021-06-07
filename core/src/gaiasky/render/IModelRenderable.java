/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import gaiasky.util.gdx.IntModelBatch;

/**
 * Interface to implement by all the entities that can be rendered as a model
 */
public interface IModelRenderable extends IRenderable {

    void render(IntModelBatch modelBatch, float alpha, double t, RenderingContext rc);

    boolean hasAtmosphere();

}
