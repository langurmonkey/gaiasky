/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

import java.io.Serializable;

public class JulianYears extends ConcreteDuration implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public JulianYears() {
    }

    /**
     * Construct object
     *
     * @param years number of years
     */
    public JulianYears(final double years) {
        value = years;
    }

    /**
     * @param years Time in JulianYears to convert
     *
     * @return JulianYears expressed in nanoSec
     */
    public static long asNanoSecs(final double years) {
        return Math.round(years * Duration.NS_PER_JULIAN_YEAR);
    }

    /**
     * @param years Time in JulianYears to convert
     *
     * @return JulianYears expressed in secs
     */
    public static double asSecs(final double years) {
        return years * Duration.SECS_PER_JULIAN_YEAR;
    }

    /**
     * @param years JulianYears in years to convert
     *
     * @return JulianYears expressed in mins
     */
    public static double asMins(final double years) {
        return years * Duration.MINS_PER_JULIAN_YEAR;
    }

    /**
     * @param years Time in JulianYears to convert
     *
     * @return JulianYears expressed in hours.
     */
    public static double asHours(final double years) {
        return years * Duration.HOURS_PER_JULIAN_YEAR;
    }

    /**
     * @param years Time in JulianYears to convert
     *
     * @return JulianYears expressed in revs.
     */
    public static double asRevs(final double years) {
        return years * Duration.REVS_PER_JULIAN_YEAR;
    }

    /**
     * @param years Time in JulianYears ton convert
     *
     * @return JulianYears expressed in days
     */
    public static double asDays(final double years) {
        return years * Duration.DAYS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#set(Duration)
     */
    @Override
    public Duration set(final Duration d) {
        value = d.asJulianYears();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asNanoSecs()
     */
    @Override
    public long asNanoSecs() {
        return Math.round(value * Duration.NS_PER_JULIAN_YEAR);
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asSecs()
     */
    @Override
    public double asSecs() {
        return value * Duration.SECS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asMins()
     */
    @Override
    public double asMins() {
        return value * Duration.MINS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asHours()
     */
    @Override
    public double asHours() {
        return value * Duration.HOURS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asRevs()
     */
    @Override
    public double asRevs() {
        return value * Duration.REVS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asDays()
     */
    @Override
    public double asDays() {
        return value * Duration.DAYS_PER_JULIAN_YEAR;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#asJulianYears()
     */
    @Override
    public double asJulianYears() {
        return value;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration add(final Duration d) {
        value += d.asJulianYears();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.Duration#sub(Duration)
     */
    @Override
    public Duration sub(final Duration d) {
        value -= d.asJulianYears();

        return this;
    }

    /**
     * @see gaiasky.util.gaia.time.ConcreteDuration#clone()
     */
    public JulianYears clone() {
        return new JulianYears(value);
    }

}
