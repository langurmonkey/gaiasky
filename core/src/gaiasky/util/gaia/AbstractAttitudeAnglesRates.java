/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia;

public abstract class AbstractAttitudeAnglesRates {
    // 2-d double array to hold the angles and rates - units are unspecified at this level
    protected double[][] anglesRates = new double[3][2];

    private void setAngle(int i, double angle) {
        anglesRates[i][0] = angle;
    }

    private void setRate(int i, double rate) {
        anglesRates[i][1] = rate;
    }

    /** Set first angle and/or rate */
    public void setFirstAngle(double angle) {
        setAngle(0, angle);
    }

    public void setFirstRate(double rate) {
        setRate(0, rate);
    }

    public void setFirstPair(final double angle, final double rate) {
        setFirstAngle(angle);
        setFirstRate(rate);
    }

    /** set second angle and/or rate value */
    public void setSecondAngle(double angle) {
        setAngle(1, angle);
    }

    public void setSecondRate(double rate) {
        setRate(1, rate);
    }

    public void setSecondPair(double angle, double rate) {
        setSecondAngle(angle);
        setSecondRate(rate);
    }

    /** set third angle and/or rate value */
    public void setThirdAngle(double angle) {
        setAngle(2, angle);
    }

    public void setThirdRate(double rate) {
        setRate(2, rate);
    }

    public void setThirdPair(double angle, double rate) {
        setThirdAngle(angle);
        setThirdRate(rate);
    }
}
