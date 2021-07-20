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

/**
 * Scene graph renderer that renders six scenes in the six cartesian
 * directions (front, back, right, left, up, down) to a cubemap.
 */
public abstract class SGRCubemap extends SGRAbstract {

    protected Vector3 aux1, aux2, aux3, dirBak, upBak, dirUpCrs;
    protected StretchViewport stretchViewport;
    // Frame buffers for each side of the cubemap
    protected Map<Integer, FrameBuffer> frameBufferCubeMap;

    // Backup of fov value
    protected float fovBak;
    // Angle from zenith, for planetarium mode
    protected float angleFromZenith = 0;

    // Frame buffers
    protected FrameBuffer zPosFb, zNegFb, xPosFb, xNegFb, yPosFb, yNegFb;
    // Flags
    protected boolean zPosFlag, zNegFlag, xPosFlag, xNegFlag, yPosFlag, yNegFlag;

    protected SGRCubemap() {
        super();
        aux1 = new Vector3();
        aux3 = new Vector3();
        aux2 = new Vector3();
        dirBak = new Vector3();
        upBak = new Vector3();
        dirUpCrs = new Vector3();
        stretchViewport = new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight());

        xPosFlag = true;
        xNegFlag = true;
        yPosFlag = true;
        yNegFlag = true;
        zPosFlag = true;
        zNegFlag = true;

        frameBufferCubeMap = new HashMap<>();
    }

    protected void renderCubemapSides(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, PostProcessBean ppb) {
        PerspectiveCamera cam = camera.getCamera();

        // Backup fov, direction and up
        fovBak = cam.fieldOfView;
        dirBak.set(cam.direction);
        upBak.set(cam.up);
        // dirUpCrs <- dir X up
        dirUpCrs.set(dirBak).crs(upBak).nor().scl(-1f);

        EventManager.instance.post(Events.FOV_CHANGED_CMD, 90f);

        // The sides of the cubemap must be square. We use the max of our resolution
        int wh = GlobalConf.program.CUBEMAP_FACE_RESOLUTION;
        zPosFb = getFrameBuffer(wh, wh, 0);
        zNegFb = getFrameBuffer(wh, wh, 1);
        xPosFb = getFrameBuffer(wh, wh, 2);
        xNegFb = getFrameBuffer(wh, wh, 3);
        yPosFb = getFrameBuffer(wh, wh, 4);
        yNegFb = getFrameBuffer(wh, wh, 5);

        Viewport viewport = stretchViewport;
        viewport.setCamera(cam);
        viewport.setWorldSize(wh, wh);
        viewport.setScreenBounds(0, 0, wh, wh);
        viewport.apply();

        // RIGHT +X
        if (xPosFlag) {
            rc.cubemapSide = CubemapSide.SIDE_RIGHT;

            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith);
            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith).rotate(cam.up, -90);
            cam.update();

            renderFace(xPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (xNegFlag) {
            // LEFT -X
            rc.cubemapSide = CubemapSide.SIDE_LEFT;

            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith);
            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith).rotate(cam.up, 90);
            cam.update();

            renderFace(xNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (yPosFlag) {
            // UP +Y
            rc.cubemapSide = CubemapSide.SIDE_UP;

            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith + 90);
            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith + 90);
            cam.update();

            renderFace(yPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (yNegFlag) {
            // DOWN -Y
            rc.cubemapSide = CubemapSide.SIDE_DOWN;

            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith - 90);
            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith - 90);
            cam.update();

            renderFace(yNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (zPosFlag) {
            // FRONT +Z
            rc.cubemapSide = CubemapSide.SIDE_FRONT;

            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith);
            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith);
            cam.update();

            renderFace(zPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (zNegFlag) {
            // BACK -Z
            rc.cubemapSide = CubemapSide.SIDE_BACK;

            cam.up.set(upBak).rotate(dirUpCrs, -angleFromZenith);
            cam.direction.set(dirBak).rotate(dirUpCrs, -angleFromZenith).rotate(upBak, -180);
            cam.update();

            renderFace(zNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        // Restore camera parameters
        cam.direction.set(dirBak);
        cam.up.set(upBak);

        rc.cubemapSide = CubemapSide.SIDE_NONE;
    }

    protected void postRender(FrameBuffer fb) {
        if (fb != null)
            fb.end();

        // ensure default texture unit #0 is active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        // Restore fov
        EventManager.instance.post(Events.FOV_CHANGED_CMD, fovBak);
    }

    protected void renderFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        renderRegularFace(fb, camera, sgr, ppb, rw, rh, wh, t);
    }

    protected void renderRegularFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        sgr.renderGlowPass(camera, null);

        boolean postProcess = postProcessCapture(ppb, fb, wh, wh);
        sgr.renderScene(camera, t, rc);
        sendOrientationUpdate(camera.getCamera(), rw, rh);
        postProcessRender(ppb, fb, postProcess, camera, rw, rh);
    }

    protected int getKey(int w, int h, int extra) {
        return w * 100 + h * 10 + extra;
    }

    protected FrameBuffer getFrameBuffer(int w, int h) {
        return getFrameBuffer(w, h, 0);
    }

    protected FrameBuffer getFrameBuffer(int w, int h, int extra) {
        int key = getKey(w, h, extra);
        if (!frameBufferCubeMap.containsKey(key)) {
            frameBufferCubeMap.put(key, new FrameBuffer(Format.RGB888, w, h, true));
        }
        return frameBufferCubeMap.get(key);
    }
}
