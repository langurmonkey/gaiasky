/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.camera.ICamera;

public class RenderModeMain extends RenderModeAbstract implements IRenderMode {

    public RenderModeMain() {
        super();
    }

    @Override
    public void render(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        boolean postProcess = postProcessCapture(ppb, fb, rw, rh, ppb::capture);

        // Viewport
        extendViewport.setCamera(camera.getCamera());
        extendViewport.setWorldSize(rw, rh);
        extendViewport.setScreenSize(rw, rh);
        extendViewport.apply();

        // Render
        try {
            sgr.renderScene(camera, t, rc);
        } finally {
            // GLFW reports a window size of 0x0 with AMD Graphics on Windows when minimizing.
            if (rw > 0 && rh > 0) {
                sendOrientationUpdate(camera.getCamera(), rw, rh);
                postProcessRender(ppb, fb, postProcess, camera, tw, th);
            }
        }

    }

    @Override
    public void resize(int rw, int rh, int tw, int th) {
    }

    @Override
    public void dispose() {
    }

}
