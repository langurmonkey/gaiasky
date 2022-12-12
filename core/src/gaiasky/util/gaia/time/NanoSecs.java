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
 * A finite number of nanoseconds.
 * <p>
 * There are two implementations provided of the conversions methods one as
 * object interface, where an object of the current class has to be
 * instantiated. The oder implementation is provided as static class methods.
 * <p>
 * Performance tests of both implementations have come up with a performance
 * improvement of 20% of the static methods compared with the object methods.
 *
 * @author ulammers
 * @version $Id: NanoSecs.java 405499 2014-12-18 20:21:02Z hsiddiqu $
 */
public class NanoSecs extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    private long ns;

    /**
     * Default constructor
     */
    public NanoSecs() {
    }

    /**
     * Construct object from number of nano seconds.
     *
     * @param ns [ns]
     */
    public NanoSecs(final long ns) {
        this.ns = ns;
    }

    /**
     * @param nanoSecs
     *
     * @return nanoSecs expressed in s
     */
    static public double asSecs(final long nanoSecs) {
        return (double) nanoSecs / (double) Duration.NS_PER_SEC;
    }

    /**
     * @param nanoSecs
     *
     * @return nanoSecs expressed in mins
     */
    static public double asMins(final long nanoSecs) {
        return (double) nanoSecs / Duration.NS_PER_MIN;
    }

    /**
     * @param nanoSecs
     *
     * @return nanoSecs expressed in hours
     */
    static public double asHours(final long nanoSecs) {
        return (double) nanoSecs / Duration.NS_PER_HOUR;
    }

    /**
     * @param nanoSecs
     *
     * @return nanoSecs expressed in revs
     */
    static public double asRevs(final long nanoSecs) {
        return (double) nanoSecs / Duration.NS_PER_REV;
    }

    /**
     * @param nanoSecs the time in nanoseconds to convert.
     *
     * @return nanoSecs expressed in days.
     */
    static public double asDays(final long nanoSecs) {
        return (double) nanoSecs / Duration.NS_PER_DAY;
    }

    /**
     * @param nanoSecs the time in nanoseconds to convert.
     *
     * @return nanoSecs expressed in years.
     */
    static public double asJulianYears(final long nanoSecs) {
        return (double) nanoSecs / Duration.NS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        this.ns = d.asNanoSecs();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return this.ns;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return (double) this.ns / (double) Duration.NS_PER_SEC;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asMins()
     */
    @Override
    public double asMins() {
        return (double) this.ns / Duration.NS_PER_MIN;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asHours()
     */
    @Override
    public double asHours() {
        return (double) this.ns / Duration.NS_PER_HOUR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return (double) this.ns / Duration.NS_PER_REV;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asDays()
     */
    @Override
    public double asDays() {
        return (double) this.ns / Duration.NS_PER_DAY;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return (double) this.ns / Duration.NS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#negate()
     */
    @Override
    public NanoSecs negate() {
        this.ns = -this.ns;

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#add(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        this.ns += d.asNanoSecs();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        this.ns -= d.asNanoSecs();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#mult(double)
     */
    @Override
    public Duration mult(double s) {
        ns = Math.round((double) ns * s);

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#clone()
     */
    public NanoSecs clone() {
        return new NanoSecs(ns);
    }

}
