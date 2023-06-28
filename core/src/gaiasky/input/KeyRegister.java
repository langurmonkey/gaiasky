/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.input;

import java.util.Collection;

/**
 * Small utility class that registers and keeps track of when keys are pressed.
 */
public class KeyRegister {
    /**
     * This array contains the last time every key was registered as pressed.
     **/
    private final long[] keyDownTime = new long[200];

    public void registerKeyDownTime(int key, long time) {
        if (key >= 0 && key < keyDownTime.length) {
            keyDownTime[key] = time;
        }
    }

    public void registerKeyDownTime(Collection<Integer> keys, long time) {
        keys.forEach(k -> registerKeyDownTime(k, time));
    }

    public long lastKeyDownTime(int key) {
        if (key >= 0 && key < keyDownTime.length) {
            return keyDownTime[key];
        }
        return -1L;
    }

    public long lastKeyDownTime(Collection<Integer> keys) {
        long t = 0;
        for (var key : keys) {
            long time = lastKeyDownTime(key);
            if (time > t) {
                t = time;
            }
        }
        return t;
    }
}
