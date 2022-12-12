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
 * A compound object for holding equatorial angles and their rates as per
 * Fig. 4 in GAIA-C3-TN-LU-085.
 *
 * @author Uwe Lammers, Lennart Lindegren
 * @version $Id: EquatorialAnglesRates.java 308373 2013-08-02 16:01:12Z ulammers $
 */
public class EquatorialAnglesRates extends AbstractAttitudeAnglesRates {
    /**
     * Get the right ascension of the SRS z axis - this is the first equatorial
     * attitude angle
     *
     * @return right ascension of the z axis [rad]
     */
    public double getAlphaZ() {
        return anglesRates[0][0];
    }

    /**
     * Get the time derivative of the right ascension of the SRS z axis
     *
     * @return time derivative of the right ascension of the z axis [rad/day]
     */
    public double getAlphaZDot() {
        return anglesRates[0][1];
    }

    /**
     * Get the declination of the SRS z axis - this is the second equatorial
     * attitude angle
     *
     * @return declination of the z axis [rad]
     */
    public double getDeltaZ() {
        return anglesRates[1][0];
    }

    /**
     * Get the time derivative of the declination of the SRS z axis
     *
     * @return time derivative of the declination of the z axis [rad/day]
     */
    public double getDeltaZDot() {
        return anglesRates[1][1];
    }

    /**
     * Get the equatorial spin phase angle, psi (from the ascending node on the
     * equator to the SRS x axis) - this is the third equatorial attitude angle
     *
     * @return equatorial spin phase [rad]
     */
    public double getPsi() {
        return anglesRates[2][0];
    }

    /**
     * Get time derivative psiDot of the the equatorial spin phase angle
     *
     * @return time derivative of the equatorial spin phase [rad/day]
     */
    public double getPsiDot() {
        return anglesRates[2][1];
    }
}