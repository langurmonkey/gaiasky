package gaiasky.vr.openxr;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

/**
 * To be implemented by all actors that render to OpenXR.
 */
public interface OpenXRRenderer {

    /**
     * Executed for each eye every cycle.
     *
     * @param layerView      The layer view.
     * @param swapchainImage The swapchain image.
     * @param frameBuffer    The frame buffer to draw to.
     * @param viewIndex      The view index.
     */
    void renderOpenXRView(XrCompositionLayerProjectionView layerView, XrSwapchainImageOpenGLKHR swapchainImage, FrameBuffer frameBuffer, int viewIndex);

}
