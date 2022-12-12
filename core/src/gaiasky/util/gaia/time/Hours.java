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
 * A finite number of hours
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The oder implementation is provided as static class methods.
 * <p>
 * Performance tests of both implementations have come up with a performance
 * improvement of 20% of the static methods compared with the object methods.
 *
 * @author ulammers
 * @version $Id: Hours.java 405499 2014-12-18 20:21:02Z hsiddiqu $
 */
public class Hours extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Hours() {
    }

    /**
     * Construct object
     *
     * @param hours number of hours
     */
    public Hours(final double hours) {
        value = hours;
    }

    /**
     * @param hours The time in hours to convert.
     *
     * @return hours expressed in nanosecs
     */
    public static long asNanoSecs(final double hours) {
        return Math.round(hours * Duration.NS_PER_HOUR);
    }

    /**
     * @param hours The time in hours to convert.
     *
     * @return hours expressed in secs
     */
    public static double asSecs(final double hours) {
        return hours * Duration.SECS_PER_HOUR;
    }

    /**
     * @param hours The time in hours to convert
     *
     * @return hours expressed in mins
     */
    public static double asMins(final double hours) {
        return hours * Duration.MINS_PER_HOUR;
    }

    /**
     * @param hours Time in hours to convert.
     *
     * @return hours expressed in revs
     */
    public static double asRevs(final double hours) {
        return hours / Duration.HOURS_PER_REV;
    }

    /**
     * @param hours Time in hours to convert.
     *
     * @return hours expressed in days
     */
    public static double asDays(final double hours) {
        return hours / Duration.HOURS_PER_DAY;
    }

    /**
     * @param hours Time in hours to convert.
     *
     * @return hours expressed in JulianYears
     */
    public static double asJulianYears(final double hours) {
        return hours / Duration.HOURS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asHours();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_HOUR);
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value * Duration.SECS_PER_HOUR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asMins()
     */
    @Override
    public double asMins() {
        return value * Duration.MINS_PER_HOUR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asHours()
     */
    @Override
    public double asHours() {
        return value;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return value / Duration.HOURS_PER_REV;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asDays()
     */
    @Override
    public double asDays() {
        return value / Duration.HOURS_PER_DAY;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value / Duration.HOURS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asHours();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asHours();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#clone()
     */
    public Hours clone() {
        return new Hours(value);
    }

}
