package gaiasky.render.api;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.RenderingContext;
import gaiasky.render.process.IRenderProcess;
import gaiasky.scenegraph.camera.ICamera;

/**
 * Defines the interface for scene renderers.
 */
public interface ISceneRenderer {

    /**
     * Renders the scene.
     *
     * @param camera        The camera to use.
     * @param t             The time in seconds since the start.
     * @param renderContext The render context.
     */
    void renderScene(ICamera camera, double t, RenderingContext renderContext);

    /**
     * Renders the glow pass for the light glow.
     *
     * @param camera      The camera.
     * @param frameBuffer The frame buffer.
     */
    void renderGlowPass(ICamera camera, FrameBuffer frameBuffer);

    /**
     * Initializes the renderer, sending all the necessary assets to the manager
     * for loading.
     *
     * @param manager The asset manager.
     */
    void initialize(AssetManager manager);

    /**
     * Actually initializes all the clockwork of this renderer using the assets
     * in the given manager.
     *
     * @param manager The asset manager.
     */
    void doneLoading(AssetManager manager);

    /**
     * Gets the current render process.
     * @return
     */
    IRenderProcess getRenderProcess();

    /**
     * Returns the post-processing glow frame buffer.
     * @return The glow frame buffer.
     */
    FrameBuffer getGlowFrameBuffer();
}
