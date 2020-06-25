/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.IFocus;
import gaiasky.scenegraph.camera.CameraManager.CameraMode;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.GlobalConf.ProgramConf.StereoProfile;
import gaiasky.util.GlobalResources;
import gaiasky.util.gdx.contrib.postprocess.effects.Anaglyphic;
import gaiasky.util.gdx.contrib.postprocess.filters.Copy;
import gaiasky.util.math.Vector3d;
import org.lwjgl.openvr.VR;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Renders all the 3D/stereoscopic modes. Renders basically two scenes, one for
 * each eye, and then blends them together on screen with the necessary
 * processing depending on the 3D regime (anaglyphic, 3dtv, crosseye, vr).
 * 
 * @author tsagrista
 *
 */
public class SGRStereoscopic extends SGRAbstract implements ISGR, IObserver {

    private static final double EYE_ANGLE_DEG = 1.5;

    /** Viewport to use in steoeroscopic mode **/
    private Viewport stretchViewport;

    /** Frame buffers for 3D mode (screen, screenshot, frame output) **/
    Map<Integer, FrameBuffer> fb3D;

    private Anaglyphic anaglyphic;
    private Copy copy;

    private Vector3 aux1, aux2, aux3;
    private Vector3d aux1d, aux2d, aux3d, aux4d, aux5d;

    public SGRStereoscopic() {
        super();
        // INIT VIEWPORT
        stretchViewport = new StretchViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // INIT FRAME BUFFER FOR 3D MODE
        fb3D = new HashMap<>();
        fb3D.put(getKey(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight()), new FrameBuffer(Format.RGB888, Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight(), true));

        // Init anaglyphic effect
        anaglyphic = new Anaglyphic();

        // Copy
        copy = new Copy();

        // Aux vectors
        aux1 = new Vector3();
        aux2 = new Vector3();
        aux3 = new Vector3();
        aux1d = new Vector3d();
        aux2d = new Vector3d();
        aux3d = new Vector3d();
        aux4d = new Vector3d();
        aux5d = new Vector3d();

        EventManager.instance.subscribe(this, Events.FRAME_SIZE_UDPATE, Events.SCREENSHOT_SIZE_UDPATE);
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        boolean movecam = camera.getMode() == CameraMode.FREE_MODE || camera.getMode() == CameraMode.FOCUS_MODE || camera.getMode() == CameraMode.SPACECRAFT_MODE;


        PerspectiveCamera cam = camera.getCamera();
        // Vector of 1 meter length pointing to the side of the camera
        double separation = Constants.M_TO_U * GlobalConf.program.STEREOSCOPIC_EYE_SEPARATION_M;
        double separationCapped;
        double dirangleDeg = 0;

        IFocus currentFocus = null;
        if (camera.getMode() == CameraMode.FOCUS_MODE) {
            currentFocus = camera.getFocus();
        } else if (camera.getCurrent().getClosestBody() != null) {
            currentFocus = camera.getCurrent().getClosestBody();
        }
        if (currentFocus != null) {
            // If we have focus, we adapt the eye separation
            double distToFocus = currentFocus.getDistToCamera() - currentFocus.getRadius();
            // Lets calculate the separation
            if (camera.getMode() == CameraMode.SPACECRAFT_MODE) {
                // In spacecraft mode, the separation is extremely small, otherwise we see no spacecraft
                separation = (5000 * Constants.M_TO_U);
            } else {
                separation = Math.tan(Math.toRadians(EYE_ANGLE_DEG)) * distToFocus;
            }
            // Lets cap it
            separationCapped = Math.min(separation, 0.1 * Constants.AU_TO_U);
            dirangleDeg = EYE_ANGLE_DEG;
        } else {
            separationCapped = Math.min(separation, 0.1 * Constants.AU_TO_U);
        }

        // Aux5d contains the direction to the side of the camera, normalised
        aux5d.set(camera.getDirection()).crs(camera.getUp()).nor();

        Vector3d side = aux4d.set(aux5d).nor().scl(separation);
        Vector3d sideRemainder = aux2d.set(aux5d).scl(separation - separationCapped);
        Vector3d sideCapped = aux3d.set(aux5d).nor().scl(separationCapped);
        Vector3 backupPos = aux2.set(cam.position);
        Vector3 backupDir = aux3.set(cam.direction);
        Vector3d backupPosd = aux1d.set(camera.getPos());

        if (GlobalConf.program.STEREO_PROFILE.isAnaglyphic()) {
            // Update viewport
            extendViewport.setCamera(camera.getCamera());
            extendViewport.setWorldSize(rw, rh);
            extendViewport.setScreenBounds(0, 0, rw, rh);
            extendViewport.apply();

            /** LEFT EYE **/

            // Camera to the left
            if (movecam) {
                moveCamera(camera, sideRemainder, side, sideCapped, dirangleDeg, false);
            }
            camera.setCameraStereoLeft(cam);

            sgr.renderGlowPass(camera, null, VR.EVREye_Eye_Left);

            FrameBuffer fb1 = getFrameBuffer(rw, rh, 1);
            boolean postproc = postprocessCapture(ppb, fb1, tw, th);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(cam, rw, rh);
            postprocessRender(ppb, fb1, postproc, camera, rw, rh);
            Texture texLeft = fb1.getColorBufferTexture();

            /** RIGHT EYE **/

            // Camera to the right
            if (movecam) {
                restoreCameras(camera, cam, backupPosd, backupPos, backupDir);
                moveCamera(camera, sideRemainder, side, sideCapped, dirangleDeg, true);
            }
            camera.setCameraStereoRight(cam);

            sgr.renderGlowPass(camera, null, VR.EVREye_Eye_Right);

            FrameBuffer fb2 = getFrameBuffer(rw, rh, 2);
            postproc = postprocessCapture(ppb, fb2, tw, th);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(cam, rw, rh);
            postprocessRender(ppb, fb2, postproc, camera, rw, rh);
            Texture texRight = fb2.getColorBufferTexture();

            // We have left and right images to texLeft and texRight

            anaglyphic.setTextureLeft(texLeft);
            anaglyphic.setTextureRight(texRight);

            // Render 
            anaglyphic.render(null, resultBuffer, null);
            resultBuffer.end();

            // ensure default texture unit #0 is active
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        } else {

            int srw, srh, boundsw, boundsh, start2w, start2h;

            boolean stretch = GlobalConf.program.STEREO_PROFILE == StereoProfile.HD_3DTV_HORIZONTAL || GlobalConf.program.STEREO_PROFILE == StereoProfile.HD_3DTV_VERTICAL;
            boolean changesides = GlobalConf.program.STEREO_PROFILE == StereoProfile.CROSSEYE;

            if (GlobalConf.program.STEREO_PROFILE.isHorizontal()) {
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

            // Side by side rendering
            Viewport viewport = stretch ? stretchViewport : extendViewport;

            viewport.setCamera(camera.getCamera());
            viewport.setWorldSize(srw, srh);

            /** LEFT EYE **/

            viewport.setScreenBounds(0, 0, boundsw, boundsh);
            viewport.apply();

            // Camera to left
            if (movecam) {
                moveCamera(camera, sideRemainder, side, sideCapped, dirangleDeg, changesides);
            }
            camera.setCameraStereoLeft(cam);

            sgr.renderGlowPass(camera, null, VR.EVREye_Eye_Left);

            FrameBuffer fb3d = getFrameBuffer(boundsw, boundsh, 3);
            boolean postproc = postprocessCapture(ppb, fb3d, boundsw, boundsh);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(cam, rw, rh);
            Texture tex;
            postprocessRender(ppb, fb3d, postproc, camera, boundsw, boundsh);
            tex = fb3d.getColorBufferTexture();

            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            resultBuffer.begin();
            GlobalResources.spriteBatch.begin();
            GlobalResources.spriteBatch.setColor(1f, 1f, 1f, 1f);
            GlobalResources.spriteBatch.draw(tex, 0, 0, 0, 0, boundsw, boundsh, 1, 1, 0, 0, 0, boundsw, boundsh, false, true);
            GlobalResources.spriteBatch.end();
            resultBuffer.end();

            /** RIGHT EYE **/

            viewport.setScreenBounds(start2w, start2h, boundsw, boundsh);
            viewport.apply();

            // Camera to right
            if (movecam) {
                restoreCameras(camera, cam, backupPosd, backupPos, backupDir);
                moveCamera(camera, sideRemainder, side, sideCapped, dirangleDeg, !changesides);
            }
            camera.setCameraStereoRight(cam);

            sgr.renderGlowPass(camera, null, VR.EVREye_Eye_Right);

            postproc = postprocessCapture(ppb, fb3d, boundsw, boundsh);
            sgr.renderScene(camera, t, rc);

            sendOrientationUpdate(cam, rw, rh);
            postprocessRender(ppb, fb3d, postproc, camera, boundsw, boundsh);
            tex = fb3d.getColorBufferTexture();

            resultBuffer = fb == null ? getFrameBuffer(rw, rh, 0) : fb;
            resultBuffer.begin();
            GlobalResources.spriteBatch.begin();
            GlobalResources.spriteBatch.setColor(1f, 1f, 1f, 1f);
            GlobalResources.spriteBatch.draw(tex, start2w, start2h, 0, 0, boundsw, boundsh, 1, 1, 0, 0, 0, boundsw, boundsh, false, true);
            GlobalResources.spriteBatch.end();
            resultBuffer.end();

            /* Restore viewport */
            viewport.setScreenBounds(0, 0, rw, rh);

        }

        /** RESTORE **/
        restoreCameras(camera, cam, backupPosd, backupPos, backupDir);

        // To screen
        if (fb == null)
            copy.setInput(resultBuffer).setOutput(null).render();

    }

    private void restoreCameras(ICamera camera, PerspectiveCamera cam, Vector3d backupPosd, Vector3 backupPos, Vector3 backupDir) {
        camera.setPos(backupPosd);
        cam.position.set(backupPos);
        cam.direction.set(backupDir);
    }

    private void moveCamera(ICamera camera, Vector3d sideRemainder, Vector3d side, Vector3d sideCapped, double angle, boolean switchSides) {
        PerspectiveCamera cam = camera.getCamera();
        Vector3 sidef = sideCapped.put(aux1);

        if (switchSides) {
            cam.position.add(sidef);
            cam.direction.rotate(cam.up, (float) -angle);

            // Uncomment to enable 3D in GPU points
            camera.getPos().add(sideRemainder);
        } else {
            cam.position.sub(sidef);
            cam.direction.rotate(cam.up, (float) angle);

            // Uncomment to enable 3D in GPU points
            camera.getPos().sub(sideRemainder);
        }
        cam.update();
    }

    private int getKey(int w, int h) {
        return getKey(w, h, 0);
    }

    private int getKey(int w, int h, int extra) {
        return w * 100 + h * 10 + extra;
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

    public void resize(final int w, final int h) {
        if (GlobalConf.program.STEREOSCOPIC_MODE) {
            extendViewport.update(w, h);
            stretchViewport.update(w, h);

            int keyHalf = getKey(w / 2, h);
            int keyFull = getKey(w, h);

            if (!fb3D.containsKey(keyHalf)) {
                fb3D.put(keyHalf, new FrameBuffer(Format.RGB888, w / 2, h, true));
            }

            if (!fb3D.containsKey(keyFull)) {
                fb3D.put(keyFull, new FrameBuffer(Format.RGB888, w, h, true));
            }

            Iterator<Map.Entry<Integer, FrameBuffer>> iter = fb3D.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, FrameBuffer> entry = iter.next();
                if (entry.getKey() != keyHalf && entry.getKey() != keyFull) {
                    entry.getValue().dispose();
                    iter.remove();
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
    public void notify(final Events event, final Object... data) {
        switch (event) {
        case SCREENSHOT_SIZE_UDPATE:
        case FRAME_SIZE_UDPATE:
            GaiaSky.postRunnable(() -> clearFrameBufferMap());
            break;
        default:
            break;
        }

    }

}
