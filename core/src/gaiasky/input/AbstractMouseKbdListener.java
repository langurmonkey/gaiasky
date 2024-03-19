/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.GaiaSky;
import gaiasky.desktop.GaiaSkyDesktop.CLIArgs;
import gaiasky.gui.IInputListener;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Settings;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractMouseKbdListener extends GestureDetector implements IInputListener {
    private static final Log logger = Logger.getLogger(AbstractMouseKbdListener.class);

    protected final AtomicBoolean active;
    protected ICamera iCamera;
    // Minimum time after key press before polling starts.
    protected long minPollTime = 150;
    protected long minPollInterval = 0;
    protected long lastPollTime = 0;
    private final CLIArgs cliArgs;

    protected AbstractMouseKbdListener(GestureListener gl, ICamera camera) {
        super(gl);
        this.iCamera = camera;
        this.active = new AtomicBoolean(true);
        this.cliArgs = GaiaSky.instance.getCliArgs();
    }

    @Override
    public boolean keyDown(int keyCode) {
        if (isActive()) {
            // Input-enabled setting only for non-GUI listeners.
            if (this instanceof GuiKbdListener || Settings.settings.runtime.inputEnabled) {
                if (iCamera != null) {
                    iCamera.setGamepadInput(false);
                }
            }
            if (cliArgs.debugInput) {
                logger.info(String.format("Key down: %d", keyCode));
            }
        }
        return false;
    }

    @Override
    public boolean keyUp(int keyCode) {
        if (isActive()) {
            if (iCamera != null) {
                iCamera.setGamepadInput(false);
            }
            if (cliArgs.debugInput) {
                logger.info(String.format("Key up: %d", keyCode));
            }
        }
        return false;
    }

    /**
     * Returns whether the key is pressed.
     *
     * @param keyCode The key code as found in {@link Input.Keys}.
     * @return true or false.
     */
    public boolean isKeyPressed(int keyCode) {
        return Gdx.input.isKeyPressed(keyCode);
    }

    /**
     * Returns whether the given logical key code is pressed.
     *
     * @param keyCode The logical key code as found in {@link Input.Keys}.
     * @return true or false.
     */
    public boolean isLogicalKeyPressed(int keyCode) {
        return Gdx.input.isKeyPressed(InputUtils.physicalToLogicalKeyCode(keyCode));
    }

    /**
     * Returns true if all keys are pressed
     *
     * @param keys The keys to test
     * @return True if all are pressed
     */
    public boolean allPressed(int... keys) {
        if (keys == null) {
            return false;
        }

        for (int k : keys) {
            if (!isKeyPressed(k))
                return false;
        }
        return true;
    }

    public boolean allPressed(Collection<Integer> keys) {
        if (keys == null) {
            return false;
        }

        for (int k : keys) {
            if (!isKeyPressed(k))
                return false;
        }
        return true;
    }

    /**
     * Returns true if any of the physical keys are pressed.
     *
     * @param keys The keys to test.
     * @return True if any physical keys are pressed.
     */
    public boolean anyPressed(int... keys) {
        for (int k : keys) {
            if (isKeyPressed(k))
                return true;
        }
        return false;
    }

    /**
     * Same as {@link AbstractMouseKbdListener#anyPressed(int...)}, but converts the physical keys to logical keys first.
     *
     * @param keys The logical keys.
     * @return Whether any of the keys are pressed.
     */
    public boolean anyPressedLogical(int... keys) {
        for (int k : keys) {
            if (isLogicalKeyPressed(k))
                return true;
        }
        return false;
    }

    public float getResponseTime() {
        return 0.25f;
    }

    @Override
    public void update() {
        if (isActive()) {
            long now = TimeUtils.millis();
            long elapsed = now - lastPollTime;
            if (minPollInterval <= 0 || elapsed > minPollInterval) {
                if (pollKeys()) {
                    lastPollTime = now;
                }
            }
        }
    }

    /**
     * Implement key polling here.
     *
     * @return True if an action was successfully executed.
     */
    protected abstract boolean pollKeys();

    public boolean isActive() {
        return this.active.get();
    }

    @Override
    public void activate() {
        this.active.set(true);
    }

    @Override
    public void deactivate() {
        this.active.set(false);
    }

}
