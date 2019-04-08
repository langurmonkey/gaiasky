/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.system.StarGroupRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.CameraManager;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.FovCamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Renders the Gaia Field of View camera mode. Positions two cameras inside
 * gaia, each looking through one of the apertures, and renders them in the same
 * viewport with a CCD texture.
 * 
 * @author tsagrista
 *
 */
public class SGRFov extends SGRAbstract implements ISGR {

    public SGRFov() {
        super();
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {
        boolean postproc = postprocessCapture(ppb, fb, rw, rh);

        /** FIELD OF VIEW CAMERA - we only render the star group process **/

        FovCamera cam = ((CameraManager) camera).fovCamera;
        int fovmode = camera.getMode().getGaiaFovMode();
        if (fovmode == 1 || fovmode == 3) {
            cam.dirindex = 0;
            sgr.renderSystem(camera, t, rc, StarGroupRenderSystem.class);
        }

        if (fovmode == 2 || fovmode == 3) {
            cam.dirindex = 1;
            sgr.renderSystem(camera, t, rc, StarGroupRenderSystem.class);
        }

        postprocessRender(ppb, fb, postproc, camera, rw, rh);

    }

    @Override
    public void resize(int w, int h) {

    }

    @Override
    public void dispose() {
    }
}
