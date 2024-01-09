/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

public class Hours extends ConcreteDuration {

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
