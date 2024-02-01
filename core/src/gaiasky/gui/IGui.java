/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;

public interface IGui extends Disposable {

    /**
     * Initializes the GUI, adding all the resources to the asset manager queue
     * for loading
     *
     * @param assetManager The asset manager to load the resources with
     * @param batch        The sprite batch to use for this GUI's stage
     */
    void initialize(AssetManager assetManager, SpriteBatch batch);

    /**
     * Hook that runs after the assets have been loaded. Completes the
     * initialization process
     *
     * @param assetManager The asset manager
     */
    void doneLoading(AssetManager assetManager);

    /**
     * Updates the GUI
     *
     * @param dt Time in seconds since the last frame
     */
    void update(double dt);

    /**
     * Renders this GUI
     *
     * @param rw The render width
     * @param rh The render height
     */
    void render(int rw, int rh);

    /**
     * Resizes this GUI to the given values at the end of the current loop
     *
     * @param width  The new width
     * @param height The new height
     */
    void resize(int width, int height);

    /**
     * Resizes without waiting for the current loop to finish
     *
     * @param width  The new width
     * @param height The new height
     */
    void resizeImmediate(int width, int height);

    /**
     * Removes the focus from this GUI and returns true if the focus was in the
     * GUI, false otherwise.
     *
     * @return true if the focus was in the GUI, false otherwise.
     */
    boolean cancelTouchFocus();

    /**
     * Returns the stage
     *
     * @return The stage
     */
    Stage getGuiStage();

    /**
     * Sets the visibility state of the component entities
     *
     * @param entities The entities
     * @param visible  The states
     */
    void setVisibilityToggles(ComponentType[] entities, ComponentTypes visible);

    /**
     * Returns the first actor found with the specified name. Note this
     * recursively compares the name of every actor in the GUI.
     *
     * @return The actor if it exists, null otherwise.
     **/
    Actor findActor(String name);

    /**
     * Whether this GUI is to be used in VR mode
     *
     * @param vr Vr mode is active
     */
    void setVR(boolean vr);

    /**
     * Returns whether this GUI is a VR gui.
     * @return Is this a VR gui?
     */
    boolean isVR();

    /**
     * Returns whether this GUI must be drawn or not
     *
     * @return Whether this is visible
     */
    boolean mustDraw();

    /**
     * Updates the units-per-pixel value of this GUI. The units-per-pixel
     * is the same as 1/UI_SCALE.
     */
    boolean updateUnitsPerPixel(float upp);

    /**
     * Sets the back buffer size.
     */
    void setBackBufferSize(int width, int height);
}