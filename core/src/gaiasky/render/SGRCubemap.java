/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;

import java.util.HashMap;
import java.util.Map;

public abstract class SGRCubemap extends SGRAbstract {

    protected Vector3 aux1, aux2, aux3, dirbak, upbak;
    protected StretchViewport stretchViewport;
    // Frame buffers for each side of the cubemap
    protected Map<Integer, FrameBuffer> fbcm;

    // Backup of fov value
    protected float fovbak;

    // Frame buffers
    protected FrameBuffer zposfb, znegfb, xposfb, xnegfb, yposfb, ynegfb;

    protected SGRCubemap() {
        super();
        aux1 = new Vector3();
        aux3 = new Vector3();
        aux2 = new Vector3();
        dirbak = new Vector3();
        upbak = new Vector3();
        stretchViewport = new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight());

        fbcm = new HashMap<>();
    }

    protected void renderCubemapSides(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, PostProcessBean ppb) {
        PerspectiveCamera cam = camera.getCamera();

        // Backup fov, direction and up
        fovbak = cam.fieldOfView;
        dirbak.set(cam.direction);
        upbak.set(cam.up);

        EventManager.instance.post(Events.FOV_CHANGED_CMD, 90f);

        // The sides of the cubemap must be square. We use the max of our resolution
        int wh = GlobalConf.scene.CUBEMAP_FACE_RESOLUTION;
        zposfb = getFrameBuffer(wh, wh, 0);
        znegfb = getFrameBuffer(wh, wh, 1);
        xposfb = getFrameBuffer(wh, wh, 2);
        xnegfb = getFrameBuffer(wh, wh, 3);
        yposfb = getFrameBuffer(wh, wh, 4);
        ynegfb = getFrameBuffer(wh, wh, 5);

        Viewport viewport = stretchViewport;
        viewport.setCamera(cam);
        viewport.setWorldSize(wh, wh);
        viewport.setScreenBounds(0, 0, wh, wh);
        viewport.apply();

        // RIGHT +X
        rc.cubemapSide = CubemapSide.SIDE_RIGHT;

        cam.up.set(upbak);
        cam.direction.set(dirbak).rotate(upbak, -90);
        cam.update();

        renderFace(xposfb, camera, sgr, ppb, rw, rh, wh, t);

        // LEFT -X
        rc.cubemapSide = CubemapSide.SIDE_LEFT;

        cam.up.set(upbak);
        cam.direction.set(dirbak).rotate(upbak, 90);
        cam.update();

        renderFace(xnegfb, camera, sgr, ppb, rw, rh, wh, t);

        // UP +Y
        rc.cubemapSide = CubemapSide.SIDE_UP;

        aux1.set(dirbak);
        aux2.set(upbak);
        aux1.crs(aux2).scl(-1);
        cam.direction.set(dirbak).rotate(aux1, 90);
        cam.up.set(upbak).rotate(aux1, 90);
        cam.update();

        renderFace(yposfb, camera, sgr, ppb, rw, rh, wh, t);

        // DOWN -Y
        rc.cubemapSide = CubemapSide.SIDE_DOWN;

        aux1.set(dirbak);
        aux2.set(upbak);
        aux1.crs(aux2).scl(-1);
        cam.direction.set(dirbak).rotate(aux1, -90);
        cam.up.set(upbak).rotate(aux1, -90);
        cam.update();

        renderFace(ynegfb, camera, sgr, ppb, rw, rh, wh, t);

        // FRONT +Z
        rc.cubemapSide = CubemapSide.SIDE_FRONT;

        cam.direction.set(dirbak);
        cam.up.set(upbak);
        cam.update();

        renderFace(zposfb, camera, sgr, ppb, rw, rh, wh, t);

        // BACK -Z
        rc.cubemapSide = CubemapSide.SIDE_BACK;

        cam.up.set(upbak);
        cam.direction.set(dirbak).rotate(upbak, -180);
        cam.update();

        renderFace(znegfb, camera, sgr, ppb, rw, rh, wh, t);

        // Restore camera parameters
        cam.direction.set(dirbak);
        cam.up.set(upbak);
            
        rc.cubemapSide = CubemapSide.SIDE_NONE;
    }

    protected void postRender(FrameBuffer fb) {
        if (fb != null)
            fb.end();

        // ensure default texture unit #0 is active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        // Restore fov
        EventManager.instance.post(Events.FOV_CHANGED_CMD, fovbak);
    }

    protected void renderFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        renderRegularFace(fb, camera, sgr, ppb, rw, rh, wh, t);
    }

    protected void renderRegularFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        sgr.renderGlowPass(camera, null, 0);

        boolean postproc = postprocessCapture(ppb, fb, wh, wh);
        sgr.renderScene(camera, t, rc);
        postprocessRender(ppb, fb, postproc, camera, rw, rh);
    }

    protected int getKey(int w, int h, int extra) {
        return w * 100 + h * 10 + extra;
    }

    protected FrameBuffer getFrameBuffer(int w, int h) {
        return getFrameBuffer(w, h, 0);
    }

    protected FrameBuffer getFrameBuffer(int w, int h, int extra) {
        int key = getKey(w, h, extra);
        if (!fbcm.containsKey(key)) {
            fbcm.put(key, new FrameBuffer(Format.RGB888, w, h, true));
        }
        return fbcm.get(key);
    }
}
