package gaia.cu9.ari.gaiaorbit.render;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.bitfire.postprocessing.effects.CubemapProjections;
import com.bitfire.postprocessing.effects.CubemapProjections.CubemapProjection;

import gaia.cu9.ari.gaiaorbit.event.EventManager;
import gaia.cu9.ari.gaiaorbit.event.Events;
import gaia.cu9.ari.gaiaorbit.event.IObserver;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.render.RenderingContext.CubemapSide;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;

/**
 * Renders the cube map 360 degree mode. Basically, it renders the six sides of
 * the cube map (front, back, up, down, right, left) with a 90 degree fov each
 * and applies the cube map to equirectangular transformation.
 * 
 * @author tsagrista
 *
 */
public class SGRCubemap extends SGRAbstract implements ISGR, IObserver {

    Vector3 aux1, aux2, aux3, dirbak, upbak;

    StretchViewport stretchViewport;

    CubemapProjections cubemapEffect;

    /** Frame buffers for each side of the cubemap **/
    Map<Integer, FrameBuffer> fbcm;

    public SGRCubemap() {
        super();
        aux1 = new Vector3();
        aux3 = new Vector3();
        aux2 = new Vector3();
        dirbak = new Vector3();
        upbak = new Vector3();
        stretchViewport = new StretchViewport(Gdx.graphics.getHeight(), Gdx.graphics.getHeight());

        fbcm = new HashMap<Integer, FrameBuffer>();

        cubemapEffect = new CubemapProjections();
        cubemapEffect.setProjection(GlobalConf.program.CUBEMAP_PROJECTION);

        EventManager.instance.subscribe(this, Events.CUBEMAP_RESOLUTION_CMD, Events.CUBEMAP_PROJECTION_CMD);
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {

        PerspectiveCamera cam = camera.getCamera();

        float fovbak = cam.fieldOfView;
        dirbak.set(cam.direction);
        upbak.set(cam.up);

        EventManager.instance.post(Events.FOV_CHANGED_CMD, 90f);

        FrameBuffer mainfb = getFrameBuffer(rw, rh);

        // The sides of the cubemap must be square. We use the max of our resolution
        int wh = GlobalConf.scene.CUBEMAP_FACE_RESOLUTION;
        FrameBuffer zposfb = getFrameBuffer(wh, wh, 0);
        FrameBuffer znegfb = getFrameBuffer(wh, wh, 1);
        FrameBuffer xposfb = getFrameBuffer(wh, wh, 2);
        FrameBuffer xnegfb = getFrameBuffer(wh, wh, 3);
        FrameBuffer yposfb = getFrameBuffer(wh, wh, 4);
        FrameBuffer ynegfb = getFrameBuffer(wh, wh, 5);

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

        // Effect
        cubemapEffect.setSides(xposfb, xnegfb, yposfb, ynegfb, zposfb, znegfb);
        cubemapEffect.render(mainfb, fb);

        if (fb != null)
            fb.end();

        // ensure default texture unit #0 is active
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

        EventManager.instance.post(Events.FOV_CHANGED_CMD, fovbak);

    }

    private void renderFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        renderRegularFace(fb, camera, sgr, ppb, rw, rh, wh, t);
    }

    private void renderRegularFace(FrameBuffer fb, ICamera camera, SceneGraphRenderer sgr, PostProcessBean ppb, int rw, int rh, int wh, double t) {
        sgr.renderGlowPass(camera);

        boolean postproc = postprocessCapture(ppb, fb, wh, wh);
        sgr.renderScene(camera, t, rc);
        postprocessRender(ppb, fb, postproc, camera, rw, rh);
    }

    private int getKey(int w, int h, int extra) {
        return w * 100 + h * 10 + extra;
    }

    private FrameBuffer getFrameBuffer(int w, int h) {
        return getFrameBuffer(w, h, 0);
    }

    private FrameBuffer getFrameBuffer(int w, int h, int extra) {
        int key = getKey(w, h, extra);
        if (!fbcm.containsKey(key)) {
            fbcm.put(key, new FrameBuffer(Format.RGB888, w, h, true));
        }
        return fbcm.get(key);
    }

    @Override
    public void resize(int w, int h) {

    }

    @Override
    public void dispose() {
        Set<Integer> keySet = fbcm.keySet();
        for (Integer key : keySet) {
            FrameBuffer fb = fbcm.get(key);
            fb.dispose();
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CUBEMAP_RESOLUTION_CMD:
            int res = (Integer) data[0];
            if (!fbcm.containsKey(getKey(res, res, 0))) {
                // Clear
                Set<Integer> keyset = fbcm.keySet();
                for (Integer key : keyset) {
                    fbcm.get(key).dispose();
                }
                fbcm.clear();
            } else {
                // All good
            }
            break;
        case CUBEMAP_PROJECTION_CMD:
            CubemapProjection p = (CubemapProjection) data[0];
            Gdx.app.postRunnable(() -> {
                cubemapEffect.setProjection(p);
            });
            break;
        default:
            break;
        }

    }

}
