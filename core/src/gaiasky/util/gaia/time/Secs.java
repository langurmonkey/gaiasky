/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

public class Secs extends ConcreteDuration {

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
