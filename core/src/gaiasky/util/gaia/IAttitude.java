/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3D;

public interface IAttitude {
    /**
     * Get the time that this attitude is valid for as a single long value. The
     * meaning of the time depends on the TimeContext of the AttitudeDataServer
     * that generated the attitude. Use #getGaiaTime() to get the time as an
     * absolute GaiaTime if needed.
     *
     * @return time that the attitude is valid for
     */
    long getTime();

    /**
     * @return quaternion that represents the attitude
     */
    QuaternionDouble getQuaternion();

    /**
     * @return time derivative [1/day] of the quaternion returned by
     * {@link #getQuaternion()}
     */
    QuaternionDouble getQuaternionDot();

    /**
     * Get the inertial spin vector in the SRS.
     *
     * @return spin vector in [rad/day] relative to SRS
     */
    Vector3D getSpinVectorInSrs();

    /**
     * Get the inertial spin vector in the ICRS (or CoMRS).
     *
     * @return spin vector in [rad/day] relative to ICRS
     */
    Vector3D getSpinVectorInIcrs();

    /**
     * Get the PFoV and FFoV directions as an array of unit vectors expressed in
     * the ICRS (or CoMRS).
     *
     * @return array of two (PFoV, FFoV3) vectors
     */
    Vector3D[] getFovDirections();

    /**
     * Get the x, y, z axes of the SRS as an array of three unit vectors
     * expressed in the ICRS (or CoMRS).
     *
     * @return array of three (x, y, z) vectors
     */
    Vector3D[] getSrsAxes(Vector3D[] xyz);

    /**
     * Compute the angular speed AL and AC of an inertial direction in the SRS
     * frame, using instrument angles (phi, zeta).
     *
     * @param alInstrumentAngle (=AL angle phi) of the direction [rad]
     * @param acFieldAngle      (=AC angle zeta) of the direction [rad]
     *
     * @return two-element double array containing the angular speed AL and AC
     * [rad/s]
     */
    double[] getAlAcRates(double alInstrumentAngle, double acFieldAngle);

    /**
     * Compute the angular speed AL and AC of an inertial direction in the SRS
     * frame, using field angles (fov, eta, zeta).
     *
     * @param fov          FOV (Preceding or Following)
     * @param alFieldAngle (=AL angle eta) of the direction [rad]
     * @param acFieldAngle (=AC angle zeta) of the direction [rad]
     *
     * @return two-element double array containing the angular speed AL and AC
     * [rad/s]
     */
    double[] getAlAcRates(FOV fov, double alFieldAngle, double acFieldAngle);
}
