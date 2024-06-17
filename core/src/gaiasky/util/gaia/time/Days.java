/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

import net.jafama.FastMath;

public class Days extends ConcreteDuration {

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
        return FastMath.round(days * Duration.NS_PER_DAY);
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
        return FastMath.round(value * Duration.NS_PER_DAY);
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
