/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.util.Logger;

public class ControllerConnectionListener extends ControllerAdapter {
    private static final Logger.Log logger = Logger.getLogger(ControllerConnectionListener.class);

    @Override
    public void connected (Controller controller) {
        logger.info("Controller connected: " + controller.getName());
        EventManager.instance.post(Events.CONTROLLER_CONNECTED_INFO, controller.getName());
    }

    @Override
    public void disconnected (Controller controller) {
        logger.info("Controller disconnected: " + controller.getName());
        EventManager.instance.post(Events.CONTROLLER_DISCONNECTED_INFO, controller.getName());
    }
}
