/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import com.badlogic.gdx.Input;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.script.v2.api.InputAPI;

import java.util.Objects;

/**
 * The camera module contains methods and calls to access and modify the input system.
 */
public class InputModule extends APIModule implements IObserver, InputAPI {
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

    @Override
    public void disable() {
        api.base.post_runnable(() -> em.post(Event.INPUT_ENABLED_CMD, this, false));
    }

    @Override
    public void enable() {
        api.base.post_runnable(() -> em.post(Event.INPUT_ENABLED_CMD, this, true));
    }

    @Override
    public void wait_input() {
        while (inputCode < 0) {
            api.base.sleep_frames(1);
        }
        // Consume
        inputCode = -1;

    }

    @Override
    public void wait_enter() {
        while (inputCode != Input.Keys.ENTER) {
            api.base.sleep_frames(1);
        }
        // Consume
        inputCode = -1;
    }

    @Override
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
