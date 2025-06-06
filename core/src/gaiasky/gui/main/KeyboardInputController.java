/*
 * Copyright (c) 2023-2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui.main;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.gui.main.KeyBindings.ProgramAction;
import gaiasky.input.InputUtils;
import gaiasky.input.KeyRegister;
import gaiasky.util.Settings;

import java.util.Objects;
import java.util.TreeSet;

/**
 * Controls inputs from the keyboard.
 */
public class KeyboardInputController extends InputAdapter implements IObserver {

    private final Input input;
    public KeyBindings mappings;
    /**
     * Holds the pressed keys at any moment
     **/
    public TreeSet<Integer> pressedKeys;
    private final KeyRegister register;

    public KeyboardInputController(Input input) {
        super();
        this.input = input;
        this.register = new KeyRegister();
        pressedKeys = new TreeSet<>();
        KeyBindings.initialize();
        mappings = KeyBindings.instance;
        EventManager.instance.subscribe(this, Event.CLEAN_PRESSED_KEYS);
    }

    @Override
    public boolean keyDown(int keyCode) {
        // Convert to logical.
        keyCode = InputUtils.physicalToLogicalKeyCode(keyCode);

        cleanSpecial();

        if (Settings.settings.runtime.inputEnabled) {
            pressedKeys.add(keyCode);
            register.registerKeyDownTime(keyCode, TimeUtils.millis());
        }
        return false;

    }

    @Override
    public boolean keyUp(int keyCode) {
        // Convert to logical.
        keyCode = InputUtils.physicalToLogicalKeyCode(keyCode);

        EventManager.publish(Event.INPUT_EVENT, this, keyCode);

        cleanSpecial();
        long now = System.currentTimeMillis();

        if (Settings.settings.runtime.inputEnabled) {
            // Use key mappings
            ProgramAction action = mappings.getMappings().get(pressedKeys);
            if (action != null && (now - register.lastKeyDownTime(pressedKeys) < action.maxKeyDownTimeMs)) {
                action.run();
            }
        } else if (keyCode == Keys.ESCAPE) {
            // If input is not enabled, only escape works
            EventManager.publish(Event.SHOW_QUIT_ACTION, this);
        }
        pressedKeys.remove(keyCode);
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
    public void notify(Event event,
                       Object source,
                       Object... data) {
        if (Objects.requireNonNull(event) == Event.CLEAN_PRESSED_KEYS) {
            if (pressedKeys != null) {
                pressedKeys.clear();
            }
        }
    }
}
