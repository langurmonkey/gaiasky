/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

import java.io.Serializable;

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
