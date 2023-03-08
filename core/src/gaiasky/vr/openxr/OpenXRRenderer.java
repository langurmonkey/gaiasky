package gaiasky.vr.openxr;

import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

/**
 * To be implemented by all actors that render to OpenXR.
 */
public interface OpenXRRenderer {
    /**
     * Before-render operations. Executed once per cycle before rendering the views.
     */
    void renderBefore();

    /**
     * Executed for each eye every cycle.
     * @param layerView The layer view.
     * @param swapchainImage The swapchain image.
     * @param viewIndex The view index.
     */
    void renderView(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, int viewIndex);

    /**
     * After-render operations. Executed once per cycle after rendering the views.
     */
    void renderAfter();
}
