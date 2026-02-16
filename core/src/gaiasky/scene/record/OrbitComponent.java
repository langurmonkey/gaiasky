/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.util.Logger;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.KeplerianElements;
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
    /** Eccentricity of the ellipse. **/
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
        if (!externalMu)
            setMu(KeplerianElements.computeMu(true, period, semiMajorAxis));
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
     *
     * @param dtDays The time from epoch, in days.
     *
     * @return The true anomaly, in radians.
     */
    public double timeToTrueAnomaly(double dtDays) {
        return KeplerianElements.timeToTrueAnomaly(dtDays, period, meanAnomaly, e);
    }

    /**
     * Get the cartesian position vector for the given delta time from epoch.
     *
     * @param out    The vector to store the result.
     * @param dtDays The Julian days from epoch.
     */
    public void keplerianToCartesianTime(Vector3D out, double dtDays) {
        computeMu(true);
        KeplerianElements.keplerianToCartesianTime(out, dtDays, period, i, e, ascendingNode, argOfPericenter, semiMajorAxis, meanAnomaly);
    }

    /**
     * Get the cartesian position vector for the given true anomaly (nu).
     *
     * @param out The vector to store the result.
     * @param nu  The true anomaly.
     */
    public void keplerianToCartesianNu(Vector3D out, double nu) {
        computeMu(true);
        KeplerianElements.keplerianToCartesianNu(out, nu, i, e, ascendingNode, argOfPericenter, semiMajorAxis);
    }

    public void loadDataPointNu(Vector3D out, double nu) {
        keplerianToCartesianNu(out, nu);
    }

    @Override
    public String toString() {
        String desc;
        desc = Objects.requireNonNullElseGet(source,
                                             () -> "{epoch: " + epoch + ", period: " + period + ", e: " + e + ", i: " + i + ", sma: " + semiMajorAxis + "}");
        return desc;
    }

}
