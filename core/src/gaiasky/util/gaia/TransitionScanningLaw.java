/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * GaiaTools Copyright (C) 2006 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package gaiasky.util.gaia;

import gaiasky.util.Nature;
import gaiasky.util.coord.NslSun;
import gaiasky.util.gaia.time.Duration;
import gaiasky.util.math.Quaterniond;

/**
 * Implements a smooth transition from {@link Nsl37} to {@link Epsl}, or vice
 * versa.
 * <p>
 * The scanning law used by this AttitudeDataServer is only valid (to required
 * accuracy) in a short (< 0.5 day) time interval when the spin axis is less
 * than a degree of the ecliptic. The revolving phase angle (nu) is written as
 * nu(t) = 0.5 * acc * t^2 (in preceding mode, that is when nu is close to 0) or
 * as nu(t) = PI + 0.5 * acc * t^2 (in following mode, that is when nu is close
 * to PI). Here t is the time in [days] from the reference time (tRef) of this
 * scanning law, and acc is a constant. On initialization, acc is set to a value
 * such that nu(t) according to this law agrees with Nsl37 (for some suitable
 * choice of initial angles) both in value and rate at t = tRef + T, where T is
 * the ramp-up time (if T > 0) or ramp-down time (if T < 0). At t = tRef the
 * revolving phase is exactly 0 (preceding mode) or PI (following mode).
 * <p>
 * A number of reference quantities can be set as usual by means of setRefTime,
 * setXiRef, setOmegaRef, setTargetPrecRate, and setTargetScanRate from the
 * AnalyticalAttitudeDataServer. Only setNuRef should not be used; it is
 * replaced by setMode(Epsl.Mode). [Nevertheless it is possible to use
 * setNuRef(0.0) instead of setMode(Epsl.Mode.PRECEDING) and setNuRef(Math.PI)
 * instead of setMode(Epsl.Mode.PRECEDING).]
 *
 * @author lennartlindegren
 */
public class TransitionScanningLaw extends AnalyticalAttitudeDataServer {

    protected Duration ramp;
    protected double acc;
    protected double om0, om1, om2, om3, om4;

    /**
     * Relative tolerance for exceeding the ramp
     */
    protected double eps = 1e-9;

    /**
     * The constructor is the only way to set the duration of the ramp
     *
     * @param ramp
     */
    public TransitionScanningLaw(Duration ramp) {
        setDefault();
        this.ramp = ramp;
        setInitialized(false);
    }

    /**
     * Return the Epsl.Mode (preceding or following)
     *
     * @return The mode
     */
    public Epsl.Mode getMode() {
        Epsl.Mode mode = Epsl.Mode.PRECEDING;
        if (Math.cos(getNuRef()) < 0) {
            mode = Epsl.Mode.FOLLOWING;
        }
        return mode;
    }

    /**
     * Preferred method to set the Epsl.Mode (it can also be set using setNuRef)
     *
     * @param mode EPSL preceding or following
     *
     * @return This object with the right mode set
     */
    public TransitionScanningLaw setMode(Epsl.Mode mode) {
        setNuRef(mode);
        setInitialized(false);
        return this;
    }

    private void setNuRef(Epsl.Mode mode) {
        if (mode == Epsl.Mode.PRECEDING) {
            setNuRef(0.0);
        } else {
            setNuRef(Math.PI);
        }
    }

    /**
     * Return the duration of the ramp
     */
    public Duration getRampDuration() {
        return ramp;
    }

    /**
     * Initialization mainly calculates the acceleration required for the
     * specified ramp
     */
    public void initialize() {

        // constants:
        double xi = getXiRef();
        double sinXi = Math.sin(xi);
        double cosXi = Math.cos(xi);
        double Snom = NslUtil.calcSNom(xi, getTargetPrecessionRate());
        double rampDays = ramp.asDays();

        // make sure nuRef is nothing but 0.0 or PI
        setNuRef(getMode());

        // derivative of solar longitude [rad/day] at the end of the ramp
        long tEnd = getRefTime() + ramp.asNanoSecs();
        NslSun sunDir = getNominalSunVector();
        sunDir.setTime(tEnd);
        double lSunDotEnd = sunDir.getSolarLongitudeDot();

        // reference quantities for lSun, lSunDot, lSunDotDot:
        sunDir.setTime(getRefTime());
        double lSunDotRef = sunDir.getSolarLongitudeDot();
        double lSunDotDotRef = (lSunDotEnd - lSunDotRef) / rampDays;

        // calculate acceleration to reach nominal nuDot at tEnd:
        acc = 0.0;
        double sign = Math.cos(getNuRef());

        // The loop here does not use the variable i; we just know from experience that this loop
        // converges after about 6 iterations, and possibly less. But it's fast, so we do 10 iterations
        // to be sure. The feedback in the loop comes in via the acc variable.
        for (int i = 0; i < 10; i++) {
            // delta = nu (preceding) or nu - PI (following mode) at tEnd
            double delta = 0.5 * acc * rampDays * rampDays;
            double sinNu = sign * Math.sin(delta);
            acc = (Math.sqrt(Snom * Snom - 1.0 + sinNu * sinNu) + cosXi * sinNu) * lSunDotEnd / (sinXi * rampDays);
        }

        om0 = getOmegaRef();
        om1 = getTargetScanRate() * Nature.ARCSEC_TO_RAD * Nature.D_TO_S;
        om2 = -acc * cosXi / 2;
        om3 = -sign * acc * sinXi * lSunDotRef / 6;
        om4 = -sign * acc * sinXi * lSunDotDotRef / 8;

        setInitialized(true);
    }

    /**
     * @param time - the time elapsed since the epoch of J2010 in ns (TCB)
     *
     * @return attitude for the given time
     *
     * @see gaiasky.util.gaia.AnalyticalAttitudeDataServer#getAttitude(long)
     */
    @Override
    public IAttitude getAttitudeNative(long time) {

        if (!isInitialized()) {
            initialize();
        }

        // t = time in [days] from tRef
        double t = (time - getRefTime()) * 1e-9 / Nature.D_TO_S;

        double tNorm = t / ramp.asDays();
        // tNorm must by in [-eps, |ramp|+eps] for a valid t
        if (tNorm < -eps || tNorm > 1.0 + eps) {
            throw new RuntimeException("TSL requested for time outside of ramp: t = " + t + " days, ramp = " + ramp.asDays() + " days");
        }

        // nu(t) is calculated for constant acceleration
        double nu = getNuRef() + 0.5 * acc * t * t;
        double nuDot = acc * t;

        // omega(t) is calculated to fourth order
        double omega = om0 + t * (om1 + t * (om2 + t * (om3 + t * om4)));
        double omegaDot = om1 + t * (2 * om2 + t * (3 * om3 + t * 4 * om4));

        NslSun sunDir = getNominalSunVector();
        // the sunDir uses the same reference epoch as this class, so we can pass the ns directly
        sunDir.setTime(time);

        // convert heliotropic angles and rates to quaternion and rate
        Quaterniond[] qr = AttitudeConverter.heliotropicToQuaternions(sunDir.getSolarLongitude(), getXiRef(), nu, omega, sunDir.getSolarLongitudeDot(), nuDot, omegaDot);

        return new ConcreteAttitude(time, qr[0], qr[1], false);
    }

}
