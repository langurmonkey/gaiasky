/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.IntSet;
import gaiasky.scenegraph.camera.NaturalCamera;
import gaiasky.util.Settings;

public abstract class MouseKbdListener extends GestureDetector implements IInputListener {

    protected NaturalCamera camera;
    /** Holds the pressed keys at any moment **/
    protected IntSet pressedKeys;


    protected MouseKbdListener(GestureListener gl, NaturalCamera camera){
        super(gl);
        pressedKeys = new IntSet();
        this.camera = camera;
    }

    public void addPressedKey(int keycode) {
        pressedKeys.add(keycode);
    }
    public void removePressedKey(int keycode) {
        pressedKeys.remove(keycode);
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean b = false;
        if (Settings.settings.runtime.inputEnabled) {
            b = pressedKeys.add(keycode);
            camera.setInputByController(false);
        }
        return b;

    }

    @Override
    public boolean keyUp(int keycode) {
        boolean b = pressedKeys.remove(keycode);
        camera.setInputByController(false);
        return b;

    }

    public boolean isKeyPressed(int keycode) {
        return pressedKeys.contains(keycode);
    }

    /**
     * Returns true if all keys are pressed
     *
     * @param keys The keys to test
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
     * @return True if any is pressed
     */
    public boolean anyPressed(int... keys) {
        for (int k : keys) {
            if (pressedKeys.contains(k))
                return true;
        }
        return false;
    }

    public float getResponseTime(){
        return 0.25f;
    }
}
