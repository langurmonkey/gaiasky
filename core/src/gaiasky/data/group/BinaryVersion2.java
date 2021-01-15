package gaiasky.data.group;

import gaiasky.scenegraph.particle.IParticleRecord;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Binary version 2, used in latter eDR3 runs.
 * Contains 6 doubles, 7 floats and 1 int (hip). It is more compact than version 0 and 1,
 * for only positions and velocity vectors are stored as doubles.
 */
public class BinaryVersion2 extends BinaryIOBase {

    protected BinaryVersion2() {
        super(6, 7, false);
    }

    @Override
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // 6 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());
        out.writeDouble(sb.pmx());
        out.writeDouble(sb.pmy());
        out.writeDouble(sb.pmz());

        // 7 floats
        out.writeFloat(sb.mualpha());
        out.writeFloat(sb.mudelta());
        out.writeFloat(sb.radvel());
        out.writeFloat(sb.appmag());
        out.writeFloat(sb.absmag());
        out.writeFloat(sb.col());
        out.writeFloat(sb.size());

        // HIP
        out.writeInt(sb.hip());

        // TYCHO
        if (tychoIds) {
            // 3 integers, keep compatibility
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }

        // ID
        out.writeLong(sb.id());

        // NAME
        String namesConcat = sb.namesConcat();
        out.writeInt(namesConcat.length());
        out.writeChars(namesConcat);
    }
}
