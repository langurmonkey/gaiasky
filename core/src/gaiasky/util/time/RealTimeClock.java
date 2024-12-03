/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.time;

import com.badlogic.gdx.utils.TimeUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;

import java.time.Instant;

public class RealTimeClock implements ITimeFrameProvider {
    private static final double SEC_TO_HOUR = 1d / 3600d;

    private double dt;
    /** Current number of milliseconds from epoch (1970-01-01T00:00Z (ISO)). **/
    private long time;
    private double lastUpdate = 0;

    public RealTimeClock() {
        time = Instant.now().toEpochMilli();
    }

    /**
     * The dt in hours
     */
    @Override
    public double getHdiff() {
        return SEC_TO_HOUR;
    }

    @Override
    public double getDt() {
        return this.dt;
    }

    @Override
    public Instant getTime() {
        return Instant.ofEpochMilli(time);
    }

    @Override
    public double getTimeSeconds() {
        return time / 1000.0;
    }

    @Override
    public void update(double dt) {
        this.dt = dt;
        time = TimeUtils.millis();

        // Post event each 1/2 second
        lastUpdate += dt;
        if (lastUpdate > .5) {
            EventManager.publish(Event.TIME_CHANGE_INFO, this, time);
            lastUpdate = 0;
        }
    }

    @Override
    public double getWarpFactor() {
        return SEC_TO_HOUR;
    }

    @Override
    public boolean isFixedRateMode() {
        return false;
    }

    @Override
    public float getFixedRate() {
        return -1;
    }

    @Override
    public boolean isTimeOn() {
        return true;
    }

}
