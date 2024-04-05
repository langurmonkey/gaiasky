/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.test;

import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathManager;
import gaiasky.util.math.Vector3d;

import java.time.Instant;

public class AstroUtilsTest {

    public static void check(Instant i, double resultJd, double expectedJd) {
        System.out.println(i + ": " + resultJd);
        if(Math.abs(resultJd - expectedJd) > 0.00001) {
            System.err.println(i + " - Error: " + resultJd + " != " + expectedJd);
        }
    }

    public static void julianDate(String dateUTC, double expected) {
        var instant = Instant.parse(dateUTC);
        double jd = AstroUtils.getJulianDate(instant);
        check(instant, jd, expected);
    }

    public static void main(String[] args) {
        MathManager.initialize(true);

        julianDate("1099-12-19T12:00:00.000Z", 2122820.0);
        julianDate("1993-01-01T00:00:00.00Z", 2448988.5);
        julianDate("2000-01-01T00:00:00.00Z", 2451544.5);
        julianDate("2010-01-01T00:00:00.00Z", 2455197.5);
        julianDate("2013-01-01T00:30:00.00Z", 2456293.520833);

    }
}
