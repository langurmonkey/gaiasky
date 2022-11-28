/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.gui.IInputListener;
import gaiasky.scene.camera.ICamera;
import gaiasky.util.Settings;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Abstract mouse and keyboard input listener. Provides tracking of pressed keys.
 */
public abstract class AbstractMouseKbdListener extends GestureDetector implements IInputListener {

    protected ICamera iCamera;
    /** Holds the pressed keys at any moment **/
    protected IntSet pressedKeys;

    protected final AtomicBoolean active;
    // Minimum time after key press before polling starts.
    protected long minPollTime =  150;
    protected long minPollInterval = 0;
    protected long lastPollTime = 0;

    protected AbstractMouseKbdListener(GestureListener gl, ICamera camera) {
        super(gl);
        this.iCamera = camera;
        this.pressedKeys = new IntSet();
        this.active = new AtomicBoolean(true);
    }

    public void addPressedKey(int keycode) {
        pressedKeys.add(keycode);
    }

    public void removePressedKey(int keycode) {
        pressedKeys.remove(keycode);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isActive()) {
            boolean b = false;
            // Input-enabled setting only for non-GUI listeners.
            if (this instanceof GuiKbdListener || Settings.settings.runtime.inputEnabled) {
                b = pressedKeys.add(keycode);
                if (iCamera != null) {
                    iCamera.setGamepadInput(false);
                }
            }
            return b;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isActive()) {
            boolean b = pressedKeys.remove(keycode);
            if (iCamera != null) {
                iCamera.setGamepadInput(false);
            }
            return b;
        }
        return false;

    }

    public boolean isKeyPressed(int keycode) {
        return pressedKeys.contains(keycode);
    }

    /**
     * Returns true if all keys are pressed
     *
     * @param keys The keys to test
     *
     * @return True if all are pressed
     */
    public boolean allPressed(int... keys) {
        for (int k : keys) {
            if (!pressedKeys.contains(k))
                return false;
        }
        return true;
    }

    /**
     * Returns true if any of the keys are pressed
     *
     * @param keys The keys to test
     *
     * @return True if any is pressed
     */
    public boolean anyPressed(int... keys) {
        for (int k : keys) {
            if (pressedKeys.contains(k))
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
