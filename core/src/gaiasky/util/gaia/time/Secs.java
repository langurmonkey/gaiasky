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
 * A finite number of seconds.
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The oder implementation is provided as static class methods.
 * <p>
 * Performance tests of both implementations have come up with a performance
 * improvement of 20% of the static methods compared with the object methods.
 *
 * @author ulammers
 * @version $Id: Secs.java 405499 2014-12-18 20:21:02Z hsiddiqu $
 */
public class Secs extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Secs() {
    }

    /**
     * Construct object from an elapsed number of seconds.
     *
     * @param secs elapsed time [seconds]
     */
    public Secs(final double secs) {
        value = secs;
    }

    /**
     * @param secs Time in secs to process
     *
     * @return Secs expressed in nanosec
     */
    public static long asNanoSecs(final double secs) {
        return Math.round(secs * Duration.NS_PER_SEC);
    }

    /**
     * @param secs Time in secs to convert
     *
     * @return Secs expressed in mins
     */
    public static double asMins(final double secs) {
        return secs / Duration.SECS_PER_MIN;
    }

    /**
     * @param secs Time in secs to convert
     *
     * @return Secs expressed in hours
     */
    public static double asHours(final double secs) {
        return secs / Duration.SECS_PER_HOUR;
    }

    /**
     * @param secs Time in secs to convert
     *
     * @return Secs expressed in days
     */
    public static double asRevs(final double secs) {
        return secs / Duration.SECS_PER_REV;
    }

    /**
     * @param secs Time in secs to convert
     *
     * @return Secs expressed in days
     */
    public static double asDays(final double secs) {
        return secs / Duration.SECS_PER_DAY;
    }

    /**
     * @param secs Time in secs to convert
     *
     * @return Secs expressed in JulianYears
     */
    public static double asJulianYears(final double secs) {
        return secs / Duration.SECS_PER_JULIAN_YEAR;
    }

    /**
     * @see Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asSecs();

        return this;
    }

    /**
     * @see Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_SEC);
    }

    /**
     * @see Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value;
    }

    /**
     * @see Duration#asMins()
     */
    @Override
    public double asMins() {
        return value / Duration.SECS_PER_MIN;
    }

    /**
     * @see Duration#asHours()
     */
    @Override
    public double asHours() {
        return value / Duration.SECS_PER_HOUR;
    }

    /**
     * @see Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return value / Duration.SECS_PER_REV;
    }

    /**
     * @see Duration#asDays()
     */
    @Override
    public double asDays() {
        return this.asSecs() / 86400.0D;
    }

    /**
     * @see Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value / Duration.SECS_PER_JULIAN_YEAR;
    }

    /**
     * @see Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asSecs();

        return this;
    }

    /**
     * @see Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asSecs();

        return this;
    }

    /**
     * @see ConcreteDuration#clone()
     */
    public Secs clone() {
        return new Secs(value);
    }

}
