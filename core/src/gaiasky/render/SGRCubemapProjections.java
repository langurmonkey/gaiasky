/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.event.IObserver;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.GlobalConf;
import gaiasky.util.gdx.contrib.postprocess.effects.CubemapProjections;

import java.util.Set;

/**
 * Renders the cube map projection mode. Basically, it renders the six sides of
 * the cube map (front, back, up, down, right, left) with a 90 degree fov each
 * and applies a cube map projection (spherical, cylindrical, hammer, fisheye).
 *
 * @author tsagrista
 */
public class SGRCubemapProjections extends SGRCubemap implements ISGR, IObserver{


    private CubemapProjections cubemapEffect;

    public SGRCubemapProjections() {
        super();

        cubemapEffect = new CubemapProjections(0, 0);
        cubemapEffect.setProjection(GlobalConf.program.CUBEMAP_PROJECTION);

        EventManager.instance.subscribe(this, Events.CUBEMAP_RESOLUTION_CMD, Events.CUBEMAP_PROJECTION_CMD);
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb) {
        // This renders the cubemap to [x|y|z][pos|neg]fb
        super.renderCubemapSides(sgr, camera, t, rw, rh, ppb);

        // Panorama effect
        FrameBuffer mainfb = getFrameBuffer(rw, rh);
        cubemapEffect.setViewportSize(tw, th);
        cubemapEffect.setSides(xposfb, xnegfb, yposfb, ynegfb, zposfb, znegfb);
        cubemapEffect.render(mainfb, fb, null);

        // Post render actions
        super.postRender(fb);
    }


    @Override
    public void resize(int w, int h) {

    }

    @Override
    public void dispose() {
        Set<Integer> keySet = fbcm.keySet();
        for (Integer key : keySet) {
            fbcm.get(key).dispose();
        }
    }

    @Override
    public void notify(Events event, Object... data) {
        switch (event) {
        case CUBEMAP_RESOLUTION_CMD:
            int res = (Integer) data[0];
            GaiaSky.postRunnable(() -> {
                // Create new ones
                if (!fbcm.containsKey(getKey(res, res, 0))) {
                    // Clear
                    dispose();
                    fbcm.clear();
                } else {
                    // All good
                }
            });
            break;
        case CUBEMAP_PROJECTION_CMD:
            CubemapProjections.CubemapProjection p = (CubemapProjections.CubemapProjection) data[0];
            GaiaSky.postRunnable(() -> {
                cubemapEffect.setProjection(p);
            });
            break;
        default:
            break;
        }

    }

}
