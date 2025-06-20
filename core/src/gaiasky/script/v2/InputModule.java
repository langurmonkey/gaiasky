/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2;

import com.badlogic.gdx.Input;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.main.GSKeys;

import java.util.Objects;

/**
 * The input module contains methods and calls to manage input data like keyboard and/or mouse
 * input events.
 */
public class InputModule extends APIModule implements IObserver {
    /** Last keyboard input code. **/
    private int inputCode = -1;

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public InputModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);

        em.subscribe(this, Event.INPUT_EVENT);
    }

    /**
     * Blocks the execution until any kind of input (keyboard, mouse, etc.) is
     * received.
     */
    public void wait_input() {
        while (inputCode < 0) {
            api.base.sleep_frames(1);
        }
        // Consume
        inputCode = -1;

    }

    /**
     * Blocks the execution until the Enter key is pressed.
     */
    public void wait_enter() {
        while (inputCode != Input.Keys.ENTER) {
            api.base.sleep_frames(1);
        }
        // Consume
        inputCode = -1;
    }

    /**
     * Blocks the execution until the given key or button is pressed.
     *
     * @param keyCode The key or button code. Please see
     *                {@link Input.Keys} and {@link GSKeys}.
     */
    public void wait_input(int keyCode) {
        while (inputCode != keyCode) {
            api.base.sleep_frames(1);
        }
        // Consume
        inputCode = -1;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.INPUT_EVENT) {
            inputCode = (Integer) data[0];
        }
    }

}
