/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import com.badlogic.gdx.Gdx;
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

    protected final AtomicBoolean active;
    // Minimum time after key press before polling starts.
    protected long minPollTime = 150;
    protected long minPollInterval = 0;
    protected long lastPollTime = 0;

    protected AbstractMouseKbdListener(GestureListener gl, ICamera camera) {
        super(gl);
        this.iCamera = camera;
        this.active = new AtomicBoolean(true);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isActive()) {
            // Input-enabled setting only for non-GUI listeners.
            if (this instanceof GuiKbdListener || Settings.settings.runtime.inputEnabled) {
                if (iCamera != null) {
                    iCamera.setGamepadInput(false);
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isActive()) {
            if (iCamera != null) {
                iCamera.setGamepadInput(false);
            }
        }
        return false;

    }

    public boolean isKeyPressed(int keycode) {
        return Gdx.input.isKeyPressed(keycode);
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
            if (!isKeyPressed(k))
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
            if (isKeyPressed(k))
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
