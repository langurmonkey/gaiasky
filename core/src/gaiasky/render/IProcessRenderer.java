/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.IPostProcessor.PostProcessBean;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Interface for component renderers.
 *
 * @author Toni Sagrista
 */
public interface IProcessRenderer extends Disposable {

    /**
     * Renders the scene.
     *
     * @param camera The camera to use
     * @param dt     The delta time computed in the update method in seconds
     * @param rw     The width of the render buffer
     * @param rh     The height of the render buffer
     * @param tw     The final target width
     * @param th     The final target height
     * @param fb     The frame buffer to write to, if any
     * @param ppb    The post process bean
     */
    void render(ICamera camera, double dt, int rw, int rh, int tw, int th, FrameBuffer fb, PostProcessBean ppb);

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
