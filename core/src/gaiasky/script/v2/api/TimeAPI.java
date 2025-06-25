/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.TimeModule;

/**
 * API definition for the time module, {@link TimeModule}.
 * <p>
 * The time module contains methods and calls to access, modify, and query the time subsystem.
 */
public interface TimeAPI {
    /**
     * Set the current time frame to <b>real time</b>. All the commands
     * executed after this command becomes active will be in the <b>real
     * time</b> frame (real clock ticks).
     */
    void activate_real_time_frame();

    /**
     * Set the current time frame to <b>simulation time</b>. All the commands
     * executed after this command becomes active will be in the <b>simulation
     * time</b> frame (simulation clock ticks).
     */
    void activate_simulation_time_frame();

    /**
     * Set the time of the application to the given time, in UTC.
     *
     * @param year     The year to represent.
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December).
     * @param day      The day-of-month to represent, from 1 to 31.
     * @param hour     The hour-of-day to represent, from 0 to 23.
     * @param min      The minute-of-hour to represent, from 0 to 59.
     * @param sec      The second-of-minute to represent, from 0 to 59.
     * @param millisec The millisecond-of-second, from 0 to 999.
     */
    void set_clock(int year,
                   int month,
                   int day,
                   int hour,
                   int min,
                   int sec,
                   int millisec);

    /**
     * Return the current simulation time as the number of milliseconds since
     * 1970-01-01T00:00:00Z (UTC).
     *
     * @return Number of milliseconds since the epoch (Jan 1, 1970 00:00:00 UTC).
     */
    long get_clock();

    /**
     * Set the time of the application. The long value represents specified
     * number of milliseconds since the standard base time known as "the epoch",
     * namely January 1, 1970, 00:00:00 GMT.
     *
     * @param time Number of milliseconds since the epoch (Jan 1, 1970).
     */
    void set_clock(long time);

    /**
     * Return the current UTC simulation time in an array.
     *
     * @return The current simulation time in an array with the given indices.
     *         <ul>
     *         <li>0 - The year.</li>
     *         <li>1 - The month, from 1 (January) to 12 (December).</li>
     *         <li>2 - The day-of-month, from 1 to 31.</li>
     *         <li>3 - The hour-of-day, from 0 to 23.</li>
     *         <li>4 - The minute-of-hour, from 0 to 59.</li>
     *         <li>5 - The second-of-minute, from 0 to 59.</li>
     *         <li>6 - The millisecond-of-second, from 0 to 999.</li>
     *         </ul>
     */
    int[] get_clock_array();

    /**
     * Start simulation time. This method causes the clock to start ticking at the current pace. You can modify the clock pace
     * with {@link #set_time_warp(double)}.
     */
    void start_clock();

    /**
     * Stop simulation time.
     */
    void stop_clock();

    /**
     * Query whether the simulation time is on or not.
     *
     * @return True if the time is on, false otherwise.
     */
    boolean is_clock_on();


    /**
     * Set the simulation time warp factor. Positive values make time advance forward, while negative values make time
     * run backwards. A warp factor of 1 sets a real time pace to the simulation time.
     *
     * @param warpFactor The warp as a factor. A value of 2.0 sets the
     *                   Gaia Sky time to be twice as fast as real world time.
     */
    void set_time_warp(double warpFactor);

    /**
     * Set a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param ms The time as the number of milliseconds since the epoch (Jan 1,
     *           1970).
     */
    void set_target_time(long ms);

    /**
     * Set a time bookmark in the global clock that, when reached, the clock
     * automatically stops.
     *
     * @param year     The year to represent.
     * @param month    The month-of-year to represent, from 1 (January) to 12
     *                 (December).
     * @param day      The day-of-month to represent, from 1 to 31.
     * @param hour     The hour-of-day to represent, from 0 to 23.
     * @param min      The minute-of-hour to represent, from 0 to 59.
     * @param sec      The second-of-minute to represent, from 0 to 59.
     * @param milliSec The millisecond-of-second, from 0 to 999.
     */
    void set_target_time(int year,
                         int month,
                         int day,
                         int hour,
                         int min,
                         int sec,
                         int milliSec);

    /**
     * Unset the target time bookmark from the global clock, if any.
     */
    void remove_target_time();

    /**
     * Set the maximum simulation time allowed, in years. This sets the maximum time in the future (years)
     * and in the past (-years). This setting is not saved to the configuration and resets to 5 Myr after
     * restart.
     *
     * @param years The maximum year number to allow.
     */
    void set_max_simulation_time(long years);

    /**
     * Create a time transition from the current time to the given time (year, month, day, hour, minute, second,
     * millisecond). The time is given in UTC.
     *
     * @param year            The year to represent.
     * @param month           The month-of-year to represent, from 1 (January) to 12
     *                        (December).
     * @param day             The day-of-month to represent, from 1 to 31.
     * @param hour            The hour-of-day to represent, from 0 to 23.
     * @param min             The minute-of-hour to represent, from 0 to 59.
     * @param sec             The second-of-minute to represent, from 0 to 59.
     * @param milliseconds    The millisecond-of-second, from 0 to 999.
     * @param durationSeconds The duration of the transition, in seconds.
     * @param smoothType      The function type to use for smoothing. Either "logit", "logisticsigmoid" or "none".
     *                        <ul>
     *                        <li>"logisticsigmoid": starts slow and ends slow. The smooth factor must be over 12 to produce
     *                        an effect, otherwise, linear interpolation is used.</li>
     *                        <li>"logit": starts fast and ends fast. The smooth factor must be between
     *                        0.09 and 0.01.</li>
     *                        <li>"none": no smoothing is applied.</li>
     *                        </ul>
     * @param smoothFactor    Smoothing factor (depends on type, see #smoothType).
     * @param sync            If true, the call waits for the transition to finish before returning,
     *                        otherwise it returns immediately.
     */
    void transition(int year,
                    int month,
                    int day,
                    int hour,
                    int min,
                    int sec,
                    int milliseconds,
                    double durationSeconds,
                    String smoothType,
                    double smoothFactor,
                    boolean sync);
}
