/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Defines the interface common to all render systems.
 */
public interface IRenderSystem extends Disposable {

    RenderGroup getRenderGroup();

    /**
     * Renders the given list of renderable objects.
     * @param renderables The list of objects to render.
     * @param camera The camera object.
     * @param t The time, in seconds, since the session start.
     * @param rc The rendering context object.
     */
    void render(Array<IRenderable> renderables, ICamera camera, double t, RenderingContext rc);

    /**
     * Resize the current render target with the given width and height.
     * @param w The new width.
     * @param h The new height.
     */
    void resize(int w, int h);

    /**
     * Updates the size of object batches, if any.
     * @param w The new width.
     * @param h The new height.
     */
    void updateBatchSize(int w, int h);

}
