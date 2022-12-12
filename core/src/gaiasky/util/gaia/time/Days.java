/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * Copyright (C) 2006-2011 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaiasky.util.gaia.time;

import java.io.Serializable;

/**
 * A finite number of days
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The oder implementation is provided as static class methods.
 * <p>
 * Performance tests of both implementations have come up with a performance
 * improvement of 20% of the static methods compared with the object methods.
 *
 * @author ulammers
 * @version $Id: Days.java 405499 2014-12-18 20:21:02Z hsiddiqu $
 */
public class Days extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Days() {
    }

    /**
     * Construct object
     *
     * @param days number of days
     */
    public Days(final double days) {
        value = days;
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in nanoSec
     */
    public static long asNanoSecs(final double days) {
        return Math.round(days * Duration.NS_PER_DAY);
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in sec
     */
    public static double asSecs(final double days) {
        return days * Duration.SECS_PER_DAY;
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in mins
     */
    public static double asMins(final double days) {
        return days * Duration.MINS_PER_DAY;
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in revolutions
     */
    public static double asRevs(final double days) {
        return days * Duration.REVS_PER_DAY;
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in hours
     */
    public static double asHours(final double days) {
        return days * Duration.HOURS_PER_DAY;
    }

    /**
     * @param days The time in days to convert.
     *
     * @return days expressed in julian years
     */
    public static double asJulianYears(final double days) {
        return days / Duration.DAYS_PER_JULIAN_YEAR;
    }

    /**
     * @param d Duration
     *
     * @return Duration
     *
     * @see gaiasky.util.gaia.time.Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asDays();

        return this;
    }

    /**
     * @return long
     *
     * @see gaiasky.util.gaia.time.Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_DAY);
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value * Duration.SECS_PER_DAY;
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asMins()
     */
    @Override
    public double asMins() {
        return value * Duration.MINS_PER_DAY;
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asHours()
     */
    @Override
    public double asHours() {
        return value * Duration.HOURS_PER_DAY;
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asRevs
     */
    @Override
    public double asRevs() {
        return value * Duration.REVS_PER_DAY;
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asDays()
     */
    @Override
    public double asDays() {
        return value;
    }

    /**
     * @return double
     *
     * @see gaiasky.util.gaia.time.Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value / Duration.DAYS_PER_JULIAN_YEAR;
    }

    /**
     * @return Duration
     *
     * @see gaiasky.util.gaia.time.Duration#negate()
     */
    @Override
    public Duration negate() {
        value = -value;

        return this;
    }

    /**
     * @param d Duration
     *
     * @return Duration
     *
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asDays();

        return this;
    }

    /**
     * @param d Duration
     *
     * @return Duration
     *
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asDays();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#clone()
     */
    public Days clone() {
        return new Days(value);
    }
}
