/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.vr.openxr;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import org.lwjgl.openxr.XrCompositionLayerProjectionView;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

/**
 * This interface is to be implemented by all agents that want to render to OpenXR.
 */
public interface XrRenderer {

    /**
     * Executed for each eye every cycle.
     *
     * @param layerView      The layer view.
     * @param swapChainImage The swap-chain image.
     * @param frameBuffer    The frame buffer to draw to.
     * @param viewIndex      The view index.
     */
    void renderOpenXRView(XrCompositionLayerProjectionView layerView,
                          XrSwapchainImageOpenGLKHR swapChainImage,
                          FrameBuffer frameBuffer,
                          int viewIndex);

    /**
     * Render to the desktop.
     * @param textureHandle The texture handle
     */
    void renderMirrorToDesktop(int textureHandle) ;
}
