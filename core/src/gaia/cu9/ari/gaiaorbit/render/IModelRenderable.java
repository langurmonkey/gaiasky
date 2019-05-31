/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.g3d.ModelBatch;

/**
 * Interface to implement by all the entities that can be rendered as a model.
 *
 * @author Toni Sagrista
 */
public interface IModelRenderable extends IRenderable {

    void render(ModelBatch modelBatch, float alpha, double t, RenderingContext rc);

    boolean hasAtmosphere();

}
