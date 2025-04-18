/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.system;

import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.RenderGroup;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.camera.ICamera;

import java.util.List;

public interface IRenderSystem extends Disposable, Comparable<IRenderSystem> {

    RenderGroup getRenderGroup();

    /**
     * Renders the given list of renderable objects.
     *
     * @param renderables The list of objects to render.
     * @param camera      The camera object.
     * @param t           The time, in seconds, since the session start.
     * @param rc          The rendering context object.
     */
    void render(List<IRenderable> renderables, ICamera camera, double t, RenderingContext rc);

    /**
     * Resize the current render target with the given width and height.
     *
     * @param w The new width.
     * @param h The new height.
     */
    void resize(int w, int h);

    /**
     * Updates the size of object batches, if any.
     *
     * @param w The new width.
     * @param h The new height.
     */
    void updateBatchSize(int w, int h);

}
