/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.UiModule;

/**
 * API definition for the UI module, {@link UiModule}.
 * <p>
 * The UI module contains calls and methods to access, modify, and query the user interface.
 */
public interface UiAPI {
    /**
     * Display a popup notification on the screen with the given contents for the default duration of 8 seconds.
     * The notification appears at the top-right of the screen and stays there until the duration time elapses, then it disappears.
     *
     * @param message The notification text.
     */
    void display_popup_notification(String message);

    /**
     * Display a popup notification on the screen for the given duration.
     * The notification appears at the top-right of the screen and stays there until the duration time elapses, then it disappears.
     *
     * @param message  The notification text.
     * @param duration The duration, in seconds, until the notification automatically disappears. Set this to a negative number so that the
     *                 notification never expires. If this is the case, the notification must be manually closed by the user.
     */
    void display_popup_notification(String message, float duration);

    /**
     * Set the contents of the headline message. The headline message appears in the middle of the screen with a big font.
     *
     * @param headline The headline text.
     */
    void set_headline_message(final String headline);

    /**
     * Set the contents of the sub-header message. The sub-header message appears just below the headline message, in the middle of the screen,
     * with a somewhat smaller font.
     *
     * @param subhead The sub-header text.
     */
    void set_subhead_message(final String subhead);

    /**
     * Clear the content of the headline message. After this method is called, the headline message disappears from screen.
     */
    void clear_headline_message();

    /**
     * Clears the content of the sub-header message. After this method is called, the sub-header message disappears from screen.
     */
    void clear_subhead_messages();

    /**
     * Clears both the headline and the sub-header messages. After this method is called, both the headline and the sub-header messages
     * disappear from screen.
     */
    void clear_all_messages();
}
