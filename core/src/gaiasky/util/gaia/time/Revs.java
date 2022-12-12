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
 * A finite number of revolutions
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The other implementation is provided as static class methods.
 *
 * @author hsiddiqu
 * @version $Id: Hours.java 300167 2013-06-24 09:42:58Z ejoliet $
 */
public class Revs extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public Revs() {
    }

    /**
     * Construct object
     *
     * @param revs number of revs
     */
    public Revs(final double revs) {
        value = revs;
    }

    /**
     * @param revs The time in revs to convert.
     *
     * @return revs expressed in nanosecs
     */
    public static long asNanoSecs(final double revs) {
        return Math.round(revs * Duration.NS_PER_REV);
    }

    /**
     * @param revs The time in revs to convert.
     *
     * @return revs expressed in secs
     */
    public static double asSecs(final double revs) {
        return revs * Duration.SECS_PER_REV;
    }

    /**
     * @param revs The time in revs to convert
     *
     * @return revs expressed in mins
     */
    public static double asMins(final double revs) {
        return revs * Duration.MINS_PER_REV;
    }

    /**
     * @param revs Time in hours to convert
     *
     * @return hours expressed in revs.
     */
    public static double asHours(final double revs) {
        return revs * Duration.HOURS_PER_REV;
    }

    /**
     * @param revs Time in revs to convert.
     *
     * @return revs expressed in days
     */
    public static double asDays(final double revs) {
        return revs / Duration.REVS_PER_DAY;
    }

    /**
     * @param revs Time in revolutions to convert.
     *
     * @return revs expressed in JulianYears
     */
    public static double asJulianYears(final double revs) {
        return revs / Duration.REVS_PER_JULIAN_YEAR;
    }

    /**
     * @see Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asRevs();

        return this;
    }

    /**
     * @see Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_REV);
    }

    /**
     * @see Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value * Duration.SECS_PER_REV;
    }

    /**
     * @see Duration#asMins()
     */
    @Override
    public double asMins() {
        return value * Duration.MINS_PER_REV;
    }

    /**
     * @see Duration#asHours()
     */
    @Override
    public double asHours() {
        return value * Duration.HOURS_PER_REV;
    }

    /**
     * @see Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return value;
    }

    /**
     * @see Duration#asDays()
     */
    @Override
    public double asDays() {
        return value / Duration.REVS_PER_DAY;
    }

    /**
     * @see Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value / Duration.REVS_PER_JULIAN_YEAR;
    }

    /**
     * @see Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asRevs();

        return this;
    }

    /**
     * @see Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asRevs();

        return this;
    }

    /**
     * @see ConcreteDuration#clone()
     */
    public Revs clone() {
        return new Revs(value);
    }

}
