/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.scene.camera.ICamera;

public interface IRenderMode extends Disposable {
    /**
     * Renders the scene.
     *
     * @param sgr    The scene renderer object.
     * @param camera The camera.
     * @param t      The time in seconds since the start.
     * @param rw     The width of the buffer.
     * @param rh     The height of the buffer.
     * @param tw     The final target width, usually of the screen.
     * @param th     The final target height, usually of the screen.
     * @param fb     The frame buffer, if any.
     * @param ppb    The post process bean.
     */
    void render(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb);

    /**
     * Resizes the assets of this renderer to the given new size
     *
     * @param rw New render buffer width.
     * @param rh New render buffer height.
     * @param tw New target (screen) width.
     * @param th New target (screen) height.
     */
    void resize(final int rw, final int rh, final int tw, final int th);

    RenderingContext getRenderingContext();

    FrameBuffer getResultBuffer();

}
