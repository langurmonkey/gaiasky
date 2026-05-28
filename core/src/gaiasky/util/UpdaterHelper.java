/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import net.jafama.FastMath;

/**
 * This class helps send load progress events in a streamlined way.
 */
public class UpdaterHelper {
    private static final Logger.Log logger = Logger.getLogger(UpdaterHelper.class);

    private final String name;
    private final long count;
    private final long step;
    private boolean active;

    public UpdaterHelper(String name, long count) {
        this.name = name;
        this.count = count;
        this.step = FastMath.max(1L, FastMath.round(count / 100d));
    }


    public void start() {
        // Initialize progress with name.
        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 0f);
        active = true;
    }

    public void update(long current) {
        if (!active) {
            logger.warn("Using updater helper without starting it: " + name);
        } else {
            // Just update.
            if (current % step == 0) {
                EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, (float) current / (float) count);
            }
        }
    }

    public void end() {
        // Remove progress.
        EventManager.publish(Event.UPDATE_LOAD_PROGRESS, this, name, 2f);
        active = false;
    }
}
