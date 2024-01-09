/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.time;

import gaiasky.util.coord.AstroUtils;

import java.io.Serializable;

public interface Duration {
    /**
     * A few obvious constants
     */
    long NS_PER_SEC = 1000000000L;
    double SECS_PER_MIN = 60.;
    double MINS_PER_HOUR = 60.;
    double HOURS_PER_DAY = 24.;
    double DAYS_PER_JULIAN_YEAR = AstroUtils.JD_TO_Y;
    double HOURS_PER_REV = 6;
    double REVS_PER_DAY = 4;

    /* _PER_JULIAN_YEAR */ double REVS_PER_JULIAN_YEAR = DAYS_PER_JULIAN_YEAR * REVS_PER_DAY;
    double HOURS_PER_JULIAN_YEAR = DAYS_PER_JULIAN_YEAR * HOURS_PER_DAY;
    double MINS_PER_JULIAN_YEAR = HOURS_PER_JULIAN_YEAR * MINS_PER_HOUR;
    double SECS_PER_JULIAN_YEAR = MINS_PER_JULIAN_YEAR * SECS_PER_MIN;
    double NS_PER_JULIAN_YEAR = SECS_PER_JULIAN_YEAR * (double) NS_PER_SEC;
    long NS_PER_JULIAN_YEAR_L = (long) NS_PER_JULIAN_YEAR;

    /* _PER_DAY */ double MINS_PER_DAY = HOURS_PER_DAY * MINS_PER_HOUR;
    double SECS_PER_DAY = MINS_PER_DAY * SECS_PER_MIN;
    double NS_PER_DAY = SECS_PER_DAY * (double) NS_PER_SEC;
    long NS_PER_DAY_L = (long) NS_PER_DAY;

    /* _PER_REV */ double MINS_PER_REV = HOURS_PER_REV * MINS_PER_HOUR;
    double SECS_PER_REV = MINS_PER_REV * SECS_PER_MIN;
    double NS_PER_REV = SECS_PER_REV * (double) NS_PER_SEC;
    long NS_PER_REV_L = (long) NS_PER_REV;

    /* _PER_HOUR */ double SECS_PER_HOUR = MINS_PER_HOUR * SECS_PER_MIN;
    double NS_PER_HOUR = SECS_PER_HOUR * (double) NS_PER_SEC;
    long NS_PER_HOUR_L = (long) NS_PER_HOUR;

    /* _PER_MIN */ double NS_PER_MIN = SECS_PER_MIN * (double) NS_PER_SEC;
    long NS_PER_MIN_L = (long) NS_PER_MIN;

    /**
     * Set this duration to a new given one
     *
     * @param d duration to set this one to
     *
     * @return updated object
     */
    Duration set(final Duration d);

    /**
     * @return duration expressed in ns
     */
    long asNanoSecs();

    /**
     * @return duration expressed in s
     */
    double asSecs();

    /**
     * @return duration expressed in min
     */
    double asMins();

    /**
     * @return duration expressed in h
     */
    double asHours();

    /**
     * @return number of ns expressed days
     */
    double asDays();

    /**
     * @return duration expressed in Julian years
     */
    double asJulianYears();

    /**
     * @return duration expressed in revolutions
     */
    double asRevs();

    /**
     * @return negated amount of time
     */
    Duration negate();

    /**
     * Add a duration to this one
     *
     * @param d amount of time to add
     *
     * @return updated object
     */
    Duration add(final Duration d);

    /**
     * Subtract a duration from this one
     *
     * @param d amount of time to subtract
     *
     * @return updated object
     */
    Duration sub(final Duration d);

    /**
     * Check that this duration is longer than a given one
     *
     * @param d duration to compare to
     *
     * @return {@code true} if this duration is longer than {@code d}
     */
    boolean isLongerThan(Duration d);

    /**
     * Multiply this duration by a given factor
     *
     * @param s scale factor
     *
     * @return updated object
     */
    Duration mult(final double s);

    /**
     * @return Current time scale of the duration
     */
    TimeScale getScale();

    /**
     * Set the time scale for this duration
     *
     * @param scale time scale to set the duration to
     */
    void setScale(TimeScale scale);
}
