package gaiasky.scene.record;

import gaiasky.util.ObjectDoubleMap;
import gaiasky.util.ucd.UCD;

/**
 * A record that holds a variable star.
 */
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
