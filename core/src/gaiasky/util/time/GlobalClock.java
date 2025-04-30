/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.time;

import com.badlogic.gdx.math.MathUtils;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.event.IObserver;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.Nature;
import gaiasky.util.Settings;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.Arrays;

/**
 * Implementation of a time frame provider that provides simulation times governed by a warp value, which informs
 * the speed at which time passes.
 */
public class GlobalClock implements IObserver, ITimeFrameProvider {
    private static final Log logger = Logger.getLogger(GlobalClock.class);

    /**
     * The current time of the clock
     **/
    public Instant time;
    /**
     * The fixed frame rate when not in real time. Set negative to use real time
     **/
    public float fps = -1;
    long lastTime;
    /**
     * Target time to stop the clock, if any
     **/
    private Instant targetTime;
    /**
     * The simulation time difference in hours
     **/
    private double hDiff;
    /**
     * The frame time difference in seconds
     **/
    private double dt;
    /**
     * Represents the time wrap multiplier. Scales the real time
     **/
    private double timeWarp;
    // Seconds since last event POST
    private float lastUpdate = 1;

    // Warp steps per side + 0, 0.125, 0.250, 0.5
    public final int warpSteps = Constants.WARP_STEPS + 4;
    /**
     * Possible values for the time warp to snap.
     */
    private final double[] timeWarpVector;

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
        timeWarpVector = generateTimeWarpVector(warpSteps);

        EventManager.instance.subscribe(this, Event.TIME_WARP_CMD, Event.TIME_WARP_DECREASE_CMD, Event.TIME_WARP_INCREASE_CMD, Event.TIME_CHANGE_CMD, Event.TARGET_TIME_CMD);
    }

    /**
     * Generate the time warp vector with the default number of steps.
     *
     * @return The vector
     */
    public double[] generateTimeWarpVector() {
        return generateTimeWarpVector(warpSteps);
    }

    /**
     * Generate the time warp vector.
     *
     * @param steps The number of steps per side (positive and negative)
     * @return The vector
     */
    public double[] generateTimeWarpVector(int steps) {
        double[] warp = new double[steps * 2 + 1];
        warp[steps] = 0;
        // Positive
        double w = 0;
        for (int i = steps + 1; i < warp.length; i++) {
            warp[i] = increaseWarp(w);
            w = warp[i];
        }
        // Negative
        w = 0;
        for (int i = steps - 1; i >= 0; i--) {
            warp[i] = decreaseWarp(w);
            w = warp[i];
        }
        return warp;
    }

    private double increaseWarp(double timeWarp) {
        if (timeWarp == 0) {
            return 0.125;
        } else if (timeWarp == -0.125) {
            return 0;
        } else if (timeWarp < 0) {
            return timeWarp / 2.0;
        } else {
            return timeWarp * 2.0;
        }
    }

    private double decreaseWarp(double timeWarp) {
        if (timeWarp == 0.125) {
            return 0;
        } else if (timeWarp == 0) {
            return -0.125;
        } else if (timeWarp < 0) {
            return timeWarp * 2.0;
        } else {
            return timeWarp / 2.0;
        }
    }

    /**
     * Update function
     *
     * @param dt Delta time in seconds
     */
    public void update(double dt) {
        this.dt = dt;
        final var settings = Settings.settings;
        dt = settings.runtime.timeOn ? this.dt : 0;

        if (dt != 0) {
            // In case we are in constant rate mode
            if (fps > 0) {
                dt = 1 / fps;
            }

            int sign = (int) FastMath.signum(timeWarp);
            double h = FastMath.abs(dt * timeWarp * Nature.S_TO_H);
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
    public double getTimeSeconds() {
        return time.toEpochMilli() / 1000.0;
    }

    @Override
    public void notify(final Event event, Object source, final Object... data) {
        switch (event) {
            case TARGET_TIME_CMD -> {
                if (data.length > 0) {
                    targetTime = (Instant) data[0];
                } else {
                    targetTime = null;
                }
            }
            case TIME_WARP_CMD -> setTimeWarp((Double) data[0]);
            case TIME_WARP_INCREASE_CMD -> {
                double tw = findNearestWarpSnapValue(timeWarp, 1.0);
                setTimeWarp(tw);
            }
            case TIME_WARP_DECREASE_CMD -> {
                double tw = findNearestWarpSnapValue(timeWarp, -1.0);
                setTimeWarp(tw);
            }
            case TIME_CHANGE_CMD -> {
                // Update time
                Instant newinstant = ((Instant) data[0]);
                long newt = newinstant.toEpochMilli();
                boolean updt = false;
                final var settings = Settings.settings;
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
            }
            default -> {
            }
        }

    }


    /**
     * Finds the nearest value in the {@link GlobalClock#timeWarpVector} array in the given direction.
     *
     * @param value The value to use.
     * @param dir   The direction, either 1 or -1.
     * @return The nearest snap value in the given direction.
     */
    public double findNearestWarpSnapValue(double value, double dir) {
        int idx = Arrays.binarySearch(timeWarpVector, value);
        if (idx < 0) {
            idx = -idx - 1;
        }
        idx = MathUtils.clamp(idx, 0, timeWarpVector.length - 1);
        double v = timeWarpVector[idx];
        if (value == v) {
            // Our value is in the array, we must get either the left or the right values.
            return dir > 0 ? timeWarpVector[Math.min(timeWarpVector.length - 1, idx + 1)] : timeWarpVector[Math.max(0, idx - 1)];
        } else {
            // Our value is not in the array! Index points to the first element in the sequence that is greater than value.
            return dir < 0 ? timeWarpVector[Math.max(0, idx - 1)] : timeWarpVector[idx];
        }
    }

    /**
     * Finds the nearest power of two to n, in the given direction.
     *
     * @param n   The number.
     * @param dir Direction. Either 1.0 or -1.0.
     * @return The nearest power of two to n.
     */
    public double nearestPowerOf2(double n, double dir) {
        if (n == 1) {
            if (dir < 0) {
                return 0.5;
            }
        }
        if (n == -1) {
            if (dir > 0) {
                return -0.5;
            }
        }
        long a = (int) (Math.log(n) / FastMath.log(2.0));

        return (long) FastMath.pow(2.0, a + dir);
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
