/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.Nature;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.coord.NslSun;
import gaiasky.util.gaia.time.Secs;
import gaiasky.util.gaia.utils.*;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.QuaternionDouble;
import gaiasky.util.math.Vector3d;

import java.util.Arrays;

public class ModifiedScanningLaw {

    protected static final double PI = Math.PI;
    protected static final double TWO_PI = 2.0 * PI;
    protected static final double FOUR_PI = 4.0 * PI;
    protected final static double DEG = PI / 180.0;

    protected final static double DAY_NS = 86400e9;

    protected static final double obliquity = Coordinates.OBLIQUITY_ARCSEC_J2000 * Nature.ARCSEC_TO_RAD;

    // define ecliptic pole in equatorial coordinates
    // NOTE: The direction cosines matrix from GPDB should not be used
    // here since the shift of the origin (equinox) by 0.05542 arcsec
    // is not consistent with the ScanningLaw transformation from
    // ecliptic to equatorial coordinates.
    protected static Vector3d eclPole = new Vector3d(0.0, 0.0, 1.0).rotate(obliquity * Nature.TO_DEG, 0, 0, 1);
    /**
     * The sun object is used to calculate the longitude and longitude rate of
     * the nominal sun. Mathematically, it should give exactly the same
     * longitude as used by NSL.
     */
    private final NslSun sun = new NslSun();
    /**
     * The Nsl37 object is used for default initialization of the MSL.
     */
    private final Nsl37 nsl = new Nsl37();
    /**
     * Reference epoch to which the reference scan parameters refer
     */
    protected long refEpoch;
    /**
     * Precession rate (K) in [rev/yr].
     */
    protected double precRate;
    /**
     * Inertial scanning speed (omega_z) given either as a period in [ns] or as
     * a rate in [deg/hour] = [arcsec/s].
     */
    protected long scanPerNs;
    protected double scanRate;
    /**
     * The uniform speed of the z-axis in solar motion units (S) for the nominal
     * scanning law [dimensionless]
     */
    protected double sNom;
    /**
     * Reference solar longitude (at the reference epoch) [rad]
     */
    protected double lSunRef;
    /**
     * Reference heliotropic spin phase (at the reference epoch) [rad]
     */
    protected double omegaRef;
    /**
     * Reference heliotropic revolving phase (at the reference epoch) [rad]
     */
    protected double nuRef;
    /**
     * Current time in [ns] from 2010.0
     */
    protected long tNow;
    /**
     * Current value of the solar longitude [rad] and rate of solar longitude
     * [rad/day]
     */
    protected double lSun;
    protected double lSunDot;
    /**
     * Current value of the solar aspect angle (revolving angle) [rad] and the
     * quaternion representing a rotation by 90-xi about Y axis
     */
    protected double xi;
    protected double sinXi, cosXi;
    /**
     * Current value of the heliotropic revolving phase [rad]
     */
    protected double nu;
    /**
     * Current value of the heliotropic spin phase Omega [rad]. For good
     * numerical precision, this is kept as an angle in the range [0, 2*pi]. The
     * integer number of revolutions since the reference value (omega0) is given
     * by the auxiliary variable omegaRevs. Thus a continuous angle as function
     * of time is in principle given by the quantity omega + 2 * pi * omegaRevs.
     */
    protected double omega;
    protected int omegaRevs;
    /**
     * Have computed quantities been properly initialized?
     */
    protected boolean initialized;
    /**
     * The starting time for the integration as GaiaTime and [ns] from J2010.0
     */
    protected long tBeg;
    protected long tBegNs;
    /**
     * Value of the revolving phase at tBeg
     */
    protected double nuBeg;
    /**
     * Value of the spin phase at tBeg
     */
    protected double deltaOmegaBeg;
    protected int omegaRevsBeg;
    /**
     * Additional fields used by numerical scanning laws
     */
    protected double deltaOmega, deltaOmegaDot, omegaDot, nuDot;
    /**
     * Variables for the Runge-Kutta integrator
     */
    protected long tOld;
    protected double[] y = new double[2];
    protected double[] yOld = new double[2];
    protected double[] dydt = new double[2];

    /**
     * The dn object allows the Runge-Kutta integrator to calculate derivatives
     * and gives access to constants needed for the calculation
     */
    protected Derivm dn;

    /**
     * Local constants
     */
    protected double sFactor, zMax, zMin, s1min;
    protected double sRed, kappa;
    protected boolean reduced;
    protected Vector3d spinAxis = new Vector3d();
    protected Vector3d[] refDir;
    protected long unit, dt;
    protected ComplexArea[] highDensityAreas;
    protected ScanState status;

    /**
     * Constructor with NSL initialization for given start time
     *
     * @param gtBeg - The time elapsed in nanoseconds since epoch J2010
     */
    public ModifiedScanningLaw(long gtBeg) {

        tBeg = gtBeg;
        tBegNs = tBeg;

        // THE FOLLOWING DEFAULT INITIALIZATIONS CAN BE CHANGED USING
        // setRefEpoch, setRefNuOmega, setPrecRate, setScanRate,
        // setMslParameters, and setHighDensityAreas:

        // starting time is also used as default reference epoch for Scanning
        // Laws
        refEpoch = tBeg;
        nsl.setRefTime(refEpoch);

        // default reference heliotropic angles are the default NSL angles at
        // refEpoch:
        xi = nsl.getXiRef();
        nuRef = nsl.getNuRef();
        omegaRef = nsl.getOmegaRef();

        // default precession and spin rate as the default NSL values
        scanRate = nsl.getTargetScanRate();
        precRate = nsl.getTargetPrecessionRate();

        // default MSL parameters (speed reduction factor, AC extent of speed
        // reduction, minimum parallax factor)
        sFactor = 0.15;
        zMax = Math.toRadians(0.50);
        zMin = Math.toRadians(0.30);
        s1min = 0.5;

        // default complex areas (none)
        highDensityAreas = new ComplexArea[]{};
        refDir = new Vector3d[0];

        // THE REMAINING INITIALIZATIONS SHOULD NOT BE CHANGED!!!

        // derivative function for the integrator and its time unit:
        dn = new Derivm();
        unit = (long) DAY_NS;

        // default maximum time step for Runge-Kutta integrator -
        // a step of 150 sec is needed for MSL (NSL can do with 1000 sec step):
        double stepSecs = 150.0;
        setMaxInternalTimeStep(Math.round(stepSecs * 1e9));

        initialize();

    }

    /**
     * Make constants available to integrator and compute the initial values of
     * the scanning law angles at the starting time of the integration.
     */
    protected void initialize() {

        // calculate the scan period in [ns]
        scanPerNs = new Secs((360.0 * 3600.0) / scanRate).asNanoSecs();

        // calculate nominal and reduced S for the given precession rate
        sNom = NslUtil.calcSNom(xi, precRate);
        sRed = sFactor * sNom;

        // calculate other constants
        sinXi = Math.sin(xi);
        cosXi = Math.cos(xi);

        // reference solar longitude
        sun.setTime(refEpoch);
        lSunRef = sun.getSolarLongitude();

        // initialize time and heliotropic angles to refEpoch:
        tNow = refEpoch;
        sun.setTime(tNow);
        lSun = sun.getSolarLongitude();
        lSunDot = sun.getSolarLongitudeDot();
        nuBeg = nuRef;
        deltaOmegaBeg = omegaRef;
        omegaRevsBeg = 0;

        // reset integration variables to refEpoch values
        reset();

        // if tBeg = refEpoch then we are done, otherwise
        if (refEpoch < tBeg) {

            // integrate from refEpoch to tBeg and set (nuBeg, deltaOmegaBeg) to
            // the computed values at tBeg, and reset the integrator to tBeg
            stepForward(tBegNs - refEpoch);
            nuBeg = nu;
            deltaOmegaBeg = deltaOmega;
            omegaRevsBeg = omegaRevs;
            tNow = tBegNs;
            reset();
        }

    }

    /**
     * Reset the integrator to (tNow, nuBeg, omegaBeg, ...)
     */
    protected void reset() {

        // initialize integration variables:
        nu = nuBeg;
        deltaOmega = deltaOmegaBeg;
        omegaRevs = omegaRevsBeg;

        y[0] = nu;
        y[1] = deltaOmega;
        dydt = dn.derivn(tNow, y);
        nuDot = dydt[0];
        deltaOmegaDot = dydt[1];
        tOld = tNow;
        yOld[0] = y[0];
        yOld[1] = y[1];

        calcOmega();

        initialized = true;

    }

    /**
     * Set the reference heliotropic angles (at refEpoch) to other values than
     * the default values obtained with the constructor
     *
     * @param refNu    value of revolving phase (nu) at refEpoch [rad]
     * @param refOmega value of spin phase (omega) at refEpoch [rad]
     */
    public void setRefNuOmega(double refNu, double refOmega) {
        nuRef = refNu;
        omegaRef = refOmega;
        initialized = false;
    }

    /**
     * Set the reference heliotropic angle xi (solar aspect angle).
     * <p>
     * This is nominally constant, so it does not refer to any specific epoch.
     *
     * @param refXi value of solar aspect angle (xi) [rad]
     */
    public void setRefXi(double refXi) {
        xi = refXi;
        initialized = false;
    }

    /**
     * Get current time step in Runge-Kutta integrator
     *
     * @return time step [ns]
     */
    public long getMaxInternalTimeStep() {
        return dt;
    }

    /**
     * Set time step in Runge-Kutta integrator
     *
     * @param stepNs time step [ns]
     */
    public void setMaxInternalTimeStep(long stepNs) {
        dt = stepNs;
    }

    /**
     * Set MSL parameters: speed reduction factor for z axis and AC limits
     *
     * @param factor  reduction factor z speed
     * @param zMaxDeg maximum AC coordinate for reduced speed [deg]
     * @param zMinDeg minimum AC coordinate for reduced speed [deg]
     * @param s1Min   minimum parallax factor for reduced speed [-]
     */
    public void setMslParameters(double factor, double zMaxDeg, double zMinDeg, double s1Min) {
        sFactor = factor;
        s1min = s1Min;
        zMax = Math.toRadians(zMaxDeg);
        zMin = Math.toRadians(zMinDeg);
        initialized = false;
    }

    /**
     * Defines a typical high-density area for the MSL (as of April 2013).
     * <p>
     * The area consists of two circles of 0.5 deg radius each, centred on BW
     * (at Galactic coordinates lon = 1.04 deg, lat = -3.88 deg) and Sgr I (at
     * lon = 1.44 deg, lat = -2.64 deg). See presentation by LL at GST-41.
     */
    public void setTypicalHighDensityArea() {

        ComplexArea ca = new ComplexArea();
        ca.setName("BW + Sgr I");

        // circle 1:
        Vector3d dir1 = new Vector3d();
        double radius1 = 0.50 * DEG;
        Coordinates.sphericalToCartesian(1.04 * DEG, -3.88 * DEG, radius1, dir1);

        // circle 2:
        Vector3d dir2 = new Vector3d();
        double radius2 = 0.50 * DEG;
        Coordinates.sphericalToCartesian(1.44 * DEG, -2.64 * DEG, radius2, dir2);

        Matrix4d galEq = Coordinates.eqToGal();
        dir1.mul(galEq);
        dir2.mul(galEq);

        ca.add(new CircleArea(new Place(dir1), radius1));
        ca.add(new CircleArea(new Place(dir2), radius2));

        this.setHighDensityAreas(new ComplexArea[]{ca});

    }

    /**
     * Get a list of high-density areas that have been set
     */
    public ComplexArea[] getHighDensityAreas() {
        return highDensityAreas;
    }

    /**
     * Set the region of the sky that is to be considered as "high density"
     *
     * @param areas high density area
     */
    public void setHighDensityAreas(ComplexArea[] areas) {
        if (areas != null) {
            int nAreas = areas.length;
            highDensityAreas = new ComplexArea[nAreas];
            refDir = new Vector3d[nAreas];
            for (int i = 0; i < nAreas; i++) {
                highDensityAreas[i] = areas[i];
                refDir[i] = areas[i].getMidPoint().getDirection();
            }
        }
        initialized = false;
    }

    /**
     * Integrate the MSL forward in time by an arbitrary step.
     *
     * @param tStepNs - time step
     * @see ModifiedScanningLaw#advanceScanningTo(long)
     */
    public void stepForward(long tStepNs) {
        if (!initialized) {
            initialize();
        }
        tOld = tNow;
        yOld[0] = y[0];
        yOld[1] = y[1];
        long tNew = tOld + tStepNs;
        y = RungeKuttaNs.fourthOrder(dn, tOld, yOld, tNew, dt, unit);
        dydt = dn.derivn(tNew, y);
        sun.setTime(tNew);
        tNow = tNew;
        lSun = sun.getSolarLongitude();
        lSunDot = sun.getSolarLongitudeDot();
        nu = y[0];
        deltaOmega = y[1];
        nuDot = dydt[0];
        deltaOmegaDot = dydt[1];
        calcOmega();
    }

    /**
     * Regret the last step
     * <p>
     * The time and integration variables are reset to their values before the
     * last invocation of the stepForward method
     */
    public void regretLastStep() {
        tNow = tOld;
        y = Arrays.copyOf(yOld, yOld.length);
        dydt = dn.derivn(tNow, y);
        sun.setTime(tNow);
        lSun = sun.getSolarLongitude();
        lSunDot = sun.getSolarLongitudeDot();
        nu = y[0];
        deltaOmega = y[1];
        nuDot = dydt[0];
        deltaOmegaDot = dydt[1];
        calcOmega();
    }

    public double getLSun() {
        return lSun;
    }

    public double getLSunDot() {
        return lSunDot;
    }

    public double getXi() {
        return xi;
    }

    /**
     * Returns the current heliotropic revolving phase in range [0,2*pi]
     *
     * @return current heliotropic revolving phase [rad] at time given by the
     * {@link #advanceScanningTo(long)} call.
     */
    public double getNu() {
        return nu;
    }

    /**
     * Returns the current heliotropic revolving phase in range [0,4*pi]
     *
     * @return current heliotropic revolving phase [rad] at time given by the
     * {@link #advanceScanningTo(long)} call.
     */
    public double getNuMod4Pi() {
        double rev = Math.floor(nu / FOUR_PI);
        return nu - FOUR_PI * rev;
    }

    /**
     * Returns the scanning phase angle (Omega) in the range [0, 2*pi].
     *
     * @return Scanning phase angle in radian at time given by the last
     * {@link #advanceScanningTo(long)} call.
     */
    public double getOmega() {
        return omega;
    }

    /**
     * Returns the scanning phase angle (Omega) in the range [0, 4*pi].
     *
     * @return Scanning phase angle in radian at time given by the last
     * {@link #advanceScanningTo(long)} call.
     */
    public double getOmegaMod4Pi() {
        return omega + TWO_PI * (double) (omegaRevs % 2);
    }

    /**
     * Returns the integer number of revolutions to be added to the spin phase
     * (omega) such that omega + 2 * pi * omegaRevs is a continuous function of
     * time
     *
     * @return number of revolutions for the spin phase
     */
    public int getOmegaRevs() {
        return omegaRevs;
    }

    /**
     * Get current precession angle rate (d(nu)/dt)
     *
     * @return precession angle rate [rad/day]
     */
    public double getNuDot() {
        return nuDot;
    }

    /**
     * Get current spin phase rate (d(Omega)/dt)
     *
     * @return spin phase rate [rad/day]
     */
    public double getOmegaDot() {
        return omegaDot;
    }

    /**
     * Get current spin phase offset (DeltaOmega)
     *
     * @return spin phase offset [rad] - continuous
     */
    public double getDeltaOmega() {
        return deltaOmega;
    }

    /**
     * Get current spin phase offset rate (d(DeltaOmega/dt)
     *
     * @return spin phase offset rate [rad/day]
     */
    public double getDeltaOmegaDot() {
        return deltaOmegaDot;
    }

    /**
     * Get current speed of z axis in solar motion units
     *
     * @return speed of z axis [-]
     */
    public double getCurrentS() {
        double cosNu = Math.cos(nu);
        double sinNu = Math.sin(nu);
        return Math.sqrt(Math.pow(cosNu, 2) + Math.pow(getKappa() * sinXi - cosXi * sinNu, 2));
    }

    /**
     * Get current kappa = d(nu)/d(sLon)
     *
     * @return kappa [-]
     */
    public double getKappa() {
        return kappa;
    }

    /**
     * Get current status of scanning (nominal, transition, modified)
     *
     * @return status
     */
    public ScanState getStatus() {

        return status;
    }

    /**
     * Get initial time as GaiaTime
     *
     * @return initial time
     */
    public long getGTimeBeg() {
        return tBeg;
    }

    /**
     * Get reference epoch (for the reference values of nu and omega) as
     * nanoseconds since J2010
     *
     * @return reference epoch
     */
    public long getRefEpoch() {
        return refEpoch;
    }

    /**
     * Set the reference epoch to which the reference heliotropic angles refer
     *
     * @param refEpoch The reference epoch.
     * @throws RuntimeException If hte reference epoch is before the start time.
     */
    public void setRefEpoch(long refEpoch) throws RuntimeException {
        if (refEpoch > tBeg) {
            throw new RuntimeException("Reference epoch for MSL cannot be later than the starting time");
        }
        this.refEpoch = refEpoch;
        initialized = false;
    }

    /**
     * Get reference value of nu
     *
     * @return value of nu at refEpoch [rad]
     */
    public double getRefNu() {
        return nuRef;
    }

    /**
     * Set the reference heliotropic angle nu (at refEpoch)
     *
     * @param refNu value of revolving phase (nu) at refEpoch [rad]
     */
    public void setRefNu(double refNu) {
        nuRef = refNu;
        initialized = false;
    }

    /**
     * Get reference values of omega
     *
     * @return value of omega at refEpoch [rad]
     */
    public double getRefOmega() {
        return omegaRef;
    }

    /**
     * Set the reference heliotropic angle Omega (at refEpoch)
     *
     * @param refOmega value of spin phase (omega) at refEpoch [rad]
     */
    public void setRefOmega(double refOmega) {
        omegaRef = refOmega;
        initialized = false;
    }

    /**
     * Get the precession rate of the underlying NSL
     *
     * @return precession rate in [rev/yr]
     */
    public double getPrecRate() {
        return precRate;
    }

    /**
     * Set the precession rate for the underlying NSL
     *
     * @param precRate (target) precession rate in [rev/yr]
     */
    public void setPrecRate(double precRate) {
        this.precRate = precRate;
        initialized = false;
    }

    /**
     * Get the (target) scan rate
     *
     * @return scan rate in [arcsec/s]
     */
    public double getScanRate() {
        return scanRate;
    }

    /**
     * Set the scan rates for the underlying NSL
     *
     * @param scanRate (target) scan rate in [arcsec/s]
     */
    public void setScanRate(double scanRate) {
        this.scanRate = scanRate;
        scanPerNs = new Secs((360.0 * 3600.0) / scanRate).asNanoSecs();
        initialized = false;
    }

    /**
     * Calculates omega and omegaRevs from deltaOmega, t, scanPerNs. omegaDot is
     * calculated at the same time
     */
    protected void calcOmega() {
        long nsSinceRef = tNow - refEpoch;
        omegaRevs = (int) (nsSinceRef / scanPerNs); // integer division
        long remainder = nsSinceRef - omegaRevs * scanPerNs;
        double fractionalPeriod = (double) remainder / (double) scanPerNs;
        omega = deltaOmega + TWO_PI * fractionalPeriod;
        adjustOmega();
        omegaDot = scanRate * TWO_PI / 15.0 + deltaOmegaDot;
    }

    /**
     * Integrate the MSL forward in time by an arbitrary step.
     * <p>
     * Since the numerical implementation of Scanning Law integrates a
     * differential equation, the only way to advance the scanning is to
     * integrate from the current time (t) to the desired time (tNs), provided
     * that tNs >= t. Otherwise, start from the initial conditions and integrate
     * up to tNs.
     *
     * @see ModifiedScanningLaw#stepForward(long)
     */
    public ModifiedScanningLaw advanceScanningTo(long newTimeNs) {
        if (newTimeNs >= tNow) {
            stepForward(newTimeNs - tNow);
        } else {
            initialized = false;
            stepForward(newTimeNs - tBegNs);
        }
        return this;
    }

    /**
     * Adjusts the spin phase (omega) and the number of spin revolutions
     * (omegaRevs) so that 0 &le; omega &lt; 2*pi
     */
    protected void adjustOmega() {
        if (omega >= TWO_PI) {
            int n = (int) (omega / TWO_PI);
            omega -= n * TWO_PI;
            omegaRevs += n;
        } else if (omega < 0) {
            int n = 1 + (int) (-omega / TWO_PI);
            omega += n * TWO_PI;
            omegaRevs -= n;
        }
    }

    /**
     * Calculates a smooth transition of kappa from a nominal value (kappaN) to
     * the reduced value (kappaR), depending on the variable x, which must be in
     * the range from 0 to 1.
     * <p>
     * x = 0 returns kappa = kappaR, x = 1 returns kappaN.
     *
     * @param x      The x.
     * @param kappaN value for x = 1.
     * @param kappaR value for x = 0.
     * @param tf     Type of transition function.
     * @return Transition value.
     */
    protected double transitionKappa(double x, double kappaN, double kappaR, TransitionFunction tf) {
        double kappa = 0.0;
        switch (tf) {
            case LINEAR:
                kappa = kappaR * (1.0 - x) + kappaN * x;
                break;
            case COSINE:
                kappa = 0.5 * ((kappaN + kappaR) - (kappaN - kappaR) * Math.cos(Math.PI * x));
                break;
            case SQUAREROOT:
                kappa = Math.sqrt((1 - x) * kappaR * kappaR + x * kappaN * kappaN);
                break;
            case FANCY:
                double p;
                if (x < 0.5) {
                    p = x * x * (3 - 2 * x);
                } else {
                    p = 1 - (1 - x) * (1 - x) * (1 + 2 * x);
                }
                kappa = Math.sqrt((1 - p) * kappaR * kappaR + p * kappaN * kappaN);
                break;
        }
        return kappa;
    }

    /**
     * The sigmoid function provides a smooth transition from 0 (for x &lt;&lt; 0) to
     * 1 (for x &gt;&gt; 0)
     *
     * @param x The value.
     * @return The sigmoid.
     */
    protected double sigmoid(double x) {
        double e = Math.exp(x);
        return (1 + (e - 1 / e) / (e + 1 / e)) / 2;
    }

    /**
     * There are three modes of scanning, enumerated by the ScanState: NOMINAL =
     * running as in NSL, MODIFIED = running at reduced precession speed,
     * TRANSITION = the precession speed is ramping up or down between the
     * NOMINAL and MODIFIED states. The attitude may have discontinuous second
     * derivatives when changing from one ScanState to another.
     */
    public enum ScanState {
        NOMINAL,
        TRANSITION,
        MODIFIED
    }

    /**
     * Enumerates the various transition functions tested. Used as an argument
     * in transitionKappa().
     *
     * @author lennartlindegren
     * @version $Id$
     */
    protected enum TransitionFunction {
        LINEAR,
        SQUAREROOT,
        COSINE,
        FANCY
    }

    /**
     * Class to evaluate the set of two ordinary differential equations
     * describing the evolution of the precession angle (nu = y[0]) and spin
     * phase (Omega = y[1]) as functions of time.
     * <p>
     * For use with scanninglaws.util.RungeKuttaG
     *
     * @author llindegr
     */
    public class Derivm implements DiffnFunctionNs {

        @Override
        public double[] derivn(long t, double[] y) {

            double[] dydt = new double[y.length];
            sun.setTime(t);
            Vector3d sunDir = new Vector3d();
            sun.getSolarDirection(sunDir);
            double sLonDot = sun.getSolarLongitudeDot();
            double cosNu = Math.cos(y[0]);
            double sinNu = Math.sin(y[0]);

            // calculate nominal kappa
            double kappaN = (Math.sqrt(sNom * sNom - cosNu * cosNu) + cosXi * sinNu) / sinXi;

            // determine the minimum altitude of the highDensityAreas over the
            // scanning plane; the relevant area is
            // highDensityAreas[indexAltMin]
            // with reference direction refDir[indexAltMin]
            QuaternionDouble q = sun.heliotropicToQuaternion(t, xi, y[0], y[1]);
            spinAxis.set(0.0, 0.0, 1.0).rotateVectorByQuaternion(q);
            Place spinAxisPlace = new Place(spinAxis);
            double altMin = 10.0;
            int indexAltMin = 0;
            for (int i = 0; i < highDensityAreas.length; i++) {
                double alt = highDensityAreas[i].altitude(spinAxisPlace);
                if (alt < altMin) {
                    altMin = alt;
                    indexAltMin = i;
                }
            }

            // determine the actual kappa in the range [kappaR, kappaN]
            // where kappaR = reduced rate. A smooth transition is
            // used when the altitude is in the range [zMin, zMax]
            if (altMin < zMax) {
                reduced = true;
                double kappaR = kappaN;
                double s0 = Vector3d.crs(eclPole, spinAxis).dot(refDir[indexAltMin]);
                double s1 = Vector3d.crs(sunDir, spinAxis).dot(refDir[indexAltMin]);
                if (s1 > 0) {
                    kappaR = Math.min(kappaN, (sRed - s0) / s1);
                } else if (s1 < 0) {
                    kappaR = Math.min(kappaN, (sRed + s0) / (-s1));
                }
                if (kappaR < 0)
                    kappaR = 0.0;
                kappaR = kappaN - (kappaN - kappaR) * sigmoid((Math.abs(s1) - s1min) / 0.1);
                if (altMin < zMin) {
                    // use the reduced kappa
                    kappa = kappaR;
                    status = ScanState.MODIFIED;
                } else {
                    // use a smooth transition for x between 0 (R) and 1 (N)
                    double x = (altMin - zMin) / (zMax - zMin);
                    kappa = transitionKappa(x, kappaN, kappaR, TransitionFunction.FANCY);
                    status = ScanState.TRANSITION;
                }
            } else {
                reduced = false;
                // use the nominal kappa
                kappa = kappaN;
                status = ScanState.NOMINAL;
            }

            dydt[0] = kappa * sLonDot;
            dydt[1] = -cosXi * dydt[0] - sinXi * sinNu * sLonDot;

            return dydt;
        }
    }
}
