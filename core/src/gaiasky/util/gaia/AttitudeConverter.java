/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.NslSun;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

public class AttitudeConverter {
    /** Mathematical constants **/
    static final double PI = FastMath.PI;
    static final double PI_HALF = 0.5 * PI;

    /** Unit vectors **/
    static final Vector3D X_AXIS = Vector3D.getUnitX();
    static final Vector3D Y_AXIS = Vector3D.getUnitY();
    static final Vector3D Z_AXIS = Vector3D.getUnitZ();

    static final Vector3D aux1 = new Vector3D();
    static final Vector3D aux2 = new Vector3D();
    static final Vector3D aux3 = new Vector3D();

    /** The obliquity of the ecliptic in radians **/
    static final double OBLIQUITY = Coordinates.OBLIQUITY_RAD_J2000;
    static final double OBLIQUITY_DEG = Coordinates.OBLIQUITY_DEG_J2000;
    static final double sinObliquity = FastMath.sin(OBLIQUITY);
    static final double cosObliquity = FastMath.cos(OBLIQUITY);

    static final Vector3D[] xyz = new Vector3D[] { new Vector3D(), new Vector3D(), new Vector3D() };

    /**
     * Converts heliotropic angles and rates to an attitude quaternion and its
     * derivative
     *
     * @param lSun     longitude of the nominal sun [rad]
     * @param xi       solar aspect angle [rad]
     * @param nu       revolving phase angle [rad]
     * @param omega    scan phase angle [rad]
     * @param lSunDot  time derivative of lSun [rad/day]
     * @param nuDot    time derivative of nu [rad/day]
     * @param omegaDot time derivative of omega [rad/day]
     *
     * @return an array of two quaternions, q (the attitude quaternion) and qDot
     * (the time derivative of q, per day)
     */
    public static QuaternionDouble[] heliotropicToQuaternions(double lSun, double xi,
            double nu, double omega, double lSunDot, double nuDot,
            double omegaDot) {

        /*
         * SOME AXES NEED TO BE SWAPPED TO ALIGN WITH OUR REF SYS:
         * 	GLOBAL -> GAIA SKY
         * 	Z -> Y
         * 	X -> Z
         * 	Y -> X
         */

        /* Calculate the attitude quaternion */
        QuaternionDouble q = new QuaternionDouble(Z_AXIS, OBLIQUITY_DEG);
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(lSun)));
        q.mul(new QuaternionDouble(Z_AXIS, FastMath.toDegrees(nu - PI_HALF)));
        q.mul(new QuaternionDouble(X_AXIS, FastMath.toDegrees(PI_HALF - xi)));
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(omega)));

        /*
         * Calculate the time derivative of the attitude quaternion using (A.17)
         * in AGIS paper, based on the rates in the ICRS:
         */
        double sinLSun = FastMath.sin(lSun);
        double cosLSun = FastMath.cos(lSun);
        Vector3D zInSrs = aux1;
        zInSrs.set(Y_AXIS).rotateVectorByQuaternion(q);
        double rateX = nuDot * cosLSun + omegaDot * zInSrs.x;
        double rateY = -lSunDot * sinObliquity + nuDot * sinLSun * cosObliquity
                + omegaDot * zInSrs.y;
        double rateZ = lSunDot * cosObliquity + nuDot * sinLSun * sinObliquity
                + omegaDot * zInSrs.z;
        QuaternionDouble halfSpinInIcrs = new QuaternionDouble(0.5 * rateZ, 0.5 * rateX,
                0.5 * rateY, 0.0);
        QuaternionDouble qDot = halfSpinInIcrs.mul(q);

        return new QuaternionDouble[] { q, qDot };
    }

    /**
     * Converts heliotropic angles and rates to the attitude quaternion
     * components and the inertial rates in SRS
     *
     * @param lSun     longitude of the nominal sun [rad]
     * @param xi       solar aspect angle [rad]
     * @param nu       revolving phase angle [rad]
     * @param omega    scan phase angle [rad]
     * @param lSunDot  time derivative of lSun [rad/day]
     * @param nuDot    time derivative of nu [rad/day]
     * @param omegaDot time derivative of omega [rad/day]
     *
     * @return double[] array {qx, qy, qz, qw, rateX, rateY, rateZ} with rates in [rad/day]
     */
    public static double[] heliotropicToQuaternionSrsRates(double lSun, double xi,
            double nu, double omega, double lSunDot, double nuDot,
            double omegaDot) {

        /* SOME AXES NEED TO BE SWAPPED TO ALIGN WITH OUR REF SYS:
         * 	GLOBAL	GAIASANDBOX
         * 	Z -> Y
         * 	X -> Z
         * 	Y -> X
         */

        /* Calculate the attitude quaternion */
        QuaternionDouble q = new QuaternionDouble(Z_AXIS, OBLIQUITY_DEG);
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(lSun)));
        q.mul(new QuaternionDouble(Z_AXIS, FastMath.toDegrees(nu - PI_HALF)));
        q.mul(new QuaternionDouble(X_AXIS, FastMath.toDegrees(PI_HALF - xi)));
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(omega)));

        /*
         * Calculate the inertial rate in SRS by adding the rotations around
         * k (ecliptic pole), s (solar direction), and z:
         */
        Vector3D k = new Vector3D(0, -sinObliquity, cosObliquity);
        k.mul(q);
        double sinLSun = FastMath.sin(lSun);
        double cosLSun = FastMath.cos(lSun);
        Vector3D sun = new Vector3D(cosLSun, cosObliquity * sinLSun, sinObliquity * sinLSun);
        sun.mul(q);
        double rateX = k.x * lSunDot + sun.x * nuDot;
        double rateY = k.y * lSunDot + sun.y * nuDot;
        double rateZ = k.z * lSunDot + sun.z * nuDot + omegaDot;

        return new double[] { q.z, q.x, q.y, q.w, rateZ, rateX, rateY };
    }

    /**
     * Converts heliotropic angles and rates to an attitude quaternion and its
     * derivative
     *
     * @param gt GaiaTime
     * @param h  heliotropic angles and their rates in [rad] and [rad/day]
     *
     * @return an array of two quaternions, q (the attitude quaternion) and qDot
     * (the time derivative of q, per day)
     */
    public static QuaternionDouble[] getQuaternionAndRate(long gt,
            HeliotropicAnglesRates h) {

        /* SOME AXES NEED TO BE SWAPPED TO ALIGN WITH OUR REF SYS:
         * 	GLOBAL	GAIASANDBOX
         * 	Z -> Y
         * 	X -> Z
         * 	Y -> X
         */

        NslSun sun = new NslSun();
        sun.setTime(gt);
        double lSun = sun.getSolarLongitude();
        double lSunDot = sun.getSolarLongitudeDot();

        /* Calculate the attitude quaternion */
        QuaternionDouble q = new QuaternionDouble(Z_AXIS, OBLIQUITY_DEG);
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(lSun)));
        q.mul(new QuaternionDouble(Z_AXIS, FastMath.toDegrees(h.getNu() - PI_HALF)));
        q.mul(new QuaternionDouble(X_AXIS, FastMath.toDegrees(PI_HALF - h.getXi())));
        q.mul(new QuaternionDouble(Y_AXIS, FastMath.toDegrees(h.getOmega())));

        /*
         * Calculate the time derivative of the attitude quaternion using (A.17)
         * in AGIS paper, based on the rates in the ICRS:
         */
        double sinLSun = FastMath.sin(lSun);
        double cosLSun = FastMath.cos(lSun);
        Vector3D zInSrs = aux1;
        zInSrs.set(Y_AXIS).mul(q);
        Vector3D sz = aux2;
        sz.set(sun.getSolarDirection(aux3)).crs(zInSrs).nor();
        double rateX = h.getNuDot() * cosLSun + h.getOmegaDot() * zInSrs.x
                + h.getXiDot() * sz.x;
        double rateY = -lSunDot * sinObliquity + h.getNuDot() * sinLSun
                * cosObliquity + h.getOmegaDot() * zInSrs.y + h.getXiDot()
                * sz.y;
        double rateZ = lSunDot * cosObliquity + h.getNuDot() * sinLSun
                * sinObliquity + h.getOmegaDot() * zInSrs.z + h.getXiDot()
                * sz.z;
        QuaternionDouble halfSpinInIcrs = new QuaternionDouble(0.5 * rateZ, 0.5 * rateX,
                0.5 * rateY, 0.0);
        QuaternionDouble qDot = halfSpinInIcrs.mul(q);

        return new QuaternionDouble[] { q, qDot };
    }

    /**
     * Calculate the heliotropic angles and rates for a given attitude
     *
     * @param gt  Time for the attitude
     * @param att attitude
     */
    public static HeliotropicAnglesRates getHeliotropicAnglesRates(long gt,
            IAttitude att) {
        HeliotropicAnglesRates anglesAndRates = new HeliotropicAnglesRates();

        // k is a unit vector (in ICRS) towards the north ecliptic pole:
        Vector3D k = new Vector3D(0.0, -sinObliquity, cosObliquity);

        // s is a unit vector (in ICRS) towards the nominal sun:
        NslSun sun = new NslSun();
        sun.setTime(gt);
        double cosLSun = FastMath.cos(sun.getSolarLongitude());
        double sinLSun = FastMath.sin(sun.getSolarLongitude());
        Vector3D s = new Vector3D(cosLSun, sinLSun * cosObliquity, sinLSun
                * sinObliquity);

        // xyz[0], xyz[1], xyz[2] are unit vectors (in ICRS) along the SRS axes:
        att.getSrsAxes(xyz);

        // m = s x z is a non-unit vector (of length sinXi) normal to the plane
        // containing s and z:
        Vector3D m = new Vector3D(s);
        m.crs(xyz[2]);

        // compute solar aspect angle xi in range [0, pi]:
        double sinXi = m.len();
        double cosXi = s.dot(xyz[2]);
        anglesAndRates.setFirstAngle(Math.atan2(sinXi, cosXi));

        // NOTE: all subsequent computations fail if sinXi = 0

        // compute revolving phase angle nu in range [-pi, pi]:
        double sinXiCosNu = k.dot(m);
        double sinXiSinNu = k.dot(xyz[2]);
        anglesAndRates.setSecondAngle(Math.atan2(sinXiSinNu, sinXiCosNu));

        // compute spin phase Omega:
        double sinXiCosOmega = -m.dot(xyz[1]);
        double sinXiSinOmega = -m.dot(xyz[0]);
        anglesAndRates.setThirdAngle(Math.atan2(sinXiSinOmega, sinXiCosOmega));

        // inertial spin rate in ICRS:
        Vector3D spin = att.getSpinVectorInIcrs();

        // subtract motion of the nominal sun to get heliotropic spin rate:
        Vector3D spinHel = new Vector3D(spin);
        spinHel.add(k.scl(-sun.getSolarLongitudeDot()));

        // scalar products with s, z, and m are used to determine the angular
        // rates:
        double sSpinHel = s.dot(spinHel);
        double zSpinHel = xyz[2].dot(spinHel);
        double mSpinHel = m.dot(spinHel);
        // d(xi)/dt:
        anglesAndRates.setFirstRate(mSpinHel / sinXi);
        // d(nu)/dt:
        anglesAndRates.setSecondRate((sSpinHel - zSpinHel * cosXi)
                / (sinXi * sinXi));
        // d(Omega)/dt:
        anglesAndRates.setThirdRate((zSpinHel - sSpinHel * cosXi)
                / (sinXi * sinXi));

        return anglesAndRates;
    }

}
