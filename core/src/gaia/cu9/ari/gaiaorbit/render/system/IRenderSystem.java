/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.utils.Array;
import gaia.cu9.ari.gaiaorbit.render.IRenderable;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext;
import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * A component that renders a type of objects.
 *
 * @author Toni Sagrista
 */
public interface IRenderSystem {

    RenderGroup getRenderGroup();

    void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc);

    void resize(int w, int h);

    void updateBatchSize(int w, int h);

}
