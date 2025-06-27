/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.impl;

import gaiasky.GaiaSky;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.script.v2.api.TimeAPI;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.time.ITimeFrameProvider;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The time module contains methods and calls to access, modify, and query the time subsystem.
 */
public class TimeModule extends APIModule implements TimeAPI {
    /** Internal time transition sequence number. **/
    private int cTransSeq = 0;

    /**
     * Create a new module with the given attributes.
     *
     * @param em   Reference to the event manager.
     * @param api  Reference to the API class.
     * @param name Name of the module.
     */
    public TimeModule(EventManager em, APIv2 api, String name) {
        super(em, api, name);
    }

    @Override
    public void activate_real_time_frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.REAL_TIME));
    }

    @Override
    public void activate_simulation_time_frame() {
        api.base.post_runnable(() -> em.post(Event.EVENT_TIME_FRAME_CMD, this, EventManager.TimeFrame.SIMULATION_TIME));
    }

    @Override
    public void set_clock(int year, int month, int day, int hour, int min, int sec, int millisec) {
        if (api.validator.checkDateTime(year, month, day, hour, min, sec, millisec)) {
            LocalDateTime date = LocalDateTime.of(year, month, day, hour, min, sec, millisec);
            em.post(Event.TIME_CHANGE_CMD, this, date.toInstant(ZoneOffset.UTC));
        }
    }

    @Override
    public long get_clock() {
        ITimeFrameProvider time = GaiaSky.instance.time;
        return time.getTime().toEpochMilli();
    }

    @Override
    public void set_clock(final long time) {
        if (api.validator.checkNum(time, -Long.MAX_VALUE, Long.MAX_VALUE, "time")) em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(time));
    }

    @Override
    public int[] get_clock_array() {
        ITimeFrameProvider time = GaiaSky.instance.time;
        Instant instant = time.getTime();
        LocalDateTime c = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        int[] result = new int[7];
        result[0] = c.get(ChronoField.YEAR_OF_ERA);
        result[1] = c.getMonthValue();
        result[2] = c.getDayOfMonth();
        result[3] = c.getHour();
        result[4] = c.getMinute();
        result[5] = c.getSecond();
        result[6] = c.get(ChronoField.MILLI_OF_SECOND);
        return result;
    }

    @Override
    public void start_clock() {
        em.post(Event.TIME_STATE_CMD, this, true);
    }

    @Override
    public void stop_clock() {
        em.post(Event.TIME_STATE_CMD, this, false);
    }

    @Override
    public boolean is_clock_on() {
        return GaiaSky.instance.time.isTimeOn();
    }

    @Override
    public void set_time_warp(final double warpFactor) {
        em.post(Event.TIME_WARP_CMD, this, warpFactor);
    }

    /**
     * Alias to {@link #set_time_warp(double)}.
     */
    public void set_time_warp(final long warp) {
        set_time_warp((double) warp);
    }

    @Override
    public void set_target_time(long ms) {

        em.post(Event.TARGET_TIME_CMD, this, Instant.ofEpochMilli(ms));
    }

    @Override
    public void set_target_time(int year, int month, int day, int hour, int min, int sec, int millisec) {
        if (api.validator.checkDateTime(year, month, day, hour, min, sec, millisec)) {
            em.post(Event.TARGET_TIME_CMD, this, LocalDateTime.of(year, month, day, hour, min, sec, millisec).toInstant(ZoneOffset.UTC));
        }
    }

    @Override
    public void remove_target_time() {
        em.post(Event.TARGET_TIME_CMD, this);
    }

    @Override
    public void set_max_simulation_time(long years) {
        if (api.validator.checkFinite(years, "years")) {
            Settings.settings.runtime.setMaxTime(Math.abs(years));
        }
    }

    public void setMaximumSimulationTime(double years) {
        set_max_simulation_time((long) years);
    }

    public void setMaximumSimulationTime(Long years) {
        set_max_simulation_time(years);
    }

    public void setMaximumSimulationTime(Double years) {
        set_max_simulation_time(years.longValue());
    }

    public void setMaximumSimulationTime(Integer years) {
        set_max_simulation_time(years.longValue());
    }


    @Override
    public void transition(int year,
                           int month,
                           int day,
                           int hour,
                           int min,
                           int sec,
                           int milliseconds,
                           double durationSeconds,
                           String smoothType,
                           double smoothFactor,
                           boolean sync) {
        transition(year, month, day, hour, min, sec, milliseconds, durationSeconds, smoothType, smoothFactor, sync, null);
    }

    public void transition(int year,
                           int month,
                           int day,
                           int hour,
                           int min,
                           int sec,
                           int milliseconds,
                           double durationSeconds,
                           String smoothType,
                           double smoothFactor,
                           boolean sync,
                           AtomicBoolean stop) {
        if (api.validator.checkDateTime(year, month, day, hour, min, sec, milliseconds)) {
            // Set up final actions
            String name = "timeTransition" + (cTransSeq++);
            Runnable end = null;
            if (!sync) end = () -> api.base.remove_runnable(name);

            // Create and park orientation transition runnable
            TimeTransitionRunnable r = new TimeTransitionRunnable(year,
                                                                  month,
                                                                  day,
                                                                  hour,
                                                                  min,
                                                                  sec,
                                                                  milliseconds,
                                                                  durationSeconds,
                                                                  smoothType,
                                                                  smoothFactor,
                                                                  end,
                                                                  stop);
            try {
                // Park the runnable.
                api.base.park_runnable(name, r);

                if (sync) {
                    // Wait on lock.
                    synchronized (r.lock) {
                        try {
                            r.lock.wait();
                        } catch (InterruptedException e) {
                            logger.error(e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e);
            } finally {
                // Remove the runnable.
                api.base.remove_runnable(name);
            }

        }
    }

    /**
     * This class manages time transitions.
     */
    class TimeTransitionRunnable implements Runnable {
        final Object lock;
        final Long currentTimeMs;
        final AtomicBoolean stop;
        Long targetTimeMs;
        Long dt;
        final double duration;
        double elapsed, start;
        Runnable end;
        /** Maps input x to output x for positions. **/
        Function<Double, Double> mapper;

        /**
         * Creates a time transition to the given time in UTC.
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
         *                        <li>"logit": starts slow and ends slow. The smooth factor must be over 12 to produce
         *                        an effect, otherwise, linear interpolation is used.</li>
         *                        <li>"logisticsigmoid": starts fast and ends fast. The smooth factor must be between
         *                        0.09 and 0.01.</li>
         *                        <li>"none": no smoothing is applied.</li>
         *                        </ul>
         * @param smoothFactor    Smoothing factor (depends on type, see #smoothType).
         * @param end             An optional runnable that is executed when the transition has completed.
         * @param stop            A reference to a boolean value as an {@link AtomicBoolean} that stops the execution of the runnable
         *                        when it changes to true.
         */
        public TimeTransitionRunnable(int year,
                                      int month,
                                      int day,
                                      int hour,
                                      int min,
                                      int sec,
                                      int milliseconds,
                                      double durationSeconds,
                                      String smoothType,
                                      double smoothFactor,
                                      Runnable end,
                                      AtomicBoolean stop) {

            lock = new Object();
            duration = durationSeconds;
            this.start = GaiaSky.instance.getT();
            this.currentTimeMs = GaiaSky.instance.time.getTime().toEpochMilli();
            this.mapper = getMapper(smoothType, smoothFactor);
            this.stop = stop;

            try {
                LocalDateTime date = LocalDateTime.of(year, month, day, hour, min, sec, milliseconds);
                var instant = date.toInstant(ZoneOffset.UTC);
                targetTimeMs = instant.toEpochMilli();
                dt = targetTimeMs - currentTimeMs;
            } catch (DateTimeException e) {
                logger.error("Could not create time transition: bad date.", e);
            }
        }

        private Function<Double, Double> getMapper(String smoothingType, double smoothingFactor) {
            Function<Double, Double> mapper;
            if (Objects.equals(smoothingType, "logisticsigmoid")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 12.0, 500.0);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logisticSigmoid(x, fac), 0.0, 1.0);
            } else if (Objects.equals(smoothingType, "logit")) {
                final double fac = MathUtilsDouble.clamp(smoothingFactor, 0.01, 0.09);
                mapper = (x) -> MathUtilsDouble.clamp(MathUtilsDouble.logit(x) * fac + 0.5, 0.0, 1.0);
            } else {
                mapper = (x) -> x;
            }
            return mapper;
        }

        @Override
        public void run() {
            // Update elapsed time.
            elapsed = GaiaSky.instance.getT() - start;

            double alpha = MathUtilsDouble.clamp(elapsed / duration, 0.0, 0.999999999999999999);
            // Linear interpolation in simulation time.
            alpha = mapper.apply(alpha);
            var t = currentTimeMs + (long) (alpha * dt);
            em.post(Event.TIME_CHANGE_CMD, this, Instant.ofEpochMilli(t));

            // Finish if needed.
            if ((stop != null && stop.get()) || elapsed >= duration) {
                // On end, run runnable if present, otherwise notify lock
                if (end != null) {
                    end.run();
                } else {
                    synchronized (lock) {
                        lock.notify();
                    }
                }
            }
        }
    }
}
