/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

public class RungeKuttaNs {

    /**
     * Returns the value of y at a given value of x (<b>xn</b>) for a set of
     * ordinary differential equation (ODEs)<br>
     * <p>
     * See <a href="http://www.ee.ucl.ac.uk/~mflanaga/java/RungeKutta.html#four">
     * http://www.ee.ucl.ac.uk/~mflanaga/java/RungeKutta.html#four</a>
     *
     * @param g        an implementation of DiffnFunction describing a set of
     *                 Ordinary Differential Equations
     * @param tOld     the initial value for t
     * @param y0       the initial value for y
     * @param tNew     the value of t where we want to know y
     * @param tStepMax the initial step size
     * @param tUnit    the time unit used for the derivatives
     *
     * @return the value of y at xn
     */
    public static double[] fourthOrder(DiffnFunctionNs g, long tOld, double[] y0, long tNew, long tStepMax, long tUnit) {
        final double half = 1.0 / 2.0;
        final double third = 1.0 / 3.0;
        final double sixth = 1.0 / 6.0;
        int nEqs = y0.length;
        double[] k1 = new double[nEqs];
        double[] k2 = new double[nEqs];
        double[] k3 = new double[nEqs];
        double k4;
        double[] y = new double[nEqs];
        double[] yd = new double[nEqs];
        double[] dydt = new double[nEqs];
        long tBeg, tMid, tEnd;

        // the time interval [tOld, tNew] will be covered in steps that are at
        // most of length tMaxStep. If tNew == tOld then just return y0;
        long dtNs = tNew - tOld;
        if (dtNs == 0L) {
            double[] ret = new double[y0.length];
            System.arraycopy(y0, 0, ret, 0, y0.length);
            return ret;
        }

        // Calculate the required number of steps, nStep (must be at least 1),
        // and the length of each step:
        long stepNs = tStepMax;
        int nStep = (int) Math.rint((double) dtNs / (double) stepNs);
        if (nStep < 1) {
            nStep = 1;
        }
        // step length in ns:
        stepNs = dtNs / nStep;
        // step expressed in the time unit used for derivatives:
        double step = (double) stepNs / (double) tUnit;

        // initialize
        System.arraycopy(y0, 0, y, 0, nEqs);

        // iteration over the allowed steps. In each step a sub-interval from
        // tBeg to tEnd is covered:
        tBeg = tOld;
        for (int j = 0; j < nStep; j++) {
            tMid = tBeg + stepNs / 2;
            if (j < nStep - 1) {
                tEnd = tBeg + stepNs;
            } else {
                // ensure that the last sub-interval ends exactly at tNew
                tEnd = tNew;
            }
            dydt = g.derivn(tBeg, y);
            for (int i = 0; i < nEqs; i++) {
                k1[i] = step * dydt[i];
                yd[i] = y[i] + k1[i] * half;
            }
            dydt = g.derivn(tMid, yd);

            for (int i = 0; i < nEqs; i++) {
                k2[i] = step * dydt[i];
                yd[i] = y[i] + k2[i] * half;
            }
            dydt = g.derivn(tMid, yd);
            for (int i = 0; i < nEqs; i++) {
                k3[i] = step * dydt[i];
                yd[i] = y[i] + k3[i];
            }
            dydt = g.derivn(tEnd, yd);
            for (int i = 0; i < nEqs; i++) {
                k4 = step * dydt[i];
                y[i] += (k1[i] + k4) * sixth + (k2[i] + k3[i]) * third;
            }

            // this is for the next step:
            tBeg = tEnd;
        }

        return y;
    }

}
