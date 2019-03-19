package gaia.cu9.ari.gaiaorbit.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaia.cu9.ari.gaiaorbit.render.IPostProcessor.PostProcessBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;

/**
 * Interface for component renderers.
 *
 * @author Toni Sagrista
 */
public interface IProcessRenderer extends Disposable {

    /**
     * Renders the scene.
     *
     * @param camera The camera to use.
     * @param dt     The delta time computed in the update method in seconds.
     * @param rw     The render width.
     * @param rh     The render height.
     * @param fb     The frame buffer to write to, if any.
     * @param ppb    The post process bean.
     */
    void render(ICamera camera, double dt, int rw, int rh, FrameBuffer fb, PostProcessBean ppb);

    /**
     * Initializes the renderer, sending all the necessary assets to the manager
     * for loading
     *
     * @param manager The asset manager
     */
    void initialize(AssetManager manager);

    /**
     * Actually initializes all the clockwork of this renderer using the assets
     * in the given manager
     *
     * @param manager The asset manager
     */
    void doneLoading(AssetManager manager);

}
