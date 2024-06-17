/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

import gaiasky.util.Nature;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.NslSun;
import gaiasky.util.math.QuaternionDouble;
import net.jafama.FastMath;

public class Nsl37 extends AnalyticalAttitudeDataServer {

    /**
     * Variables that are calculated on initialization
     */
    private double sx, cx, sx2, cx2, cx3, sx4, cx4, cx6;
    private double c1a, c2a, c3a, c4a, c5a;
    private double s1a, s2a, s3a, s4a;
    private double on, ocn, os2n, oc3n, os4n, oc5n;
    private double c0v, c1v, c2v, c3v, c4v;
    private double s1v, s2v, s3v, s4v;
    private double lSunRef, xi, nu0;
    private long scanPerNs;

    /**
     * Variables used to keep track of number of revolutions in nu and Omega
     **/
    private double nu, omega, nuDot, omegaDot;
    private int nuRevs, omegaRevs;

    /**
     * Creates a new instance of the Nominal Scanning Law with default values
     * from the GPDB
     */
    public Nsl37() {
        super.setDefault();
    }

    /**
     * Calculate the scanning law at a given time. See GAIA-CA-TN-OCA-FM-037-2
     *
     * @param julianDate The julian date
     *
     * @return The attidue object
     */
    public IAttitude getAttitudeNative(double julianDate) {
        long tNs = (long) ((julianDate - AstroUtils.JD_J2010) * Nature.D_TO_NS);
        return getAttitudeNative(tNs);
    }

    /**
     * @param tNow The time elapsed in nanoseconds since epoch J2010
     *
     * @return The attitude object
     */
    protected IAttitude getAttitudeNative(long tNow) {
        if (!initialized) {
            recomputeConstants();
            setInitialized(true);
        }

        // Get the solar longitude [rad]
        nslSun.setTime(tNow);
        double lSun = nslSun.getSolarLongitude();
        double lSunDot = nslSun.getSolarLongitudeDot();
        double precRate = this.getTargetPrecessionRate();
        double z = precRate * (lSun - lSunRef);
        double zDot = precRate * lSunDot;

        double s1z = FastMath.sin(z);
        double c1z = FastMath.cos(z);
        double s2z = 2 * c1z * s1z;
        double c2z = 2 * c1z * c1z - 1;
        double s3z = s2z * c1z + c2z * s1z;
        double c3z = c2z * c1z - s2z * s1z;
        double s4z = 2 * c2z * s2z;
        double c4z = 2 * c2z * c2z - 1;

        nu = this.getNuRef() + z + c0v + c1v * c1z + s1v * s1z + c2v * c2z + s2v * s2z + c3v * c3z + s3v * s3z + c4v * c4z + s4v * s4z;

        nuDot = zDot * (1.0 - c1v * s1z + s1v * c1z - 2.0 * c2v * s2z + 2.0 * s2v * c2z - 3.0 * c3v * s3z + 3.0 * s3v * c3z - 4.0 * c4v * s4z + 4.0 * s4v * c4z);

        // Elapsed time since 2010 epoch:
        long tElapsed = tNow - getRefTime();
        this.omegaRevs = (int) (tElapsed / scanPerNs);

        double omegaArg = TWO_PI * (double) (tElapsed - omegaRevs * scanPerNs) / (double) scanPerNs;

        omega = this.getOmegaRef() + omegaArg + on * (nu - nu0) + ocn * (Math.cos(nu) - c1a) + os2n * (Math.sin(2 * nu) - s2a) + oc3n * (Math.cos(3 * nu) - c3a) + os4n * (Math.sin(4 * nu) - s4a) + oc5n * (Math.cos(5 * nu) - c5a);

        omegaDot = TWO_PI * 86400e9 / (double) scanPerNs + nuDot * (on - ocn * FastMath.sin(nu) + 2 * os2n * FastMath.cos(2 * nu) - 3 * oc3n * FastMath.sin(3 * nu) + 4 * os4n * FastMath.cos(4 * nu) - 5 * oc5n * FastMath.sin(5 * nu));

        nuRevs = 0;
        // put nu in [0, 2*pi[ and adjust number of revolutions accordingly
        if (nu >= TWO_PI) {
            int n = (int) (nu / TWO_PI);
            nu -= n * TWO_PI;
            nuRevs += n;
        } else if (nu < 0) {
            int n = 1 + (int) (-nu / TWO_PI);
            nu += n * TWO_PI;
            nuRevs -= n;
        }

        // put omega in [0, 2*pi[ and adjust number of revolutions accordingly
        if (omega >= TWO_PI) {
            int n = (int) (omega / TWO_PI);
            omega -= n * TWO_PI;
            omegaRevs += n;
        } else if (omega < 0) {
            int n = 1 + (int) (-omega / TWO_PI);
            omega += n * TWO_PI;
            omegaRevs -= n;
        }

        // EventManager.publish(Events.DEBUG_RAM, "Nu: " + (float) FastMath.toDegrees(getNuMod4Pi()) + ", Omega: " + (float) FastMath.toDegrees(getOmegaMod4Pi()));

        QuaternionDouble[] qAndRate = AttitudeConverter.heliotropicToQuaternions(lSun, super.getXiRef(), getNuMod4Pi(), getOmegaMod4Pi(), lSunDot, nuDot, omegaDot);

        return new ConcreteAttitude(tNow, qAndRate[0], qAndRate[1], true);
    }

    /**
     * Initializes quantities that only depend on the initial parameters. Must
     * be called whenever the scanning law parameters have been changed.
     */
    public void recomputeConstants() {

        xi = this.getXiRef();
        nu0 = this.getNuRef();
        double precRate = this.getTargetPrecessionRate();
        double sNom = NslUtil.calcSNom(xi, precRate);

        this.scanPerNs = this.getTargetScanPeriod();

        NslSun sun0 = new NslSun();
        sun0.setTime(getRefTime() * Nature.NS_TO_D);
        this.lSunRef = sun0.getSolarLongitude();

        sx = FastMath.sin(xi);
        cx = FastMath.cos(xi);
        sx2 = sx * sx;
        sx4 = sx2 * sx2;
        cx2 = cx * cx;
        cx3 = cx * cx2;
        cx4 = cx2 * cx2;
        cx6 = cx4 * cx2;

        s1a = FastMath.sin(nu0);
        c1a = FastMath.cos(nu0);
        s2a = 2 * c1a * s1a;
        c2a = 2 * c1a * c1a - 1;
        s3a = s2a * c1a + c2a * s1a;
        c3a = c2a * c1a - s2a * s1a;
        s4a = 2 * c2a * s2a;
        c4a = 2 * c2a * c2a - 1;
        c5a = c2a * c3a - s2a * s3a;
        double s1 = 1 / sNom;
        double s2 = s1 * s1;
        double s3 = s1 * s2;
        double s4 = s2 * s2;
        double s5 = s2 * s3;
        double s6 = s2 * s4;

        c0v = s1 * (cx * c1a) + s2 * ((1 - 2 * cx2) * s2a / 8) + s3 * (cx3 * c1a / 4 + (cx - cx3) * c3a / 12) + s4 * ((1 - 2 * cx4) * s2a / 16 + (3 - 12 * cx2 + 8 * cx4) * s4a / 256);

        c1v = s1 * (-cx * c1a) + s2 * (cx2 * s2a / 2) + s3 * (cx * c1a / 16 + cx * c1a / 16 - (cx - 4 * cx3) * c3a / 16) + s4 * (-cx2 * s2a / 96 + cx4 * s2a / 8 + (7 * cx2 - 12 * cx4) * s4a / 96);

        s1v = s1 * (cx * s1a) + s2 * (cx2 / 2 + cx2 * c2a / 2) + s3 * ((cx - 4 * cx3) * s3a / 16) + s4 * (-(cx2 - 4 * cx4) / 16 + cx2 * c2a / 96 + cx4 * c2a / 8 + (7 * cx2 - 12 * cx4) * c4a / 96);

        c2v = s2 * (-(1 + 2 * cx2) * s2a / 8) + s3 * (-(cx + 2 * cx3) * c1a / 8 - (cx + 2 * cx3) * c3a / 8) + s4 * (-(3 - 8 * cx2 - 6 * cx4) * s2a / 48 - (1 - 4 * cx2 - 12 * cx4) * s4a / 64);

        s2v = s2 * (-(1 + 2 * cx2) * c2a / 8) + s3 * ((cx + 2 * cx3) * s1a / 8 + (cx + 2 * cx3) * s3a / 8) + s4 * ((1 + 4 * cx2 + 4 * cx4) / 64 - (3 - 8 * cx2 - 6 * cx4) * c2a / 48 - (1 - 4 * cx2 - 12 * cx4) * c4a / 64);

        c3v = s3 * ((5 * cx + 4 * cx3) * c3a / 48) + s4 * (-(5 * cx2 + 4 * cx4) * s2a / 32 - (5 * cx2 + 4 * cx4) * s4a / 32);
        s3v = s3 * (-(5 * cx + 4 * cx3) * s3a / 48) + s4 * (-(5 * cx2 + 4 * cx4) * c2a / 32 - (5 * cx2 + 4 * cx4) * c4a / 32);
        c4v = s4 * ((3 + 52 * cx2 + 24 * cx4) * s4a / 768);

        s4v = s4 * ((3 + 52 * cx2 + 24 * cx4) * c4a / 768);

        on = cx * (-1 + 0.5 * s2 * sx2 * (1 + s2 * (1 + 3 * cx2) / 4 + s4 * (1 + 2 * cx2 + 5 * cx4) / 8 + s6 * (5 + 9 * cx2 + 15 * cx4 + 35 * cx6) / 64));

        ocn = s1 * sx2 * (1 + s2 * (1 + 6 * cx2) / 8 + s4 * (3 + 12 * cx2 + 40 * cx4) / 64);

        os2n = -s2 * sx2 * cx * (1 + s2 * cx2) / 4;

        oc3n = s3 * sx2 * (1 - 2 * cx2 + s2 * (9 + 12 * cx2 - 40 * cx4) / 16) / 24;

        os4n = -s4 * sx4 * cx / 32;

        oc5n = s5 * sx2 * (3 - 12 * cx2 + 8 * cx4) / 640;

    }

    /**
     * Returns the argument modulo 4*pi [rad]
     *
     * @param angle angle [rad]
     *
     * @return angle modulo 4*PI
     */
    public double modFourPi(double angle) {
        double rev = FastMath.floor(angle / FOUR_PI);
        return angle - FOUR_PI * rev;
    }

    /**
     * @return full precession angle [rad]
     */
    public double getNuFull() {
        return nu + nuRevs * TWO_PI;
    }

    /**
     * @return precession angle modulo 2 Pi
     */
    public double getNuMod4Pi() {
        return nu + (nuRevs % 2) * TWO_PI;
    }

    /**
     * @return full spin angle [rad]
     */
    public double getOmegaFull() {
        return omega + omegaRevs * TWO_PI;
    }

    /**
     * @return number of revolutions around the spin axis
     */
    public int getOmegaRevs() {
        return omegaRevs;
    }

    /**
     * @return spin angle modulo 4 Pi
     */
    public double getOmegaMod4Pi() {
        return omega + (omegaRevs % 2) * TWO_PI;
    }

    /**
     * @return longitude of sun [rad]
     */
    public double getLSunRef() {
        return this.lSunRef;
    }

    /**
     * @return time derivative of spin angle
     */
    public double getOmegaDot1() {
        return omegaDot;
    }

}
