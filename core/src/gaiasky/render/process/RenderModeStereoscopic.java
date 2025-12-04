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
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.render.api.IPostProcessor.PostProcessBean;
import gaiasky.render.api.IRenderMode;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.render.postprocess.effects.AnaglyphEffect;
import gaiasky.render.postprocess.filters.CopyFilter;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.CameraManager.CameraMode;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.Settings;
import gaiasky.util.Settings.StereoProfile;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Renders all the 3D/stereoscopic modes. Renders basically two scenes, one for
 * each eye, and then blends them together on screen with the necessary
 * processing depending on the 3D regime (anaglyph 3D, 3DTV, cross-eye, VR)
 */
public class RenderModeStereoscopic extends RenderModeAbstract implements IRenderMode, IObserver {

    /** Current separation, for smoothing. **/
    private double sep = 0.0;

    /**
     * Viewport to use in stereoscopic mode
     **/
    private final Viewport stretchViewport;
    private final SpriteBatch sb;
    private final AnaglyphEffect anaglyphEffect;
    private final CopyFilter copyFilter;
    private final Vector3 aux1;
    private final Vector3 aux2;
    private final Vector3 aux3;
    private final Vector3D aux1d;
    private final Vector3D aux2d;
    private final Vector3D aux3d;
    private final Vector3D aux4d;
    private final Vector3D aux5d;
    /**
     * Frame buffers for 3D mode (screen, screenshot, frame output)
     **/
    Map<Integer, FrameBuffer> fb3D;

    public RenderModeStereoscopic(final SpriteBatch spriteBatch) {
        super();
        // INIT VIEWPORT
        stretchViewport = new StretchViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        this.sb = spriteBatch;

        // INIT FRAME BUFFER FOR 3D MODE
        fb3D = new HashMap<>();
        fb3D.put(getKey(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight()),
                 new FrameBuffer(Format.RGB888, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight(), true));

        // Init anaglyph 3D effect
        anaglyphEffect = new AnaglyphEffect();
        updateAnaglyphMode();

        // Copy
        copyFilter = new CopyFilter();

        // Aux vectors
        aux1 = new Vector3();
        aux2 = new Vector3();
        aux3 = new Vector3();
        aux1d = new Vector3D();
        aux2d = new Vector3D();
        aux3d = new Vector3D();
        aux4d = new Vector3D();
        aux5d = new Vector3D();

        EventManager.instance.subscribe(this, Event.FRAME_SIZE_UPDATE, Event.SCREENSHOT_SIZE_UPDATE, Event.SHADER_RELOAD_CMD);
    }

    public void updateAnaglyphMode() {
        if (anaglyphEffect != null) {
            anaglyphEffect.setAnaglyphMode(Settings.settings.program.modeStereo.profile.getAnaglyphModeInteger());
        }
    }

    /**
     * Gets the distance to the current focus or the closest body object.
     *
     * @param camera The camera.
     *
     * @return The distance, in internal units.
     */
    private double getObjectDistance(ICamera camera) {
        IFocus body = camera.getMode().isFocus() ? camera.getFocus() : camera.getClosestBody();
        return body.getDistToCamera() - body.getRadius();
    }

    @Override
    public void render(ISceneRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        boolean moveCam = camera.getMode() == CameraMode.FREE_MODE || camera.getMode() == CameraMode.FOCUS_MODE || camera.getMode() == CameraMode.SPACECRAFT_MODE;

        PerspectiveCamera perspectiveCam = camera.getCamera();

        // Compute distance to focus or closest body object.
        double closestDist = getObjectDistance(camera);
        double k = Settings.settings.program.modeStereo.k;
        double ipd = Settings.settings.program.modeStereo.ipd;
        double screenDist = Settings.settings.program.modeStereo.screenDistance;
        double maxSep = 1.0e8 * Constants.KM_TO_U;
        // The magic formula.
        double eyeSep = k * (closestDist * ipd / screenDist) * camera.getFovFactor();
        sep = MathUtilsDouble.lowPass(eyeSep, sep, 4.0);
        double eyeSepCapped = FastMath.min(sep, maxSep);

        // Aux5d contains the direction to the side of the camera, normalized.
        aux5d.set(camera.getDirection()).crs(camera.getUp()).nor();

        Vector3D side = aux4d.set(aux5d).nor().scl(sep);
        Vector3D sideRemainder = aux2d.set(aux5d).scl(sep - eyeSepCapped);
        Vector3D sideCapped = aux3d.set(aux5d).nor().scl(eyeSepCapped);
        Vector3 backupPos = aux2.set(perspectiveCam.position);
        Vector3 backupDir = aux3.set(perspectiveCam.direction);
        Vector3D backupPosD = aux1d.set(camera.getPos());

        if (Settings.settings.program.modeStereo.profile.isAnaglyph()) {
            // Update viewport.
            extendViewport.setCamera(camera.getCamera());
            extendViewport.setWorldSize(rw, rh);
            extendViewport.setScreenBounds(0, 0, rw, rh);
            extendViewport.apply();

            // LEFT EYE.

            // Camera to the left.
            if (moveCam) {
                moveCamera(camera, sideRemainder, side, sideCapped, 0.0, false);
            }
            camera.setCameraStereoLeft(perspectiveCam);

            sgr.getLightGlowPass().render(camera);

            FrameBuffer fb1 = getFrameBuffer(rw, rh, 1);
            boolean postProcess = postProcessCapture(ppb, fb1, tw, th, ppb::capture);
            try {
                sgr.renderScene(camera, t, rc);
            } finally {
                sendOrientationUpdate(perspectiveCam, rw, rh);
                postProcessRender(ppb, fb1, postProcess, camera, rw, rh);
            }
            Texture texLeft = fb1.getColorBufferTexture();

            // RIGHT EYE.

            // Camera to the right.
            if (moveCam) {
                restoreCameras(camera, perspectiveCam, backupPosD, backupPos, backupDir);
                moveCamera(camera, sideRemainder, side, sideCapped, 0.0, true);
            }
            camera.setCameraStereoRight(perspectiveCam);

            sgr.getLightGlowPass().render(camera);

            FrameBuffer fb2 = getFrameBuffer(rw, rh, 2);
            postProcess = postProcessCapture(ppb, fb2, tw, th, ppb::capture);
            try {
                sgr.renderScene(camera, t, rc);
            } finally {
                sendOrientationUpdate(perspectiveCam, rw, rh);
                postProcessRender(ppb, fb2, postProcess, camera, rw, rh);
            }
            Texture texRight = fb2.getColorBufferTexture();

            // We have left and right images to texLeft and texRight.
            updateAnaglyphMode();
            anaglyphEffect.setTextureLeft(texLeft);
            anaglyphEffect.setTextureRight(texRight);

            // Render 
            anaglyphEffect.render(null, resultBuffer, null, null);
            resultBuffer.end();

            // ensure default texture unit #0 is active.
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        } else {

            double srw, srh, boundsw, boundsh, start2w, start2h;

            float unitsPerPixel = GaiaSky.instance.getUnitsPerPixel();
            boolean stretch = Settings.settings.program.modeStereo.profile == StereoProfile.HORIZONTAL_3DTV || Settings.settings.program.modeStereo.profile == StereoProfile.VERTICAL_3DTV;
            boolean changeSides = Settings.settings.program.modeStereo.profile == StereoProfile.CROSS_EYE;

            if (Settings.settings.program.modeStereo.profile.isHorizontal()) {
                if (stretch) {
                    srw = rw;
                } else {
                    srw = rw / 2;
                }
                srh = rh;
                boundsw = rw / 2;
                boundsh = rh;
                start2w = boundsw;
                start2h = 0;
            } else {
                if (stretch) {
                    srh = rh;
                } else {
                    srh = rh / 2;
                }
                srw = rw;
                boundsw = rw;
                boundsh = rh / 2;
                start2w = 0;
                start2h = boundsh;
            }
            boundsw *= unitsPerPixel;
            boundsh *= unitsPerPixel;
            start2w *= unitsPerPixel;
            start2h *= unitsPerPixel;

            boundsw /= Settings.settings.graphics.backBufferScale;
            boundsh /= Settings.settings.graphics.backBufferScale;
            start2w /= Settings.settings.graphics.backBufferScale;
            start2h /= Settings.settings.graphics.backBufferScale;

            // Side by side rendering
            Viewport viewport = stretch ? stretchViewport : extendViewport;

            viewport.setCamera(camera.getCamera());
            viewport.setWorldSize((float) srw, (float) srh);

            // LEFT EYE

            viewport.setScreenBounds(0, 0, (int) boundsw, (int) boundsh);
            viewport.apply();

            // Camera to left
            if (moveCam) {
                moveCamera(camera, sideRemainder, side, sideCapped, 0.0, changeSides);
            }
            camera.setCameraStereoLeft(perspectiveCam);

            sgr.getLightGlowPass().render(camera);

            FrameBuffer fb3d = getFrameBuffer((int) boundsw, (int) boundsh, 3);
            boolean postProcess = postProcessCapture(ppb, fb3d, (int) boundsw, (int) boundsh, ppb::capture);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(perspectiveCam, rw, rh);
            Texture tex;
            postProcessRender(ppb, fb3d, postProcess, camera, (int) boundsw, (int) boundsh);
            tex = fb3d.getColorBufferTexture();

            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            resultBuffer.begin();
            sb.begin();
            sb.setColor(1f, 1f, 1f, 1f);
            sb.draw(tex, 0f, 0f, 0f, 0f, (float) boundsw, (float) boundsh, 1f, 1f, 0f, 0, 0, (int) boundsw, (int) boundsh, false, true);
            sb.end();
            resultBuffer.end();

            // RIGHT EYE

            viewport.setScreenBounds((int) start2w, (int) start2h, (int) boundsw, (int) boundsh);
            viewport.apply();

            // Camera to right
            if (moveCam) {
                restoreCameras(camera, perspectiveCam, backupPosD, backupPos, backupDir);
                moveCamera(camera, sideRemainder, side, sideCapped, 0.0, !changeSides);
            }
            camera.setCameraStereoRight(perspectiveCam);

            sgr.getLightGlowPass().render(camera);

            postProcess = postProcessCapture(ppb, fb3d, (int) boundsw, (int) boundsh, ppb::capture);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(perspectiveCam, rw, rh);
            postProcessRender(ppb, fb3d, postProcess, camera, (int) boundsw, (int) boundsh);
            tex = fb3d.getColorBufferTexture();

            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            resultBuffer.begin();
            sb.begin();
            sb.setColor(1f, 1f, 1f, 1f);
            sb.draw(tex,
                    (float) start2w,
                    (float) start2h,
                    0f,
                    0f,
                    (float) boundsw,
                    (float) boundsh,
                    1f,
                    1f,
                    0f,
                    0,
                    0,
                    (int) boundsw,
                    (int) boundsh,
                    false,
                    true);
            sb.end();
            resultBuffer.end();

            /* Restore viewport */
            viewport.setScreenBounds(0, 0, rw, rh);

        }

        // RESTORE
        restoreCameras(camera, perspectiveCam, backupPosD, backupPos, backupDir);

        // To screen
        if (fb == null)
            copyFilter.setInput(resultBuffer).setOutput(null).render();

    }

    private void restoreCameras(ICamera camera, PerspectiveCamera cam, Vector3D backupPosd, Vector3 backupPos, Vector3 backupDir) {
        camera.setPos(backupPosd);
        cam.position.set(backupPos);
        cam.direction.set(backupDir);
    }

    /**
     * Move the camera to the side for stereo projection.
     *
     * @param camera        Camera.
     * @param sideRemainder Side remainder vector.
     * @param side          The side vector.
     * @param sideCapped    The side, capped.
     * @param toeInAngle    Angle to use for rotating the direction vectors inwards towards the convergence point. Set to 0 for parallel projection.
     * @param switchSides   Whether to switch sides.
     */
    private void moveCamera(ICamera camera, Vector3D sideRemainder, Vector3D side, Vector3D sideCapped, double toeInAngle, boolean switchSides) {
        PerspectiveCamera cam = camera.getCamera();
        Vector3 sideFloat = sideCapped.put(aux1);

        if (switchSides) {
            cam.position.add(sideFloat);
            if (toeInAngle != 0)
                cam.direction.rotate(cam.up, (float) -toeInAngle);

            // Uncomment to enable 3D in GPU points
            camera.getPos().add(sideRemainder);
        } else {
            cam.position.sub(sideFloat);
            if (toeInAngle != 0)
                cam.direction.rotate(cam.up, (float) toeInAngle);

            // Uncomment to enable 3D in GPU points
            camera.getPos().sub(sideRemainder);
        }
        cam.update();
    }

    private int getKey(int w, int h) {
        return getKey(w, h, 0);
    }

    private int getKey(int w, int h, int extra) {
        return 31 * (31 * h + w) + extra;
    }

    private FrameBuffer getFrameBuffer(int w, int h, int extra) {
        int key = getKey(w, h, extra);
        if (!fb3D.containsKey(key)) {
            fb3D.put(key, new FrameBuffer(Format.RGB888, w, h, true));
        }
        return fb3D.get(key);
    }

    private FrameBuffer getFrameBuffer(int w, int h) {
        return getFrameBuffer(w, h, 0);
    }

    public void resize(int rw, int rh, int tw, int th) {
        if (Settings.settings.program.modeStereo.active) {
            extendViewport.update(tw, th);
            stretchViewport.update(tw, th);

            int keyHalf = getKey(tw / 2, th);
            int keyFull = getKey(tw, th);

            if (!fb3D.containsKey(keyHalf)) {
                fb3D.put(keyHalf, new FrameBuffer(Format.RGB888, tw / 2, th, true));
            }

            if (!fb3D.containsKey(keyFull)) {
                fb3D.put(keyFull, new FrameBuffer(Format.RGB888, tw, th, true));
            }

            Iterator<Map.Entry<Integer, FrameBuffer>> iterator = fb3D.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, FrameBuffer> entry = iterator.next();
                if (entry.getKey() != keyHalf && entry.getKey() != keyFull) {
                    entry.getValue().dispose();
                    iterator.remove();
                }
            }
        }

    }

    private void clearFrameBufferMap() {
        Set<Integer> keySet = fb3D.keySet();
        for (Integer key : keySet) {
            FrameBuffer fb = fb3D.get(key);
            fb.dispose();
        }
        fb3D.clear();
    }

    public void dispose() {
        clearFrameBufferMap();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case SCREENSHOT_SIZE_UPDATE, FRAME_SIZE_UPDATE -> GaiaSky.postRunnable(this::clearFrameBufferMap);
            case SHADER_RELOAD_CMD -> GaiaSky.postRunnable(anaglyphEffect::updateShaders);
            default -> {
            }
        }

    }

}
