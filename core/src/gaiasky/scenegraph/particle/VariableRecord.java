package gaiasky.scenegraph.particle;

import gaiasky.util.ObjectDoubleMap;
import gaiasky.util.ucd.UCD;

/**
 * A record that holds a variable star.
 */
public class VariableRecord extends ParticleRecord {

    public float[] magnitudes;
    public int nMagnitudes;

    public VariableRecord(double[] dataD, float[] dataF, float[] magnitudes, int nMagnitudes, Long id, String[] names, ObjectDoubleMap<UCD> extra) {
        super(dataD, dataF, id, names, extra);
        this.magnitudes = magnitudes;
        this.nMagnitudes = nMagnitudes;
    }

    public float sizes(int i) {
        assert i < nMagnitudes : "Size out of bounds";
        return magnitudes[i];
    }

}
