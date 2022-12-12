/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

/*
 * GaiaTools
 * Copyright (C) 2006 Gaia Data Processing and Analysis Consortium
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package gaiasky.util.gaia;

/**
 * A little helper class to hold a set of three angles and associated rates.
 *
 * @author Uwe Lammers
 * @version $Id: AbstractAttitudeAnglesRates.java 308373 2013-08-02 16:01:12Z ulammers $
 */
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
    @SuppressWarnings("javadoc")
    public void setFirstAngle(double angle) {
        setAngle(0, angle);
    }

    @SuppressWarnings("javadoc")
    public void setFirstRate(double rate) {
        setRate(0, rate);
    }

    @SuppressWarnings("javadoc")
    public void setFirstPair(final double angle, final double rate) {
        setFirstAngle(angle);
        setFirstRate(rate);
    }

    /** set second angle and/or rate value */
    @SuppressWarnings("javadoc")
    public void setSecondAngle(double angle) {
        setAngle(1, angle);
    }

    @SuppressWarnings("javadoc")
    public void setSecondRate(double rate) {
        setRate(1, rate);
    }

    @SuppressWarnings("javadoc")
    public void setSecondPair(double angle, double rate) {
        setSecondAngle(angle);
        setSecondRate(rate);
    }

    /** set third angle and/or rate value */
    @SuppressWarnings("javadoc")
    public void setThirdAngle(double angle) {
        setAngle(2, angle);
    }

    @SuppressWarnings("javadoc")
    public void setThirdRate(double rate) {
        setRate(2, rate);
    }

    @SuppressWarnings("javadoc")
    public void setThirdPair(double angle, double rate) {
        setThirdAngle(angle);
        setThirdRate(rate);
    }
}
