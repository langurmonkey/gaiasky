/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.desktop.util;

import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;

import java.util.Scanner;

public class EqGalTest {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter right ascension [deg]: ");
        double ra = readFloat(sc);
        System.out.print("Enter declination [deg]: ");
        double dec = readFloat(sc);
        double dist = 10;
        Vector3d pos = Coordinates.sphericalToCartesian(Math.toRadians(ra), Math.toRadians(dec), dist, new Vector3d());

        Vector3d posGal = new Vector3d(pos);
        posGal.mul(Coordinates.eqToGal());
        Vector3d posGalSph = Coordinates.cartesianToSpherical(posGal, new Vector3d());
        double l = posGalSph.x * MathUtilsDouble.radiansToDegrees;
        double b = posGalSph.y * MathUtilsDouble.radiansToDegrees;

        System.out.println("Galactic coordinates - l: " + l + ", b: " + b);
    }

    private static float readFloat(Scanner sc) {
        try {
            return sc.nextFloat();
        } catch (Exception e) {
            System.err.println("Input is not a valid float");
            System.exit(1);
        }
        return 0;
    }
}
