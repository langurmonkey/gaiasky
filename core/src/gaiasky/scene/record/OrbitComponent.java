/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix3D;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.Objects;

import static gaiasky.util.math.MathUtilsDouble.PI2;

public class OrbitComponent {
    protected static final Logger.Log logger = Logger.getLogger(OrbitComponent.class);

    /** Source file **/
    public String source;
    /** Orbital period in days **/
    public double period;
    /** Base epoch in julian days **/
    public double epoch;
    /** Semi major axis of the ellipse, in Km. **/
    public double semiMajorAxis;
    /** Eccentricity of the ellipse, in degrees. **/
    public double e;
    /** Inclination, angle between the reference plane and the orbital plane, in degrees. **/
    public double i;
    /** Longitude of the ascending node in degrees. **/
    public double ascendingNode;
    /** Argument of perihelion in degrees. **/
    public double argOfPericenter;
    /** Mean anomaly at epoch, in degrees. **/
    public double meanAnomaly;
    /** G*M of central body (gravitational constant). Defaults to the Sun's. In km^3/s^2. **/
    public double mu = 1.32712440041e11;
    /** Flag when mu is set externally. **/
    private boolean externalMu = false;

    /** Auxiliary rotation matrix. **/
    private final Matrix3D Rx = new Matrix3D();
    /** Auxiliary vector. **/
    private final Vector3D vAux = new Vector3D();

    public OrbitComponent() {

    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setPeriod(Double period) {
        this.period = period;
    }

    public void setEpoch(Double epoch) {
        this.epoch = epoch;
    }

    public void setSemiMajorAxis(Double semiMajorAxis) {
        this.semiMajorAxis = semiMajorAxis;
    }

    public void setSemimajoraxis(Double setmiMajorAxis) {
        setSemiMajorAxis(setmiMajorAxis);
    }

    public void setEccentricity(Double e) {
        this.e = e;
    }

    public void setInclination(Double i) {
        this.i = i;
    }

    public void setAscendingNode(Double ascendingNode) {
        this.ascendingNode = ascendingNode;
    }

    public void setAscendingnode(Double ascendingNode) {
        setAscendingNode(ascendingNode);
    }

    public void setArgOfPericenter(Double argOfPericenter) {
        this.argOfPericenter = argOfPericenter;
    }

    public void setArgofpericenter(Double argOfPericenter) {
        setArgOfPericenter(argOfPericenter);
    }

    public void setMeanAnomaly(Double meanAnomaly) {
        this.meanAnomaly = meanAnomaly;
    }

    public void setMeananomaly(Double meanAnomaly) {
        setMeanAnomaly(meanAnomaly);
    }

    /**
     * This method automatically computes the standard gravitational
     * parameter (mu) of the orbit if the period and the semi-major axis
     * are set as mu=4 pi^2 a^3 / T^2.
     */
    public void computeMu(boolean km3s2) {
        if (period > 0 && semiMajorAxis > 0 && !externalMu) {
            double a = semiMajorAxis; // In km.
            // Compute mu from period and semi-major axis.
            double T = period * Nature.D_TO_S; // d to s.
            this.mu = (4.0 * FastMath.PI * FastMath.PI * a * a * a) / (T * T);
            if (!km3s2) {
                this.mu *= 1.0e9;
            }
        }
    }

    public void setMu(Double mu) {
        this.mu = mu;
        this.externalMu = true;
    }

    /**
     * Load the point (cartesian position vector) in the orbit for the given time.
     *
     * @param out The vector to store the result.
     * @param t   The time as a {@link Instant}.
     */
    public void loadDataPoint(Vector3D out, Instant t) {
        double tjd = AstroUtils.getJulianDate(t);
        keplerianToCartesianTime(out, tjd - epoch);
    }

    /**
     * Loads the orbit cartesian position vector for the given time.
     *
     * @param out    The vector to store the result.
     * @param dtDays The time as Julian days from epoch.
     */
    public void loadDataPoint(Vector3D out, double dtDays) {
        keplerianToCartesianTime(out, dtDays);
    }

    /**
     * Gets the mean anomaly at the given delta time from epoch.
     *
     * @param dtDays The delta time in days.
     *
     * @return The mean anomaly in radians.
     */
    public double getMeanAnomalyAt(double dtDays) {
        // Mean orbital motion [rad/day]
        double n = PI2 / period;
        // Mean anomaly at epoch
        double M0 = FastMath.toRadians(meanAnomaly);
        // Mean anomaly at epoch + dt
        double M = (M0 + n * dtDays) % PI2;
        if (M < 0) M += PI2;
        return M;
    }

    /**
     * Gets the true anomaly for the given solution to Kepler's equation.
     *
     * @param E Solution to Kepler's equation.
     *
     * @return The true anomaly.
     */
    public double getTrueAnomaly(double E) {
        return 2.0 * FastMath.atan2(FastMath.sqrt(1.0 + e) * FastMath.sin(E / 2.0),
                FastMath.sqrt(1.0 - e) * FastMath.cos(E / 2.0));
    }

    /**
     * Solves Kepler's equation: M = E - e * sin(E)
     *
     * @param M The mean anomaly at the time.
     * @param e The eccentricity.
     *
     * @return The solution to Kepler's equation, E.
     */
    public double solveKepler(double M, double e) {
        double E = (e < 0.8) ? M : FastMath.PI;
        for (int i = 0; i < 100; i++) {
            double f = E - e * FastMath.sin(E) - M;
            double fPrime = 1.0 - e * FastMath.cos(E);
            double dE = -f / fPrime;
            E += dE;
            if (FastMath.abs(dE) < 1.0e-10) break;
        }
        return E;
    }

    /**
     * Convert a true anomaly (nu) in the current orbit to the Julian days since epoch.
     *
     * @param nuRad The true anomaly, in radians.
     *
     * @return The Julian days since epoch corresponding to the given true anomaly.
     */
    public double trueAnomalyToTime(double nuRad) {
        // Mean motion in rad/day
        var n = PI2 / period;
        // Convert true anomaly to eccentric anomaly E
        double tanHalfNu = FastMath.tan(nuRad / 2.0);
        double sqrtFactor = FastMath.sqrt((1 - e) / (1 + e));
        double E = 2.0 * FastMath.atan2(tanHalfNu * sqrtFactor, 1.0);

        E = (E + PI2) % PI2;

        // Compute mean anomaly
        double M = E - e * FastMath.sin(E);

        // Mean anomaly at epoch
        double M0 = FastMath.toRadians(meanAnomaly);

        // Return time in Julian days since epoch
        return (M - M0) / n;
    }

    /**
     * Convert the given time from epoch (in days) to the true anomaly angle.
     * @param dtDays The time from epoch, in days.
     * @return The true anomaly, in radians.
     */
    public double timeToTrueAnomaly(double dtDays) {
        // Mean anomaly at time.
        double M = getMeanAnomalyAt(dtDays);
        // Solve eccentric anomaly with Kepler's equations.
        double E = solveKepler(M, e);
        // True anomaly at target time.
        return getTrueAnomaly(E);
    }

    /**
     * Get the cartesian position vector for the given delta time from epoch.
     *
     * @param out    The vector to store the result.
     * @param dtDays The Julian days from epoch.
     */
    public void keplerianToCartesianTime(Vector3D out, double dtDays) {
        computeMu(true);

        double inc = FastMath.toRadians(i);
        double ascNode = FastMath.toRadians(ascendingNode);
        double argP = FastMath.toRadians(argOfPericenter);

        // Mean anomaly at time.
        double M = getMeanAnomalyAt(dtDays);
        // Solve Kepler's equations.
        double E = solveKepler(M, e);
        // True anomaly at target time.
        double nu = getTrueAnomaly(E);
        // Radius
        double r = semiMajorAxis * (1 - e * FastMath.cos(E));

        // Perifocal coordinates.
        double xPf = r * FastMath.cos(nu);
        double yPf = r * FastMath.sin(nu);
        double zPf = 0;

        // Uncomment for velocity vector.
        //double p = semimajoraxis * (1 - e * e);
        //double vxPf = -FastMath.sqrt(mu / p) * FastMath.sin(nu);
        //double vyPf = FastMath.sqrt(mu / p) * (e + FastMath.cos(nu));
        //double vzPf = 0;

        // Rotation matrix
        double cosO = FastMath.cos(ascNode);
        double sinO = FastMath.sin(ascNode);
        double cosI = FastMath.cos(inc);
        double sinI = FastMath.sin(inc);
        double cosW = FastMath.cos(argP);
        double sinW = FastMath.sin(argP);

        Rx.val[Matrix3D.M00] = cosO * cosW - sinO * sinW * cosI;
        Rx.val[Matrix3D.M01] = -cosO * sinW - sinO * cosW * cosI;
        Rx.val[Matrix3D.M02] = sinO * sinI;

        Rx.val[Matrix3D.M10] = sinO * cosW + cosO * sinW * cosI;
        Rx.val[Matrix3D.M11] = -sinO * sinW + cosO * cosW * cosI;
        Rx.val[Matrix3D.M12] = -cosO * sinI;

        Rx.val[Matrix3D.M20] = sinW * sinI;
        Rx.val[Matrix3D.M21] = cosW * sinI;
        Rx.val[Matrix3D.M22] = cosI;

        // Position
        vAux.set(xPf, yPf, zPf);
        vAux.mul(Rx);
        // Velocity
        //vAux.set(vxPf, vyPf, vzPf);
        //vAux.traMul(mAux);

        // From regular XYZ to X'Y'Z' (Gaia Sky coordinates).
        out.set(vAux.y, vAux.z, vAux.x).scl(Constants.KM_TO_U);
    }

    /**
     * Get the cartesian position vector for the given true anomaly (nu).
     *
     * @param out The vector to store the result.
     * @param nu  The true anomaly.
     */
    public void keplerianToCartesianNu(Vector3D out, double nu) {
        computeMu(true);

        double inc = FastMath.toRadians(i);
        double ascNode = FastMath.toRadians(ascendingNode);
        double argP = FastMath.toRadians(argOfPericenter);

        // Distance
        double r = semiMajorAxis * (1 - e * e) / (1 + e * FastMath.cos(nu));
        // Perifocal coordinates
        double xPf = r * FastMath.cos(nu);
        double yPf = r * FastMath.sin(nu);
        double zPf = 0;

        // Uncomment for velocity vector.
        //double p = semimajoraxis * (1 - e * e);
        //double vxPf = -FastMath.sqrt(mu / p) * FastMath.sin(nu);
        //double vyPf = FastMath.sqrt(mu / p) * (e + FastMath.cos(nu));
        //double vzPf = 0;

        // Rotation matrix
        double cosO = FastMath.cos(ascNode);
        double sinO = FastMath.sin(ascNode);
        double cosI = FastMath.cos(inc);
        double sinI = FastMath.sin(inc);
        double cosW = FastMath.cos(argP);
        double sinW = FastMath.sin(argP);

        Rx.val[Matrix3D.M00] = cosO * cosW - sinO * sinW * cosI;
        Rx.val[Matrix3D.M01] = -cosO * sinW - sinO * cosW * cosI;
        Rx.val[Matrix3D.M02] = sinO * sinI;

        Rx.val[Matrix3D.M10] = sinO * cosW + cosO * sinW * cosI;
        Rx.val[Matrix3D.M11] = -sinO * sinW + cosO * cosW * cosI;
        Rx.val[Matrix3D.M12] = -cosO * sinI;

        Rx.val[Matrix3D.M20] = sinW * sinI;
        Rx.val[Matrix3D.M21] = cosW * sinI;
        Rx.val[Matrix3D.M22] = cosI;

        // Position
        vAux.set(xPf, yPf, zPf);
        vAux.mul(Rx);
        // Velocity
        //vAux.set(vxPf, vyPf, vzPf);
        //vAux.traMul(mAux);

        // From regular XYZ to X'Y'Z' (Gaia Sky coordinates).
        out.set(vAux.y, vAux.z, vAux.x).scl(Constants.KM_TO_U);
    }

    public void loadDataPointNu(Vector3D out, double nu) {
        keplerianToCartesianNu(out, nu);
    }

    // See https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
    @Deprecated
    public void loadDataPointReneSchwarz(Vector3D out, double dtDays) {
        computeMu(false);
        double a = semiMajorAxis * Nature.KM_TO_M; // km to m
        double M0 = meanAnomaly * MathUtilsDouble.degRad;
        double omega_lan = ascendingNode * MathUtilsDouble.degRad;
        double omega_ap = argOfPericenter * MathUtilsDouble.degRad;
        double ic = i * MathUtilsDouble.degRad;

        // 1
        double dt = dtDays * Nature.D_TO_S;
        double M = M0 + dt * FastMath.sqrt(mu / FastMath.pow(a, 3d));

        // 2
        double E = M;
        for (int j = 0; j < 2; j++) {
            E = E - ((E - e * FastMath.sin(E) - M) / (1 - e * FastMath.cos(E)));
        }
        double E_t = E;

        // 3
        double nu_t = 2d * FastMath.atan2(FastMath.sqrt(1.0 + e) * FastMath.sin(E_t / 2.0), FastMath.sqrt(1.0 - e) * FastMath.cos(E_t / 2.0));

        // 4
        double rc_t = a * (1.0 - e * FastMath.cos(E_t));

        // 5
        double ox = rc_t * FastMath.cos(nu_t);
        double oy = rc_t * FastMath.sin(nu_t);

        // 6
        double sinomega = FastMath.sin(omega_ap);
        double cosomega = FastMath.cos(omega_ap);
        double sinOMEGA = FastMath.sin(omega_lan);
        double cosOMEGA = FastMath.cos(omega_lan);
        double cosi = FastMath.cos(ic);
        double sini = FastMath.sin(ic);

        double x = ox * (cosomega * cosOMEGA - sinomega * cosi * sinOMEGA) - oy * (sinomega * cosOMEGA + cosomega * cosi * sinOMEGA);
        double y = ox * (cosomega * sinOMEGA + sinomega * cosi * cosOMEGA) + oy * (cosomega * cosi * cosOMEGA - sinomega * sinOMEGA);
        double z = ox * (sinomega * sini) + oy * (cosomega * sini);

        // 7
        x *= Constants.M_TO_U;
        y *= Constants.M_TO_U;
        z *= Constants.M_TO_U;

        out.set(y, z, x);
    }

    @Override
    public String toString() {
        String desc;
        desc = Objects.requireNonNullElseGet(source, () -> "{epoch: " + epoch + ", period: " + period + ", e: " + e + ", i: " + i + ", sma: " + semiMajorAxis + "}");
        return desc;
    }

}
