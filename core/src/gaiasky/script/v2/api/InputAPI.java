/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import com.badlogic.gdx.Input;
import gaiasky.gui.main.GSKeys;
import gaiasky.script.v2.impl.UiModule;

/**
 * Public API definition for the UI module, {@link UiModule}.
 * <p>
 * The UI module contains methods and calls that modify and query the user interface.
 */
public interface InputAPI {
    /**
     * Disable all input events in Gaia Sky (mouse, keyboard, touchscreen, and gamepad) until a call to
     * {@link #enable()} is issued.
     * <p>
     * After this method is called, Gaia Sky will not respond to any kind of input event.
     */
    void disable();

    /**
     * Enable all input events in Gaia Sky (mouse, keyboard, touchscreen, and gamepad). This call re-enables
     * all input events, which were possibly disabled with {@link #disable()}.
     */
    void enable();

    /**
     * Block the execution until any kind of input (keyboard, mouse, etc.) is
     * received.
     */
    void wait_input();

    /**
     * Block the execution until the given key or button is pressed.
     *
     * @param keyCode The key or button code. Please see
     *                {@link Input.Keys} and {@link GSKeys}.
     */
    void wait_input(int keyCode);

    /**
     * Block the execution until the Enter key is pressed.
     */
    void wait_enter();
}
