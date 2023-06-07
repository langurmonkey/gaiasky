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

    public VariableRecord(ParticleRecordType type) {
        super(type);
    }
    public VariableRecord() {
        this(ParticleRecordType.STAR);
    }

    public VariableRecord(double[] dataD,
                          float[] dataF,
                          int nVari,
                          double period,
                          float[] variMags,
                          double[] variTimes,
                          Long id,
                          String[] names,
                          ObjectDoubleMap<UCD> extra) {
        super(ParticleRecordType.STAR, dataD, dataF, id, names, extra);
        this.nVari = nVari;
        this.variMags = variMags;
        this.variTimes = variTimes;
        this.period = period;
    }

    public void setNVari(int nVari) {
        this.nVari = nVari;
    }

    public void setVariMags(float[] variMags) {
        this.variMags = variMags;
    }

    public float variMag(int i) {
        assert i < nVari : "Size out of bounds";
        return variMags[i];
    }

    public void setVariTimes(double[] variTimes) {
        this.variTimes = variTimes;
    }

    public double variTime(int i) {
        assert i < nVari : "Size out of bounds";
        return variTimes[i];
    }

    public void setPeriod(double period) {
        this.period = period;
    }

}
