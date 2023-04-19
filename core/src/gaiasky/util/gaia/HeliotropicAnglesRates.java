/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

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