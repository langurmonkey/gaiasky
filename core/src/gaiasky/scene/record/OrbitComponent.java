/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.time.Instant;
import java.util.Objects;

public class OrbitComponent {

    /** Source file **/
    public String source;
    /** Orbital period in days **/
    public double period;
    /** Base epoch in julian days **/
    public double epoch;
    /** Semi major axis of the ellipse, in Km. **/
    public double semimajoraxis;
    /** Eccentricity of the ellipse, in degrees. **/
    public double e;
    /** Inclination, angle between the reference plane and the orbital plane, in degrees. **/
    public double i;
    /** Longitude of the ascending node in degrees. **/
    public double ascendingnode;
    /** Argument of perihelion in degrees. **/
    public double argofpericenter;
    /** Mean anomaly at epoch, in degrees. **/
    public double meananomaly;
    /** G*M of central body (gravitational constant). Defaults to the Sun's. In km^3/s^2. **/
    public double mu = 1.32712440041e11;

    /** Flag when mu is set externally. **/
    private boolean externalMu = false;

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
        this.semimajoraxis = semiMajorAxis;
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
        this.ascendingnode = ascendingNode;
    }

    public void setAscendingnode(Double ascendingNode) {
        setAscendingNode(ascendingNode);
    }

    public void setArgOfPericenter(Double argOfPericenter) {
        this.argofpericenter = argOfPericenter;
    }

    public void setArgofpericenter(Double argOfPericenter) {
        setArgOfPericenter(argOfPericenter);
    }

    public void setMeanAnomaly(Double meanAnomaly) {
        this.meananomaly = meanAnomaly;
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
        if (period > 0 && semimajoraxis > 0 && !externalMu) {
            double a = semimajoraxis; // In km.
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

    public double trueAnomalyToTime(double nuRad, double meanMotionRadPerSec) {
        // Convert true anomaly to eccentric anomaly E
        double tanHalfNu = Math.tan(nuRad / 2.0);
        double sqrtFactor = Math.sqrt((1 - e) / (1 + e));
        double E = 2.0 * Math.atan2(tanHalfNu * sqrtFactor, 1.0);

        // Normalize E to [0, 2π)
        E = E % (2 * Math.PI);
        if (E < 0) {
            E += 2 * Math.PI;
        }

        // Compute mean anomaly
        double M = E - e * Math.sin(E);

        // Time since epoch in seconds
        double deltaT = M / meanMotionRadPerSec;

        // Return time in Julian Date
        return epoch + deltaT / 86400.0;
    }

    public void loadDataPoint(Vector3d out, Instant t) {
        double tjd = AstroUtils.getJulianDate(t);
        loadDataPoint(out, tjd - epoch);
    }

    public void keplerianToCartesian(Vector3d out, double nu) {
        computeMu(true);

        double inc = FastMath.toRadians(i);
        double raan = FastMath.toRadians(ascendingnode);
        double argp = FastMath.toRadians(argofpericenter);

        // Distance
        double r = semimajoraxis * (1 - e * e) / (1 + e * FastMath.cos(nu));
        // Perifocal coordinates
        double x_pf = r * FastMath.cos(nu);
        double y_pf = r * FastMath.sin(nu);
        double z_pf = 0;

        // Uncomment for velocity vector.
        //double p = semimajoraxis * (1 - e * e);
        //double vx_pf = -FastMath.sqrt(mu / p) * FastMath.sin(nu);
        //double vy_pf = FastMath.sqrt(mu / p) * (e + FastMath.cos(nu));
        //double vz_pf = 0;

        // Rotation matrix
        double cosO = FastMath.cos(raan);
        double sinO = FastMath.sin(raan);
        double cosI = FastMath.cos(inc);
        double sinI = FastMath.sin(inc);
        double cosW = FastMath.cos(argp);
        double sinW = FastMath.sin(argp);

        double[][] R = new double[][]{
                {
                        cosO * cosW - sinO * sinW * cosI,
                        -cosO * sinW - sinO * cosW * cosI,
                        sinO * sinI
                },
                {
                        sinO * cosW + cosO * sinW * cosI,
                        -sinO * sinW + cosO * cosW * cosI,
                        -cosO * sinI
                },
                {
                        sinW * sinI,
                        cosW * sinI,
                        cosI
                }
        };

        double[] rVec = matVecMul(R, new double[]{x_pf, y_pf, z_pf});
        //double[] vVec = matVecMul(R, new double[]{vx_pf, vy_pf, vz_pf});

        // From regular XYZ to X'Y'Z' (Gaia Sky coordinates).
        out.set(rVec[1], rVec[2], rVec[0]).scl(Constants.KM_TO_U);
    }

    private double[] matVecMul(double[][] mat, double[] vec) {
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = mat[i][0] * vec[0] + mat[i][1] * vec[1] + mat[i][2] * vec[2];
        }
        return result;
    }

    public void loadDataPoint(Vector3d out, double nu) {
        keplerianToCartesian(out, nu);
    }

    // See https://downloads.rene-schwarz.com/download/M001-Keplerian_Orbit_Elements_to_Cartesian_State_Vectors.pdf
    @Deprecated
    public void loadDataPointReneSchwarz(Vector3d out, double dtDays) {
        computeMu(false);
        double a = semimajoraxis * Nature.KM_TO_M; // km to m
        double M0 = meananomaly * MathUtilsDouble.degRad;
        double omega_lan = ascendingnode * MathUtilsDouble.degRad;
        double omega_ap = argofpericenter * MathUtilsDouble.degRad;
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
        double nu_t = 2d * FastMath.atan2(Math.sqrt(1.0 + e) * FastMath.sin(E_t / 2.0), FastMath.sqrt(1.0 - e) * FastMath.cos(E_t / 2.0));

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
        desc = Objects.requireNonNullElseGet(source, () -> "{epoch: " + epoch + ", period: " + period + ", e: " + e + ", i: " + i + ", sma: " + semimajoraxis + "}");
        return desc;
    }

}
