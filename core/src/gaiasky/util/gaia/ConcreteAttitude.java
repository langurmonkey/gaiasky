/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

public class ConcreteAttitude implements IAttitude {

    private static final double BASICANGLE_DEGREE = 106.5;
    private static final Vector3D[] xyz = new Vector3D[] { new Vector3D(), new Vector3D(), new Vector3D() };
    private static final Vector3D[] fovDirections = new Vector3D[] { new Vector3D(), new Vector3D() };
    private static final Vector3D aux = new Vector3D();
    // half the conventional basic angle Gamma [rad]
    private final double halfGamma = FastMath.toRadians(.5 * BASICANGLE_DEGREE);
    /**
     * time to which the attitude refers, in elapsed ns since the reference epoch
     */
    private long t;
    /**
     * attitude quaternion at time t
     */
    private QuaternionDouble q;
    /**
     * time derivative of the attitude at time t
     */
    private QuaternionDouble qDot;

    /**
     * Construct object from time, and a quaternion. This leaves the time
     * derivative undefined. It can be set later with
     * {@link #setQuaternionDot(QuaternionDouble)}
     *
     * @param t time of the attitude
     * @param q quaternion
     */
    public ConcreteAttitude(long t, QuaternionDouble q, boolean withZeroSigmaCorr) {
        this(t, q, null, withZeroSigmaCorr);
    }

    /**
     * Construct object from time, quaternion and its derivative.
     *
     * @param t    time of the attitude
     * @param q    quaternion
     * @param qDot time derivative of quaternion [1/day]
     */
    public ConcreteAttitude(long t, QuaternionDouble q, QuaternionDouble qDot,
            boolean withZeroSigmaCorr) { //-V6022
        this.t = t;
        this.q = q;
        this.qDot = qDot;
    }

    /**
     *
     */
    @Override
    public long getTime() {
        return t;
    }

    /**
     * Set the time of the attitude. This usually does not make sense as the
     * time is set during construction of the object
     *
     * @param time time of the attitude in [ns] since reference epoch
     */
    public void setTime(long time) {
        t = time;
    }

    /**
     *
     */
    @Override
    public QuaternionDouble getQuaternion() {
        return q;
    }

    /**
     * The quaternion of the attitude. * Set the time of the attitude. This
     * usually does not make sense as the time is set during construction of the
     */
    public void setQuaternion(QuaternionDouble q) {
        this.q = q;
    }

    /**
     * Get the time derivative of the attitude.
     *
     * @return time derivative of the attitude quaternion [1/day]
     */
    @Override
    public QuaternionDouble getQuaternionDot() {
        return qDot;
    }

    /**
     * @param qDot quaternion derivative to set - all components in [1/day]
     */
    public void setQuaternionDot(QuaternionDouble qDot) {
        this.qDot = qDot;
    }

    /**
     *
     */
    @Override
    public Vector3D getSpinVectorInSrs() {
        // Using (A.18) in AGIS paper (A&A 538, A78, 2012):
        QuaternionDouble tmp = q.cpy();
        tmp.inverse().mul(qDot);
        return new Vector3D(2. * tmp.x, 2. * tmp.y, 2. * tmp.z);
    }

    /**
     *
     */
    @Override
    public Vector3D getSpinVectorInIcrs() {
        // Using (A.17) in AGIS paper (A&A 538, A78, 2012):
        QuaternionDouble tmp = qDot.cpy();
        tmp.mulInverse(q);
        return new Vector3D(2. * tmp.x, 2. * tmp.y, 2. * tmp.z);
    }

    /**
     *
     */
    @Override
    public Vector3D[] getFovDirections() {
        // half the nominal basic angle:
        double halfBasicAngle = 0.5 * FastMath.toRadians(BASICANGLE_DEGREE);

        // xyz[0], xyz[1], xyz[2] are unit vectors (in ICRS) along the SRS axes:
        getSrsAxes(xyz);
        Vector3D xScaled = xyz[0].scl(Math.cos(halfBasicAngle));
        Vector3D yScaled = xyz[1].scl(Math.sin(halfBasicAngle));

        // PFoV = x * cos(halfBasicAngle) + y * sin(halfBasicAngle):
        fovDirections[0].set(xScaled).add(yScaled); // .set(xScaled).add(yScaled);

        // FFoV = x * cos(halfBasicAngle) - y * sin(halfBasicAngle):
        fovDirections[1].set(xScaled).sub(yScaled);

        return fovDirections;
    }

    /**
     * @see IAttitude#getSrsAxes(Vector3D[])
     */
    @Override
    public Vector3D[] getSrsAxes(Vector3D[] xyz) {
        // computed from q using vector rotation on three unit vectors
        xyz[0].set(1, 0, 0).rotateVectorByQuaternion(q);
        xyz[1].set(0, 1, 0).rotateVectorByQuaternion(q);
        xyz[2].set(0, 0, 1).rotateVectorByQuaternion(q);

        return xyz;
    }

    /**
     *
     */
    @Override
    public double[] getAlAcRates(double alInstrumentAngle, double acFieldAngle) {
        // Formulas (11) and (12) from GAIA-LL-056 : valid for any scanning law
        double cphi = FastMath.cos(alInstrumentAngle);
        double sphi = FastMath.sin(alInstrumentAngle);
        double tzeta = FastMath.tan(acFieldAngle);

        // The inertial rate in SRS in [rad/s]:
        Vector3D spinRate = getSpinVectorInSrs().scl(86400.);
        // Along scan speed in rad/s
        double phip = -spinRate.z
                + (spinRate.x * cphi + spinRate.y * sphi) * tzeta;
        // Across scan speed in rad/s
        double zetap = -spinRate.x * sphi + spinRate.y * cphi;

        return new double[] { phip, zetap };
    }

    /**
     *
     */
    @Override
    public double[] getAlAcRates(FOV fov, double alFieldAngle,
            double acFieldAngle) {

        return getAlAcRates(alFieldAngle + fov.getNumericalFieldIndex()
                * halfGamma, acFieldAngle);
    }
}
