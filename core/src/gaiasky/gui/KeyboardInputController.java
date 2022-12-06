/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.KeyBindings.ProgramAction;
import gaiasky.util.Settings;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This input inputListener connects the input events with the key binding actions
 */
public class KeyboardInputController extends InputAdapter implements IObserver {

    public KeyBindings mappings;
    /** Holds the pressed keys at any moment **/
    public Set<Integer> pressedKeys;

    private final Input input;

    public KeyboardInputController(Input input) {
        super();
        this.input = input;
        pressedKeys = new HashSet<>();
        KeyBindings.initialize();
        mappings = KeyBindings.instance;
        EventManager.instance.subscribe(this, Event.CLEAN_PRESSED_KEYS);
    }

    @Override
    public boolean keyDown(int keycode) {
        cleanSpecial();

        if (Settings.settings.runtime.inputEnabled) {
            pressedKeys.add(keycode);
        }
        return false;

    }

    @Override
    public boolean keyUp(int keycode) {
        EventManager.publish(Event.INPUT_EVENT, this, keycode);

        cleanSpecial();

        if (Settings.settings.runtime.inputEnabled) {
            // Use key mappings
            ProgramAction action = mappings.getMappings().get(pressedKeys);
            if (action != null) {
                action.run();
            }
        } else if (keycode == Keys.ESCAPE) {
            // If input is not enabled, only escape works
            EventManager.publish(Event.SHOW_QUIT_ACTION, this);
        }
        pressedKeys.remove(keycode);
        return false;

    }

    /**
     * Makes sure all unpressed special keys are not on the pressed keys list.
     */
    private void cleanSpecial() {
        for (int special : KeyBindings.SPECIAL) {
            if (!input.isKeyPressed(special))
                pressedKeys.remove(special);
        }
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (Objects.requireNonNull(event) == Event.CLEAN_PRESSED_KEYS) {
            if (pressedKeys != null) {
                pressedKeys.clear();
            }
        }
    }
}
