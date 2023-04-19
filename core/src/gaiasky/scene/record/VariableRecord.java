/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.util.ObjectDoubleMap;
import gaiasky.util.ucd.UCD;

public class VariableRecord extends ParticleRecord {

    public int nVari;
    public float[] variMags;
    public double[] variTimes;
    public double period;

    public VariableRecord(double[] dataD, float[] dataF, int nVari, double period, float[] variMags, double[] variTimes, Long id, String[] names, ObjectDoubleMap<UCD> extra) {
        super(dataD, dataF, id, names, extra);
        this.nVari = nVari;
        this.variMags = variMags;
        this.variTimes = variTimes;
        this.period = period;
    }

    public float variMag(int i) {
        assert i < nVari : "Size out of bounds";
        return variMags[i];
    }

    public double variTime(int i) {
        assert i < nVari : "Size out of bounds";
        return variTimes[i];
    }

}
