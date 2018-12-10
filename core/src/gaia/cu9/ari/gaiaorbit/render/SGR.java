package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.GlobalResources;

/**
 * Normal SGR, takes care of the regular to-screen rednering with no strange
 * modes (stereoscopic, planetarium, cubemap) active.
 * 
 * @author tsagrista
 *
 */
public class SGR extends SGRAbstract implements ISGR {

    SpriteBatch sb;

    public SGR() {
        super();
        sb = new SpriteBatch(GlobalConf.scene.SHADOW_MAPPING_N_SHADOWS, GlobalResources.spriteShader);
    }

    @Override
    public void render(SceneGraphRenderer sgr, ICamera camera, double t, int rw, int rh, FrameBuffer fb, PostProcessBean ppb) {
        boolean postproc = postprocessCapture(ppb, fb, rw, rh);

        // Viewport
        extendViewport.setCamera(camera.getCamera());
        extendViewport.setWorldSize(rw, rh);
        extendViewport.setScreenSize(rw, rh);
        extendViewport.apply();

        // Render
        sgr.renderScene(camera, t, rc);

        // Uncomment this to show the shadow map
        //        if (GlobalConf.scene.SHADOW_MAPPING) {
        //            // Render shadow map
        //            int s = GlobalConf.scene.SHADOW_MAPPING_RESOLUTION;
        //            sb.begin();
        //            for (int i = 0; i < sgr.shadowMapFb.length; i++) {
        //                sb.draw(sgr.shadowMapFb[i].getColorBufferTexture(), 0, 0, s, s);
        //            }
        //            sb.end();
        //        }

        postprocessRender(ppb, fb, postproc, camera, rw, rh);

    }

    @Override
    public void resize(int w, int h) {
    }

    @Override
    public void dispose() {
    }

}
