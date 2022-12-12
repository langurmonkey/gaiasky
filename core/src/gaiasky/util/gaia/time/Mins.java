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
 * A finite number of minutes.
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The oder implementation is provided as static class methods.
 * <p>
 * Performance tests of both implementations have come up with a performance
 * improvement of 20% of the static methods compared with the object methods.
 *
 * @author ulammers
 * @version $Id: Mins.java 405499 2014-12-18 20:21:02Z hsiddiqu $
 */
public class Mins extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Mins() {
    }

    /**
     * Construct object from number of minutes.
     *
     * @param mins number of mins [minutes]
     */
    public Mins(final double mins) {
        value = mins;
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Minutes expressed in nanoSec
     */
    public static long asNanoSecs(final double mins) {
        return Math.round(mins * Duration.NS_PER_MIN);
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Mins expressed in secs
     */
    public static double asSecs(final double mins) {
        return mins * Duration.SECS_PER_MIN;
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Mins expressed in hours
     */
    public static double asHours(final double mins) {
        return mins / Duration.MINS_PER_HOUR;
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Mins expressed in revs
     */
    public static double asRevs(final double mins) {
        return mins / Duration.MINS_PER_REV;
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Mins expressed in days
     */
    public static double asDays(final double mins) {
        return mins / Duration.MINS_PER_DAY;
    }

    /**
     * @param mins Time in mins to convert
     *
     * @return Mins expressed in JulianYears
     */
    public static double asJulianYears(final double mins) {
        return mins / Duration.MINS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asMins();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_MIN);
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value * Duration.SECS_PER_MIN;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asMins()
     */
    @Override
    public double asMins() {
        return value;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asHours()
     */
    @Override
    public double asHours() {
        return value / Duration.MINS_PER_HOUR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return value / Duration.MINS_PER_REV;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asDays()
     */
    @Override
    public double asDays() {
        return value / Duration.MINS_PER_DAY;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value / Duration.MINS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asMins();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asMins();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#clone()
     */
    public Mins clone() {
        return new Mins(value);
    }

}
