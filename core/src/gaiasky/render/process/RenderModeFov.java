/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.camera.CameraManager;
import gaiasky.scene.camera.FovCamera;
import gaiasky.scene.camera.ICamera;

/**
 * Renders the Gaia Field of View camera mode. Positions two cameras inside
 * gaia, each looking through one of the apertures, and renders them in the same
 * viewport with a CCD texture
 */
public class RenderModeFov extends RenderModeAbstract implements IRenderMode {

    public RenderModeFov() {
        super();
    }

    @Override
    public void render(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        boolean postProcess = postProcessCapture(ppb, fb, tw, th, ppb::capture);

        // Viewport
        extendViewport.setCamera(camera.getCamera());
        extendViewport.setWorldSize(rw, rh);
        extendViewport.setScreenSize(rw * rw / tw, rh * rh / th);
        extendViewport.apply();

        /* FIELD OF VIEW CAMERA - we only render the star group process */
        FovCamera cam = ((CameraManager) camera).fovCamera;
        int fovMode = camera.getMode().getGaiaFovMode();
        if (fovMode == 1 || fovMode == 3) {
            cam.dirIndex = 0;
            sgr.renderScene(camera, t, rc);
        }

        if (fovMode == 2 || fovMode == 3) {
            cam.dirIndex = 1;
            sgr.renderScene(camera, t, rc);
        }

        // GLFW reports a window size of 0x0 with AMD Graphics on Windows when minimizing
        if (rw > 0 && rh > 0) {
            sendOrientationUpdate(camera.getCamera(), rw, rh);
            postProcessRender(ppb, fb, postProcess, camera, rw, rh);
        }

    }

    @Override
    public void resize(int rw, int rh, int tw, int th) {
    }

    @Override
    public void dispose() {
    }
}
