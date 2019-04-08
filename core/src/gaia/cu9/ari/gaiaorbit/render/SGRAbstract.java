/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Abstract implementation with some useful methods for all SGRs.
 * 
 * @author tsagrista
 *
 */
public class SGRAbstract {

    protected RenderingContext rc;
    /** Viewport to use in normal mode **/
    protected Viewport extendViewport;

    public SGRAbstract() {
        // Render context
        rc = new RenderingContext();
        // Viewport
        extendViewport = new ExtendViewport(200, 200);
    }

    protected boolean postprocessCapture(PostProcessBean ppb, FrameBuffer fb, int rw, int rh) {
        boolean postproc = ppb.capture();
        if (postproc) {
            rc.ppb = ppb;
        } else {
            rc.ppb = null;
        }
        rc.fb = fb;
        rc.set(rw, rh);
        return postproc;
    }

    protected void postprocessRender(PostProcessBean ppb, FrameBuffer fb, boolean postproc, ICamera camera, int rw, int rh) {
        ppb.render(fb);

        // Render camera
        if (fb != null && postproc) {
            fb.begin();
        }
        camera.render(rw, rh);
        if (fb != null && postproc) {
            fb.end();
        }
    }

}
