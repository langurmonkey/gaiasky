/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interafce;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.interafce.KeyBindings.ProgramAction;
import gaiasky.util.GlobalConf;

import java.util.HashSet;
import java.util.Set;

/**
 * This input inputListener connects the input events with the key binding actions
 */
public class KeyboardInputController extends InputAdapter {

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
    }

    @Override
    public boolean keyDown(int keycode) {
        cleanSpecial();

        if (GlobalConf.runtime.INPUT_ENABLED) {
            pressedKeys.add(keycode);
        }
        return false;

    }

    @Override
    public boolean keyUp(int keycode) {
        EventManager.instance.post(Events.INPUT_EVENT, keycode);

        cleanSpecial();

        if (GlobalConf.runtime.INPUT_ENABLED) {
            // Use key mappings
            ProgramAction action = mappings.getMappings().get(pressedKeys);
            if (action != null) {
                action.run();
            }
        } else if (keycode == Keys.ESCAPE) {
            // If input is not enabled, only escape works
            EventManager.instance.post(Events.SHOW_QUIT_ACTION);
        }
        pressedKeys.remove(keycode);
        return false;

    }

    /**
     * Makes sure all unpressed special keys are not on the pressed keys list.
     */
    private void cleanSpecial(){
        for (int special : KeyBindings.SPECIAL){
            if (!input.isKeyPressed(special))
                pressedKeys.remove(special);
        }
    }
}
