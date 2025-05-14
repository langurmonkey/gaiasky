/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.NslSun;
import gaiasky.util.math.Vector3D;
import net.jafama.FastMath;

public abstract class AnalyticalAttitudeDataServer extends BaseAttitudeDataServer<IAttitude> {

    /** Mathematical constants **/
    protected static final double PI = FastMath.PI;
    protected static final double TWO_PI = 2.0 * FastMath.PI;
    protected static final double FOUR_PI = 4.0 * FastMath.PI;
    protected static final double PI_HALF = 0.5 * FastMath.PI;

    /** Factor converting from arcsec/s to deg/day **/
    protected static final double ARCSEC_PER_S_TO_DEG_PER_DAY = 86400.D * (1d / 3600d);

    /** Unit vectors **/
    protected static final Vector3D X_AXIS = Vector3D.getUnitX();
    protected static final Vector3D Y_AXIS = Vector3D.getUnitY();
    protected static final Vector3D Z_AXIS = Vector3D.getUnitZ();

    /** The obliquity of the ecliptic **/
    protected static final double OBLIQUITY_RAD = Coordinates.OBLIQUITY_RAD_J2000;
    protected static final double OBLIQUITY_DEG = Coordinates.OBLIQUITY_DEG_J2000;

    /**
     * The time in ns of one rotation of the satellite around its spin axis.
     */
    protected long targetScanPeriod = FastMath.round(360.0 * 3600.0 * 1.e9 / Satellite.SCANRATE);
    /*
     * every thread gets is own local copy of the NslSun
     */
    protected NslSun nslSun = new NslSun();
    /**
     * Reference time
     */
    private long tRef;
    /**
     * Reference value of the solar aspect angle (valid at time tRef) [rad]
     */
    private double xiRef;
    /**
     * Reference value of the revolving phase angle (valid at time tRef) [rad]
     */
    private double nuRef;
    /**
     * Reference value of the scan phase angle (valid at time tRef) [rad]
     */
    private double omegaRef;
    /**
     * Target precession rate (K) in revolutions per year
     */
    private double targetPrecessionRate;

    /**
     * Set all parameters to default values (from GaiaParam)
     */
    public void setDefault() {
        // nativeTimeContext = TimeContext.TCB;

        // Default reference epoch - 2010
        setRefTime((long) (AstroUtils.JD_J2010 * Nature.D_TO_NS));

        // Default reference solar aspect angle [rad]
        setXiRef(Math.toRadians(Satellite.SOLARASPECTANGLE_NOMINAL));

        // Default reference revolving phase angle [rad]
        setNuRef(Satellite.REVOLVINGPHASE_INITIAL);

        // Default reference scan phase angle [rad]
        setOmegaRef(Satellite.SCANPHASE_INITIAL);

        // Default target scan rate [arcsec/s]
        setTargetScanRate(Satellite.SCANRATE);

        setTargetPrecessionRate(Satellite.SPINAXIS_NUMBEROFLOOPSPERYEAR);
    }

    /**
     * Get the target scan period
     *
     * @return targetScanPeriod period in [ns]
     */
    public long getTargetScanPeriod() {
        return targetScanPeriod;
    }

    /**
     * Set the target scan period
     *
     * @param targetScanPeriod period in [ns]
     */
    public void setTargetScanPeriod(long targetScanPeriod) {
        this.targetScanPeriod = targetScanPeriod;
        initialized = false;
    }

    /**
     * Get the target scan rate
     *
     * @return target scan rate value in [arcsec/s]
     */
    public double getTargetScanRate() {
        return 360.0 * 3600.0 * 1e9 / (double) targetScanPeriod;
    }

    /**
     * Set the target scan rate
     *
     * @param targetScanRate target value in [arcsec/s]
     */
    public void setTargetScanRate(double targetScanRate) {
        targetScanPeriod = FastMath.round(360.0 * 3600.0 * 1e9 / targetScanRate);
        initialized = false;
    }

    /**
     * Get the reference solar aspect angle
     *
     * @return reference solar aspect angle [rad]
     */
    public double getXiRef() {
        return xiRef;
    }

    /**
     * Set the reference value for the solar aspect angle (xi)
     *
     * @param xiRef angle in [rad]
     */
    public void setXiRef(double xiRef) {
        this.xiRef = xiRef;
        initialized = false;
    }

    /**
     * Get the reference revolving phase angle
     *
     * @return reference revolving phase angle [rad]
     */
    public double getNuRef() {
        return nuRef;
    }

    /**
     * Set the reference value for the precession phase angle (nu)
     *
     * @param nuRef angle in [rad]
     */
    public void setNuRef(double nuRef) {
        this.nuRef = nuRef;
        initialized = false;
    }

    /**
     * Get the reference scan phase angle
     *
     * @return reference scan phase angle [rad]
     */
    public double getOmegaRef() {
        return omegaRef;
    }

    /**
     * Set the reference value for the spin phase abgle (Omega)
     *
     * @param omegaRef angle in [rad]
     */
    public void setOmegaRef(double omegaRef) {
        this.omegaRef = omegaRef;
        initialized = false;
    }

    /**
     * Get the target precession rate
     *
     * @return target precession rate [rev/year]
     */
    public double getTargetPrecessionRate() {
        return targetPrecessionRate;
    }

    /**
     * Set the target precession rate
     *
     * @param targetPrecessionRate target value in [rev/yr]
     */
    public void setTargetPrecessionRate(double targetPrecessionRate) {
        this.targetPrecessionRate = targetPrecessionRate;
        initialized = false;
    }

    public boolean inGap(long time) {
        return false;
    }

    /**
     * Ref time in nanoseconds since epoch.
     *
     * @return The reference time [ns]
     */
    public long getRefTime() {
        return tRef;
    }

    /**
     * Sets the reference time in nanoseconds.
     *
     * @param tRef [ns]
     */
    public void setRefTime(long tRef) {
        this.tRef = tRef;
    }

    protected NslSun getNominalSunVector() {
        return nslSun;
    }
}
