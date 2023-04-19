/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.event.Event;
import gaiasky.event.EventManager;

public abstract class OneTimeRunnable implements Runnable {
    private final String name;

    public OneTimeRunnable(String name) {
        this.name = name;
    }

    public void post() {
        EventManager.publish(Event.PARK_RUNNABLE, this, name, this);
    }

    @Override
    public void run() {
        // Run process.
        process();
        // Unpark runnable.
        EventManager.publish(Event.UNPARK_RUNNABLE, this, name);
    }

    protected abstract void process();
}
