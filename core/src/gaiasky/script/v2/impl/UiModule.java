/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.UiAPI;

/**
 * The UI module contains calls and methods to access, modify, and query the user interface.
 */
public class UiModule extends APIModule implements UiAPI {

    /**
     * Create a new module with the given attributes.
     *
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public UiModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }


    /**
     * Display a popup notification on the screen with the given contents for the default duration of 8 seconds.
     * The notification appears at the top-right of the screen and stays there until the duration time elapses, then it disappears.
     *
     * @param message The notification text.
     */
    public void display_popup_notification(String message) {
        if (api.validator.checkString(message, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, message);
        }
    }

    @Override
    public void display_popup_notification(String message, float duration) {
        if (api.validator.checkString(message, "message")) {
            em.post(Event.POST_POPUP_NOTIFICATION, this, message, duration);
        }
    }

    /**
     * Alias to {@link #display_popup_notification(String, float)}.
     */
    public void display_popup_notification(String message, Double duration) {
        display_popup_notification(message, duration.floatValue());
    }

    @Override
    public void set_headline_message(final String headline) {
        api.base.post_runnable(() -> em.post(Event.POST_HEADLINE_MESSAGE, this, headline));
    }

    @Override
    public void set_subhead_message(final String subhead) {
        api.base.post_runnable(() -> em.post(Event.POST_SUBHEAD_MESSAGE, this, subhead));
    }

    @Override
    public void clear_headline_message() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_HEADLINE_MESSAGE, this));
    }

    @Override
    public void clear_subhead_messages() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_SUBHEAD_MESSAGE, this));
    }

    @Override
    public void clear_all_messages() {
        api.base.post_runnable(() -> em.post(Event.CLEAR_MESSAGES, this));
    }
}
