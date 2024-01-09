/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

public class Mins extends ConcreteDuration {

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
