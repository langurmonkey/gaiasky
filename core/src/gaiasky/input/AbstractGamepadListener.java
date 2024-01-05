/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.AbstractGamepadMappings;
import gaiasky.gui.IGamepadMappings;
import gaiasky.gui.IInputListener;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractGamepadListener implements ControllerListener, IInputListener, IObserver {
    private static final Log logger = Logger.getLogger(AbstractGamepadListener.class);

    protected static final float MIN_ZERO_POINT = 0.3f;

    /**
     * Delay between axis events.
     */
    protected long axisEventDelay = 200;

    /**
     * Delay between axis poll operations.
     */
    protected long axisPollDelay = 200;
    /**
     * Delay between button poll operations.
     */
    protected long buttonPollDelay = 400;

    protected final EventManager em;
    protected final AtomicBoolean active = new AtomicBoolean(true);
    protected Controller lastControllerUsed = null;
    protected IGamepadMappings mappings;
    protected long lastAxisEvtTime = 0, lastButtonPollTime = 0;

    public AbstractGamepadListener(String mappingsFile) {
        this(AbstractGamepadMappings.readGamepadMappings(mappingsFile));
    }

    public AbstractGamepadListener(IGamepadMappings mappings) {
        this.mappings = mappings;
        this.em = EventManager.instance;
        em.subscribe(this, Event.RELOAD_CONTROLLER_MAPPINGS);
    }

    /** Zero-point function for the axes. **/
    protected double applyZeroPoint(double value) {
        return Math.abs(value) >= Math.max(mappings.getZeroPoint(), MIN_ZERO_POINT) ? value : 0;
    }

    public IGamepadMappings getMappings() {
        return mappings;
    }

    public void setMappings(IGamepadMappings mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (isActive()) {
            lastControllerUsed = controller;
        }
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        if (isActive()) {
            lastControllerUsed = controller;
        }
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (isActive()) {
            lastControllerUsed = controller;
        }
        return false;
    }

    /**
     * Checks whether the button with the given code is pressed in the last controller used.
     *
     * @param buttonCode The button code.
     *
     * @return Whether the button is pressed in the last controller used.
     */
    public boolean isKeyPressed(int buttonCode) {
        return isKeyPressed(lastControllerUsed, buttonCode);
    }

    public boolean isKeyPressed(Controller controller, int buttonCode) {
        return controller != null && controller.getButton(buttonCode);
    }

    /**
     * Returns true if any of the buttons are pressed in the last controller used.
     *
     * @param buttonCodes The buttons to test.
     *
     * @return True if any of the given buttons is pressed.
     */
    public boolean anyPressed(int... buttonCodes) {
        return anyPressed(lastControllerUsed, buttonCodes);
    }

    public boolean anyPressed(Controller controller, int... buttonCodes) {
        if (controller == null) {
            return false;
        }
        for (int buttonCode : buttonCodes) {
            if (isKeyPressed(controller, buttonCode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void connected(Controller controller) {
        logger.info("Controller connected: " + controller.getName());
        em.post(Event.CONTROLLER_CONNECTED_INFO, this, controller.getName());
    }

    @Override
    public void disconnected(Controller controller) {
        logger.info("Controller disconnected: " + controller.getName());
        em.post(Event.CONTROLLER_DISCONNECTED_INFO, this, controller.getName());
    }

    public abstract boolean pollAxes();

    public abstract boolean pollButtons();

    @Override
    public void update() {
        if (active.get()) {
            long now = TimeUtils.millis();
            // AXES POLL
            if (now - lastAxisEvtTime > axisPollDelay) {
                if (pollAxes()) {
                    lastAxisEvtTime = now;
                }
            }

            // BUTTONS POLL
            if (now - lastButtonPollTime > buttonPollDelay) {
                if (pollButtons()) {
                    lastButtonPollTime = now;
                }
            }
        }
    }

    @Override
    public void activate() {
        active.set(true);
    }

    @Override
    public void deactivate() {
        active.set(false);
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        if (event == Event.RELOAD_CONTROLLER_MAPPINGS) {
            mappings = AbstractGamepadMappings.readGamepadMappings((String) data[0]);
        }
    }
}
