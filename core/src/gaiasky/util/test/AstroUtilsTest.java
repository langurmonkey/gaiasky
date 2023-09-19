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

public class AstroUtilsTest {
    public static void main(String[] args) {
        MathManager.initialize(true);
        Vector3d coord = new Vector3d();
        AstroUtils.moonEclipticCoordinates(2448724.5, coord);

        System.out.println("lambda[deg] : " + Math.toDegrees(coord.x));
        System.out.println("beta[deg]   : " + Math.toDegrees(coord.y));
        System.out.println("dist[km]    : " + coord.z);

        System.out.println("J2010: " + AstroUtils.JD_J2010);

        double jd = AstroUtils.getJulianDate(2000, 1, 1, 12, 0, 0, 0, true);
        System.out.println("2000-01-01.5 JD: " + jd);
    }
}
