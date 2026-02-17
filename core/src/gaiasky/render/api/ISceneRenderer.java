/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.api;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderingContext;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.system.render.pass.LightGlowRenderPass;

import java.util.List;

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
     * Checks if a given component type is on.
     *
     * @param comp The component.
     *
     * @return Whether the component is on.
     */
    boolean isOn(ComponentType comp);

    /**
     * Checks if the component types are all on.
     *
     * @param comp The components.
     *
     * @return Whether the components are all on.
     */
    boolean allOn(ComponentTypes comp);

    /**
     * Gets the current render process.
     *
     * @return The render mode.
     */
    IRenderMode getRenderProcess();

    /**
     * Returns he render lists of this renderer.
     *
     * @param full Whether to return the render lists for the full- or the half-resolution buffer.
     **/
    List<List<IRenderable>> getRenderLists(boolean full);
}
