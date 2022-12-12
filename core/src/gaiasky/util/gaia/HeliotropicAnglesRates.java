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
 * A compound object for holding heliotropic angles and their rates as per
 * Fig. 3 in GAIA-C3-TN-LU-085.
 *
 * @author Uwe Lammers, Lennart Lindegren
 * @version $Id: HeliotropicAnglesRates.java 308373 2013-08-02 16:01:12Z ulammers $
 */
public class HeliotropicAnglesRates extends AbstractAttitudeAnglesRates {
    /**
     * Get the solar aspect angle (between the nominal sun and the SRS z axis) -
     * this is the first heliotropic attitude angle
     *
     * @return solar aspect angle [rad]
     */
    public double getXi() {
        return anglesRates[0][0];
    }

    /**
     * Get the time derivative of the solar aspect angle
     *
     * @return time derivative of the solar aspect angle [rad/day]
     */
    public double getXiDot() {
        return anglesRates[0][1];
    }

    /**
     * Get the revolving phase angle - this is the second heliotropic attitude
     * angle
     *
     * @return revolving phase [rad]
     */
    public double getNu() {
        return anglesRates[1][0];
    }

    /**
     * Get the time derivative of the revolving phase angle
     *
     * @return time derivative of the revolving phase [rad/day]
     */
    public double getNuDot() {
        return anglesRates[1][1];
    }

    /**
     * Get the spin phase angle - this is the third heliotropic attitude angle
     *
     * @return spin phase [rad]
     */
    public double getOmega() {
        return anglesRates[2][0];
    }

    /**
     * Get the time derivative of the spin phase angle
     *
     * @return time derivative of the spin phase [rad/day]
     */
    public double getOmegaDot() {
        return anglesRates[2][1];
    }
}