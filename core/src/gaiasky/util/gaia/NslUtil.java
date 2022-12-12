/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * GaiaTools
 * Copyright (C) 2006 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaiasky.util.gaia;

/**
 * Class with various static methods for analytical scanning laws.
 *
 * @author lennartlindegren
 * @version $Id$
 */
public class NslUtil {

    /**
     * Calculates the nominal speed of the z axis in solar motion units, as
     * function of the precession rate precRate [rev/yr] and the solar aspect
     * angle xi [rad].
     * <p>
     * This method implements Eq. (26) in FM-037-2, accurate to O(1/K^12).
     *
     * @param xi       solar aspect angle [rad]
     * @param precRate precession rate [rev/yr]
     *
     * @return nominal speed (S)
     */
    public static double calcSNom(double xi, double precRate) {

        double sx = Math.sin(xi);
        double cx = Math.cos(xi);
        double c2 = cx * cx;
        double ks = precRate * sx;
        double f = 1 / (4 * ks * ks);
        double t2 = 1 + 2 * c2;
        double t4 = 1 + c2 * (-20 + c2 * (-8));
        double t6 = 1 + c2 * (-18 + c2 * (-88 + c2 * (-16)));
        double t8 = 7 + c2 * (8 + c2 * (4208 + c2 * (5952 + c2 * (640))));
        double t10 = 11
                + c2
                * (-130 + c2
                * (6000 + c2 * (35168 + c2 * (24704 + c2 * (1792)))));
        return ks
                * (1 + f
                * (t2 + f
                * (t4 / 4 + f
                * (-t6 / 4 + f
                * (-t8 / 64 + f * t10 / 64)))));
    }

}
