/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.time;

import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Nature;
import gaiasky.util.Settings;

import java.time.Instant;

/**
 * Keeps pace of the simulation time vs real time and holds the global clock. It
 * uses a time warp factor which is a multiplier to real time.
 */
public class GlobalClock implements IObserver, ITimeFrameProvider {
    private static final Log logger = Logger.getLogger(GlobalClock.class);

    /** The current time of the clock **/
    public Instant time;
    long lastTime;
    /** Target time to stop the clock, if any **/
    private Instant targetTime;

    /** The simulation time difference in hours **/
    private double hDiff;

    /** The frame time difference in seconds **/
    private double dt;

    /** Represents the time wrap multiplier. Scales the real time **/
    private double timeWarp = 1;

    // Seconds since last event POST
    private float lastUpdate = 1;
    /**
     * The fixed frame rate when not in real time. Set negative to use real time
     **/
    public float fps = -1;

    /**
     * Creates a new GlobalClock
     *
     * @param timeWrap The time wrap multiplier
     * @param instant  The instant with which to initialise the clock
     */
    public GlobalClock(double timeWrap, Instant instant) {
        super();
        // Now
        this.timeWarp = timeWrap;
        hDiff = 0d;
        time = instant;
        targetTime = null;
        lastTime = time.toEpochMilli();
        EventManager.instance.subscribe(this, Event.TIME_WARP_CMD, Event.TIME_WARP_DECREASE_CMD, Event.TIME_WARP_INCREASE_CMD, Event.TIME_CHANGE_CMD, Event.TARGET_TIME_CMD);
    }

    /**
     * Update function
     *
     * @param dt Delta time in seconds
     */
    public void update(double dt) {
        this.dt = dt;
        Settings settings = Settings.settings;
        dt = settings.runtime.timeOn ? this.dt : 0;

        if (dt != 0) {
            // In case we are in constant rate mode
            if (fps > 0) {
                dt = 1 / fps;
            }

            int sign = (int) Math.signum(timeWarp);
            double h = Math.abs(dt * timeWarp * Nature.S_TO_H);
            hDiff = h * sign;

            double ms = sign * h * Nature.H_TO_MS;

            long currentTime = time.toEpochMilli();
            lastTime = currentTime;

            long newTime = currentTime + (long) ms;
            // Check target time
            if (targetTime != null) {
                long target = targetTime.toEpochMilli();
                if ((timeWarp > 0 && currentTime <= target && newTime > target) || (timeWarp < 0 && currentTime >= target && newTime < target)) {
                    newTime = target;
                    // Unset target time
                    targetTime = null;
                    // STOP!
                    setTimeWarp(0);
                }
            }

            if (newTime > settings.runtime.maxTimeMs) {
                if (currentTime < settings.runtime.maxTimeMs) {
                    logger.info("Maximum time reached (" + (settings.runtime.maxTimeMs * Nature.MS_TO_Y) + " years)!");
                    // Turn off time
                    EventManager.publish(Event.TIME_STATE_CMD, this, false);
                }
                newTime = settings.runtime.maxTimeMs;
                time = Instant.ofEpochMilli(newTime);
                EventManager.publish(Event.TIME_CHANGE_INFO, this, time);
                lastUpdate = 0;
            } else if (newTime < settings.runtime.minTimeMs) {
                if (currentTime > settings.runtime.minTimeMs) {
                    logger.info("Minimum time reached (" + (settings.runtime.minTimeMs * Nature.MS_TO_Y) + " years)!");
                    // Turn off time
                    EventManager.publish(Event.TIME_STATE_CMD, this, false);
                }
                newTime = settings.runtime.minTimeMs;
                time = Instant.ofEpochMilli(newTime);
                EventManager.publish(Event.TIME_CHANGE_INFO, this, time);
                lastUpdate = 0;
            } else {
                time = Instant.ofEpochMilli(newTime);
            }

            // Post event each 1/2 second
            lastUpdate += dt;
            if (lastUpdate > .5) {
                EventManager.publish(Event.TIME_CHANGE_INFO, this, time);
                lastUpdate = 0;
            }
        } else if (time.toEpochMilli() - lastTime != 0) {
            hDiff = (time.toEpochMilli() - lastTime) * Nature.MS_TO_H;
            lastTime = time.toEpochMilli();
        } else {
            hDiff = 0d;
        }
    }

    @Override
    public Instant getTime() {
        return time;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
        case TARGET_TIME_CMD:
            if (data.length > 0) {
                targetTime = (Instant) data[0];
            } else {
                targetTime = null;
            }
            break;
        case TIME_WARP_CMD:
            // Update pace
            setTimeWarp((Double) data[0]);
            break;
        case TIME_WARP_INCREASE_CMD:
            double tw;
            if (timeWarp == 0) {
                tw = 0.125;
            } else if (timeWarp == -0.125) {
                tw = 0;
            } else if (timeWarp < 0) {
                tw = timeWarp / 2.0;
            } else {
                tw = timeWarp * 2.0;
            }
            setTimeWarp(tw);
            break;
        case TIME_WARP_DECREASE_CMD:
            if (timeWarp == 0.125) {
                tw = 0;
            } else if (timeWarp == 0) {
                tw = -0.125;
            } else if (timeWarp < 0) {
                tw = timeWarp * 2.0;
            } else {
                tw = timeWarp / 2.0;
            }
            setTimeWarp(tw);
            break;
        case TIME_CHANGE_CMD:
            // Update time
            Instant newinstant = ((Instant) data[0]);
            long newt = newinstant.toEpochMilli();
            boolean updt = false;
            Settings settings = Settings.settings;
            if (newt > settings.runtime.maxTimeMs) {
                newt = settings.runtime.maxTimeMs;
                logger.info("Time overflow, set to maximum (" + (settings.runtime.maxTimeMs * Nature.MS_TO_Y) + " years)");
                updt = true;
            }
            if (newt < settings.runtime.minTimeMs) {
                newt = settings.runtime.minTimeMs;
                logger.info("Time overflow, set to minimum (" + (settings.runtime.minTimeMs * Nature.MS_TO_Y) + " years)");
                updt = true;
            }
            if (updt) {
                this.time = Instant.ofEpochMilli(newt);
            } else {
                this.time = newinstant;
            }
            break;
        default:
            break;
        }

    }

    public void setTimeWarp(double tw) {
        this.timeWarp = tw;
        checkTimeWarpValue();
        EventManager.publish(Event.TIME_WARP_CHANGED_INFO, this, this.timeWarp);
    }

    private void checkTimeWarpValue() {
        if (timeWarp > Constants.MAX_WARP) {
            timeWarp = Constants.MAX_WARP;
        }
        if (timeWarp < Constants.MIN_WARP) {
            timeWarp = Constants.MIN_WARP;
        }
    }

    /**
     * Provides the time difference in hours
     */
    @Override
    public double getHdiff() {
        return this.hDiff;
    }

    @Override
    public double getDt() {
        return this.dt;
    }

    @Override
    public double getWarpFactor() {
        return this.timeWarp;
    }

    public boolean isFixedRateMode() {
        return fps > 0;
    }

    @Override
    public float getFixedRate() {
        return fps;
    }

    @Override
    public boolean isTimeOn() {
        return timeWarp != 0d;
    }

}
