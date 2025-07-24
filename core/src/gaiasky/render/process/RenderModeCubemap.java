/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.process;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderingContext.CubemapSide;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.render.postprocess.effects.Mosaic;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the rendering of each of the sides of a cubemap in the directions X, -X, Y, -Y, Z, -Z.
 */
public abstract class RenderModeCubemap extends RenderModeAbstract {

    private final Mosaic mosaic;
    protected Vector3 aux1, aux2, aux3, dirEffective, upEffective, dirBackup, upBackup, dirUpCrs;
    protected StretchViewport stretchViewport;
    // Frame buffers for each side of the cubemap
    protected Map<Integer, FrameBuffer> frameBufferCubeMap;
    // Backup of fov value
    protected float fovBackup;
    // Frame buffers
    protected FrameBuffer zPosFb, zNegFb, xPosFb, xNegFb, yPosFb, yNegFb;
    // Flags
    protected boolean zPosFlag, zNegFlag, xPosFlag, xNegFlag, yPosFlag, yNegFlag;

    protected float angleFromZenith = 0;

    protected RenderModeCubemap() {
        super();
        aux1 = new Vector3();
        aux3 = new Vector3();
        aux2 = new Vector3();
        dirEffective = new Vector3();
        dirBackup = new Vector3();
        upEffective = new Vector3();
        upBackup = new Vector3();
        dirUpCrs = new Vector3();
        stretchViewport = new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight());

        mosaic = new Mosaic(0, 0);

        xPosFlag = true;
        xNegFlag = true;
        yPosFlag = true;
        yNegFlag = true;
        zPosFlag = true;
        zNegFlag = true;

        frameBufferCubeMap = new HashMap<>();
    }

    protected void renderCubemapSides(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, PostProcessBean ppb) {
        PerspectiveCamera cam = camera.getCamera();
        // Original direction and up (untouched by angleFromZenith)
        dirBackup.set(cam.direction);
        upBackup.set(cam.direction);

        // Backup fov, direction and up.
        fovBackup = cam.fieldOfView;
        dirEffective.set(cam.direction);
        upEffective.set(cam.up);

        // dirUpCrs <- dir X up
        dirUpCrs.set(cam.direction).crs(cam.up).nor().scl(-1f);

        // Apply angle from zenith to dirBak and upBak.
        if (angleFromZenith != 0) {
            // Rotate direction and up around dirUpCrs.
            dirEffective.rotate(dirUpCrs, -angleFromZenith);
            upEffective.rotate(dirUpCrs, -angleFromZenith);
        }

        EventManager.publish(Event.FOV_CMD, this, 90f);

        // The sides of the cubemap must be square. We use the max of our resolution
        int wh = Settings.settings.program.modeCubemap.faceResolution;
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

        if (xPosFlag) {
            // RIGHT +X
            rc.cubemapSide = CubemapSide.SIDE_RIGHT;

            cam.up.set(upEffective);
            cam.direction.set(dirEffective).rotate(cam.up, -90);
            cam.update();

            renderFace(xPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (xNegFlag) {
            // LEFT -X
            rc.cubemapSide = CubemapSide.SIDE_LEFT;

            cam.up.set(upEffective);
            cam.direction.set(dirEffective).rotate(cam.up, 90);
            cam.update();

            renderFace(xNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (yPosFlag) {
            // UP +Y
            rc.cubemapSide = CubemapSide.SIDE_UP;

            cam.direction.set(dirEffective).rotate(dirUpCrs, 90);
            cam.up.set(upEffective).rotate(dirUpCrs, 90);
            cam.update();

            renderFace(yPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (yNegFlag) {
            // DOWN -Y
            rc.cubemapSide = CubemapSide.SIDE_DOWN;

            cam.direction.set(dirEffective).rotate(dirUpCrs, -90);
            cam.up.set(upEffective).rotate(dirUpCrs, -90);
            cam.update();

            renderFace(yNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (zPosFlag) {
            // FRONT +Z
            rc.cubemapSide = CubemapSide.SIDE_FRONT;

            cam.direction.set(dirEffective);
            cam.up.set(upEffective);
            cam.update();

            renderFace(zPosFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        if (zNegFlag) {
            // BACK -Z
            rc.cubemapSide = CubemapSide.SIDE_BACK;

            cam.up.set(upEffective);
            cam.direction.set(dirEffective).rotate(upEffective, -180);
            cam.update();

            renderFace(zNegFb, camera, sgr, ppb, rw, rh, wh, t);
        }

        // Restore camera parameters
        cam.direction.set(dirBackup);
        cam.up.set(upBackup);

        rc.cubemapSide = CubemapSide.SIDE_NONE;
    }

    protected void postRender(FrameBuffer fb) {
        if (fb != null)
            fb.end();

        // ensure default texture unit #0 is active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        // Restore fov
        EventManager.publish(Event.FOV_CMD, this, fovBackup);
    }

    protected void renderFace(FrameBuffer fb, ICamera camera, ISceneRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        renderRegularFace90(fb, camera, sgr, ppb, rw, rh, wh, t);
    }

    protected void renderRegularFace45(FrameBuffer fb, ICamera camera, ISceneRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {

        // -----------------
        // |       |       |
        // |  TL   |   TR  |
        // |       |       |
        // -----------------
        // |       |       |
        // |  BL   |   BR  |
        // |       |       |
        // -----------------

        wh = wh / 2;

        FrameBuffer tlFb = getFrameBuffer(wh, wh, 10);
        FrameBuffer blFb = getFrameBuffer(wh, wh, 11);
        FrameBuffer trFb = getFrameBuffer(wh, wh, 12);
        FrameBuffer brFb = getFrameBuffer(wh, wh, 13);

        float fov = 45f;
        float fov2 = fov / 2f;

        EventManager.publish(Event.FOV_CMD, this, fov);

        PerspectiveCamera cam = camera.getCamera();

        Vector3 upBak = aux2.set(cam.up);
        Vector3 dirBak = aux3.set(cam.direction);

        // TL
        cam.direction.rotate(upBak, fov2);
        Vector3 dirUpX = aux1.set(cam.direction).crs(upBak);
        cam.direction.rotate(dirUpX, fov2);
        cam.up.rotate(dirUpX, fov2);
        cam.update();

        renderFacePart(tlFb, camera, sgr, ppb, rw, rh, wh, t);

        // BL
        cam.direction.rotate(dirUpX, -fov);
        cam.up.rotate(dirUpX, -fov);
        cam.update();

        renderFacePart(blFb, camera, sgr, ppb, rw, rh, wh, t);

        // Reset
        cam.direction.set(dirBak);
        cam.up.set(upBak);

        // TR
        cam.direction.rotate(upBak, -fov2);
        dirUpX = aux1.set(cam.direction).crs(upBak);
        cam.direction.rotate(dirUpX, fov2);
        cam.up.rotate(dirUpX, fov2);
        cam.update();

        renderFacePart(trFb, camera, sgr, ppb, rw, rh, wh, t);

        // BR
        cam.direction.rotate(dirUpX, -fov);
        cam.up.rotate(dirUpX, -fov);
        cam.update();

        renderFacePart(brFb, camera, sgr, ppb, rw, rh, wh, t);

        // Render mosaic to fb
        mosaic.setViewportSize(wh, wh);
        mosaic.setTiles(tlFb, blFb, trFb, brFb);
        mosaic.render(null, fb, null);

        // Reset camera
        cam.direction.set(dirBak);
        cam.up.set(upBak);
        cam.update();
    }

    private void renderFacePart(FrameBuffer fb, ICamera camera, ISceneRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        sgr.getLightGlowPass().render(camera);

        boolean postProcess = postProcessCapture(ppb, fb, wh, wh, ppb::captureCubemap);
        try {
            sgr.renderScene(camera, t, rc);
        } finally {
            sendOrientationUpdate(camera.getCamera(), rw, rh);
            postProcessRender(ppb, fb, postProcess, camera, rw, rh);
        }
    }

    protected void renderRegularFace90(FrameBuffer fb, ICamera camera, ISceneRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        sgr.getLightGlowPass().render(camera);

        boolean postProcess = postProcessCapture(ppb, fb, wh, wh, ppb::captureCubemap);
        try {
            sgr.renderScene(camera, t, rc);
        } finally {
            sendOrientationUpdate(camera.getCamera(), rw, rh);
            postProcessRender(ppb, fb, postProcess, camera, rw, rh);
        }
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
