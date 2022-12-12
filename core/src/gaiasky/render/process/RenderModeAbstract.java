/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderingContext;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;

/**
 * Abstract implementation with some useful methods for all SGRs.
 */
public class RenderModeAbstract {

    protected FrameBuffer resultBuffer;
    protected RenderingContext rc;
    /** Viewport to use in normal mode **/
    protected Viewport extendViewport;

    public RenderModeAbstract() {
        // Render context
        rc = new RenderingContext();
        // Viewport
        extendViewport = new ExtendViewport(200, 200);
    }

    protected boolean postProcessCapture(PostProcessBean ppb, FrameBuffer fb, int rw, int rh) {
        boolean postProcess = ppb.capture();
        if (postProcess) {
            rc.ppb = ppb;
        } else {
            rc.ppb = null;
        }
        rc.fb = fb;
        rc.set(rw, rh);
        return postProcess;
    }

    protected void postProcessRender(PostProcessBean ppb, FrameBuffer fb, boolean postproc, ICamera camera, int rw, int rh) {
        ppb.render(fb);

        // Render camera
        if (!Settings.settings.runtime.openVr) {
            if (fb != null && postproc) {
                fb.begin();
            }
            camera.render(rw, rh);
            if (fb != null && postproc) {
                fb.end();
            }
        }

        resultBuffer = fb != null ? fb : ppb.pp.getCombinedBuffer().getResultBuffer();
    }

    public RenderingContext getRenderingContext() {
        return rc;
    }

    public FrameBuffer getResultBuffer() {
        return resultBuffer;
    }

    protected void sendOrientationUpdate(PerspectiveCamera cam, int w, int h) {
        EventManager.publish(Event.CAMERA_ORIENTATION_UPDATE, this, cam, w, h);
    }

}
