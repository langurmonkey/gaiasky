/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

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