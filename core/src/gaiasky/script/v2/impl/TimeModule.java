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
import gaiasky.util.time.ITimeFrameProvider;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

/**
 * The time module contains methods and calls to access, modify, and query the time subsystem.
 */
public class TimeModule extends APIModule implements TimeAPI {
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
}
