/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Normal SGR, takes care of the regular to-screen rendering with no strange
 * modes (stereoscopic, planetarium, cubemap) active
 */
public class SGR extends SGRAbstract implements ISGR {

    SGR() {
        super();
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        boolean postProcess = postProcessCapture(ppb, fb, rw, rh);

        // Viewport
        extendViewport.setCamera(camera.getCamera());
        extendViewport.setWorldSize(rw, rh);
        extendViewport.setScreenSize(rw * rw / tw, rh * rh / th);
        extendViewport.apply();

        // Render
        sgr.renderScene(camera, t, rc);

        // Uncomment this to show the shadow map
        //if (GlobalConf.scene.SHADOW_MAPPING) {
        //    float screenSize = 300 * GlobalConf.SCALE_FACTOR;
        //    int s = GlobalConf.scene.SHADOW_MAPPING_RESOLUTION;
        //    float scl = screenSize / (float) s;
        //    // Render shadow map
        //    sb.begin();
        //    for (int i = 0; i < sgr.shadowMapFb.length; i++) {
        //        sb.draw(sgr.shadowMapFb[i].getColorBufferTexture(), 0, 0, 0, 0, s, s, scl, scl, 0f, 0, 0, s, s, false, false);
        //    }
        //    sb.end();
        //}

        // GLFW reports a window size of 0x0 with AMD Graphics on Windows when minimizing
        if (rw > 0 && rh > 0) {
            sendOrientationUpdate(camera.getCamera(), rw, rh);
            postProcessRender(ppb, fb, postProcess, camera, tw, th);
        }

    }

    @Override
    public void resize(int w, int h) {
    }

    @Override
    public void dispose() {
    }

}
