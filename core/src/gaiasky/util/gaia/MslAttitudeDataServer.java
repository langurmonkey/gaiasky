/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.gaia.time.Duration;
import gaiasky.util.gaia.utils.AttitudeUtils;
import gaiasky.util.math.QuaternionDouble;

import java.util.ArrayList;
import java.util.List;

public class MslAttitudeDataServer extends HermiteInterpolatedAttitudeDataServer {

    protected ModifiedScanningLaw msl;

    /**
     * Maximum step size for the Hermite interpolation.
     */
    protected double maxStepSec;

    /**
     * Actual time step for Hermite interpolation, in [ns]
     */
    protected long step;

    /**
     * The actual starting and end times of the time period covered by the
     * Hermite nodes must be multiples of the timeGranularity since the scanning
     * law reference epoch. This implies that the actual Hermite time step must
     * be an integer fraction of the granularity.
     */
    protected long timeGranularity = (long) 1000e9;

    /**
     * The default integrator time step is the same as for Hermite
     * interpolation. This can be overridden by setMaxStepForINtegrator(double).
     */
    protected boolean useDefaultStepForIntegrator = true;
    protected long stepForIntegrator;

    /**
     * An extra rotation by this angle [rad] is added to the generated attitude
     */
    protected double extraOmega = 0.0;

    /**
     * Keep track of times when the precession rate is reduced
     */
    protected boolean[] reducedPrecession;

    /**
     * Keep track of times when the precession rate is in a transition from
     * nominal to reduced, or vice versa
     */
    protected boolean[] transitionPrecession;

    /**
     * The times when the precession rate changes, calculated from
     * {@link #reducedPrecession}.
     */
    protected long[] precessionRateChanges;

    /**
     * Quaterniond corresponding to extraOmega
     */
    protected QuaternionDouble qExtraOmega = new QuaternionDouble(0.0, 0.0, 0.0, 1.0);

    /**
     * Constructor for given start time and mission length
     *
     * @param tStart  start time
     * @param tLength coverage length
     * @param msl     the underlying scanning law
     *
     * @throws IllegalArgumentException if the tStart is before the start time of the passed scanning
     *                                  law, msl.
     */
    public MslAttitudeDataServer(long tStart, Duration tLength, ModifiedScanningLaw msl) {

        super(tStart, tLength);

        if (tStart < msl.getGTimeBeg()) {
            throw new IllegalArgumentException("MslAttitudeDataServer starts earlier than ModifiedScanningLaw");
        }

        // Attitude data server must have the same reference epoch as MSL
        super.setRefTime(msl.getRefEpoch());

        // Ensure that TimeContext is GAIATIME, not OBMT
        //this.nativeTimeContext = TimeContext.TCB;

        this.msl = msl;
        maxStepSec = 125.0;
        initialized = false;
    }

    /**
     * @see gaiasky.util.gaia.HermiteInterpolatedAttitudeDataServer#initialize()
     */
    @Override
    public void initialize() {

        long tBeg = super.getStartTime();
        long tEnd = super.getStopTime();

        // adjust tBegNs such that the interval since the reference epoch is a
        // multiple of the timeGranularity (the largest such multiple not
        // greater than the original tBeg is chosen):
        long refEpoch = getRefTime();
        long tBegNsFromRef = tBeg - refEpoch;
        tBegNsFromRef = timeGranularity * (tBegNsFromRef / timeGranularity);
        long tBegNs = refEpoch + tBegNsFromRef;

        // adjust tEndNs such that the interval since the reference epoch is a
        // multiple of the timeGranularity (the smallest such multiple not
        // less than the original tEnd is chosen):
        long tEndNsFromRef = tEnd - refEpoch;
        tEndNsFromRef = timeGranularity * ((tEndNsFromRef - 1L) / timeGranularity + 1L);

        long tEndNs = refEpoch + tEndNsFromRef;

        // adjust step to be the largest integer fraction of the timeGranularity
        // less than or equal to the specified maximum step:
        long maxStep = Math.round(maxStepSec * 1e9);
        long part = 1L;
        step = timeGranularity;
        while (step > maxStep || timeGranularity - part * step > 0L) {
            part++;
            step = timeGranularity / part;
        }

        nT = 1 + (int) ((tEndNsFromRef - tBegNsFromRef) / step);
        if (nT < 2) {
            nT = 2;
            step = tEndNsFromRef - tBegNsFromRef;
        }

        // by default the step used by the RK integration is the same as for the
        // Hermite interpolation
        if (useDefaultStepForIntegrator) {
            stepForIntegrator = step;
        }
        msl.setMaxInternalTimeStep(stepForIntegrator);

        tNs = new long[nT];
        qX = new double[nT];
        qY = new double[nT];
        qZ = new double[nT];
        qW = new double[nT];
        rateX = new double[nT];
        rateY = new double[nT];
        rateZ = new double[nT];
        reducedPrecession = new boolean[nT];
        transitionPrecession = new boolean[nT];
        double[] om = new double[nT];

        tNs[0] = tBegNs;
        for (int i = 1; i < nT - 1; i++) {
            tNs[i] = tNs[0] + i * step;
        }
        tNs[nT - 1] = tEndNs;

        long tNowNs = msl.getGTimeBeg();
        for (int i = 0; i < nT; i++) {

            msl.stepForward(tNs[i] - tNowNs);

            double lSun = msl.getLSun();
            double xi = msl.getXi();
            double nu = msl.getNuMod4Pi();
            om[i] = msl.getOmegaMod4Pi();
            double omega = om[i];
            double lSunDot = msl.getLSunDot();
            double nuDot = msl.getNuDot();
            double omegaDot = msl.getOmegaDot();

            QuaternionDouble[] qq = AttitudeConverter.heliotropicToQuaternions(lSun, xi, nu, omega, lSunDot, nuDot, omegaDot);
            QuaternionDouble q = qq[0];
            QuaternionDouble qInvQDot = qq[1].mulLeftInverse(q);
            qX[i] = q.x;
            qY[i] = q.y;
            qZ[i] = q.z;
            qW[i] = q.w;
            rateX[i] = 2 * qInvQDot.x;
            rateY[i] = 2 * qInvQDot.y;
            rateZ[i] = 2 * qInvQDot.z;

            reducedPrecession[i] = (msl.getStatus() != ModifiedScanningLaw.ScanState.NOMINAL);
            transitionPrecession[i] = (msl.getStatus() == ModifiedScanningLaw.ScanState.TRANSITION);

            tNowNs = tNs[i];

        }

        initialized = true;
    }

    /**
     *
     */
    public boolean inGap(long time) {
        return false;
    }

    /**
     * This allows to override the default choice of max step for integrator
     *
     * @param maxStepInSec maximum step in [s]
     */
    public void setMaxStepForIntegrator(double maxStepInSec) {
        stepForIntegrator = Math.round(maxStepInSec * 1e9);
        useDefaultStepForIntegrator = false;
        initialized = false;
    }

    /**
     * Return the currently set maximum step used in the Hermite interpolation
     *
     * @return step [sec]
     */
    public double getMaxStep() {
        return this.maxStepSec;
    }

    /**
     * Set the maximum step size to be used in the Hermite interpolation.
     * Replaces the default value set at construction.
     *
     * @param maxStepInSec [sec]
     */
    public void setMaxStep(double maxStepInSec) {
        this.maxStepSec = maxStepInSec;
        initialized = false;
    }

    /**
     * Return the parameters defining the Hermite interpolation nodes (array
     * tNs)
     * <p>
     * Note: Although the super (HermiteInterpolatedAttitudeDataServer) permits
     * to use a non-equidistant grid of Hermite nodes, the grid in
     * MslAttitudeDataServer is always equidistant. It is therefore completely
     * specified by three parameters: (1) the first point (= tNs[0]), (2) the
     * increment (= tNs[1] - tNs[0]), and (3) the number of nodes (nT). These
     * three values are returned as a long[] array.
     *
     * @return { first node, increment, number of nodes }
     */
    public long[] getNodeParams() {
        return new long[] { this.tNs[0], this.step, (long) this.nT };
    }

    /**
     * Returns the currently set extraOmega [rad]
     *
     * @return
     */
    private double getExtraOmega() {
        return this.extraOmega;
    }

    /**
     * Adds an extra angle to the reference spin phase (omegaRef).
     * <p>
     * In contrast to the normal way of setting omegaRef (applying the method
     * setRefNuOmega to the ModifiedScanningLaw object and initializing the
     * MslAttitudeDataServer) this method does not trigger a re-initialization.
     * It is therefore a fast way to change the effective value of omegaRef.
     * <p>
     * This method is private, so extraOmega cannot be accessed from the
     * outside, instead it is set through the setRefOmega method.
     *
     * @param extraOmega [rad]
     */
    private void setExtraOmega(double extraOmega) {
        this.extraOmega = extraOmega;
        this.qExtraOmega.set(0.0, 0.0, Math.sin(extraOmega / 2), Math.cos(extraOmega / 2));
    }

    /**
     * Returns the (effective) reference value of the heliotropic angle Omega.
     * <p>
     * This is the sum of refOmega (as set in the MSL) and extraOmega
     *
     * @return effective reference value of Omega in [rad]
     */
    public double getRefOmega() {
        return msl.getRefOmega() + this.getExtraOmega();
    }

    /**
     * Sets the (effective) reference value of the heliotropic angle Omega.
     * <p>
     * To avoid re-initializing, extraOmega is set to the difference between the
     * requested refOmega and the value set in the MSL
     *
     * @param refOmega reference value of Omega in [rad]
     */
    public void setRefOmega(double refOmega) {
        this.setExtraOmega(refOmega - msl.getRefOmega());
    }

    /**
     * Returns the underlying ModeifiedScanningLaw. This can be used to retrieve
     * refNu, precRate, scanRate, etc.
     *
     * @return ModifiedScanningLaw used to set up the attitude data server
     */
    public ModifiedScanningLaw getMsl() {
        return msl;
    }

    /**
     * @see gaiasky.util.gaia.HermiteInterpolatedAttitudeDataServer#getAttitude(long)
     */
    public IAttitude getAttitudeNative(long t) throws RuntimeException {

        if (!initialized) {
            initialize();
        }

        IAttitude att = super.getAttitudeNative(t);

        // modify attitude through post-multiplication by qExtraOmega
        if (extraOmega != 0.0) {
            QuaternionDouble q = att.getQuaternion();
            QuaternionDouble qDot = att.getQuaternionDot();
            q.mul(qExtraOmega);
            qDot.mul(qExtraOmega);
            att = new ConcreteAttitude(t, q, qDot, true);
        }

        return att;
    }

    /**
     * Returns true if the precession rate is reduced in the interpolation
     * interval containing t
     *
     * @param t
     *
     * @return
     */
    public boolean isModified(long t) {
        int left = AttitudeUtils.findLeftIndexVar(t, tNs, 0);
        return (reducedPrecession[left] || reducedPrecession[left + 1]);
    }

    /**
     * Returns true if the precession rate is in a transition phase during the
     * interval containing t
     *
     * @param t The rate
     *
     * @return Whether it is in a transition phase
     */
    public boolean isTransition(long t) {
        int left = AttitudeUtils.findLeftIndexVar(t, tNs, 0);
        return (transitionPrecession[left] || transitionPrecession[left + 1]);
    }

    /**
     * Get a list of the (approximate) times when the precession rate has
     * changed.
     */
    public long[] getPrecessionRateChanges() {

        if (!initialized) {
            initialize();
        }

        if (precessionRateChanges == null) {
            synchronized (this) {
                if (precessionRateChanges == null) {
                    precessionRateChanges = calculatePrecessionRateChanges();
                }
            }
        }

        return precessionRateChanges;

    }

    private long[] calculatePrecessionRateChanges() {
        List<Long> changes = new ArrayList<>();

        for (int i = 1; i < reducedPrecession.length; i++) {

            // if (transitionPrecession[i - 1] != transitionPrecession[i]) {
            if (reducedPrecession[i - 1] != reducedPrecession[i]) {

                long t_prev = tNs[i - 1];
                long t_i = tNs[i];

                changes.add((t_prev + t_i) / 2);
            }

        }

        // Convert List<Double> to double[]
        return toPrimitive(changes);

    }

    private long[] toPrimitive(List<Long> l) {
        long[] res = new long[l.size()];
        int i = 0;
        for (Long d : l) {
            res[i++] = d;
        }
        return res;
    }

}
