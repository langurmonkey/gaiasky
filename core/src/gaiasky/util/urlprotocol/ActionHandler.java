/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.urlprotocol;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import gaiasky.event.Event;
import gaiasky.event.EventManager;

import java.util.Map;

/**
 * Interface that defines the contract for all action handlers.
 */
public interface ActionHandler {

    /**
     * Process the action.
     *
     * @param url     The URL string.
     * @param skin    The UI skin.
     * @param stage   The UI stage.
     * @param hotLoad Whether to hot load.
     * @param params  The parameters.
     */
    void process(String url,
                 Skin skin,
                 Stage stage,
                 boolean hotLoad,
                 Map<String, String> params);

    default String getParameterValue(Map<String, String> params,
                                     String... names) {
        for (var name : names) {
            if (params.containsKey(name)) {
                return params.get(name);
            }
        }
        return null;
    }

    /**
     * Posts a popup notification.
     *
     * @param message The message.
     */
    default void postNotification(String message) {
        EventManager.publish(Event.POST_POPUP_NOTIFICATION, URLProtocolHandler.class, message, 10f);
    }
}
