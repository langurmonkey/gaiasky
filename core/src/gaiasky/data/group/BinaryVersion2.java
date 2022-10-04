package gaiasky.data.group;

import gaiasky.scene.api.IParticleRecord;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Binary version 2, used in latter eDR3 runs, and DR3+.
 * Contains 6 doubles, 7 floats and 1 int (hip). It is more compact than version 0 and 1,
 * for only positions and velocity vectors are stored as doubles.
 */
public class BinaryVersion2 extends BinaryIOBase {

    protected BinaryVersion2() {
        super(3, 10, false);
    }

    @Override
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // 3 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());

        // 10 floats
        out.writeFloat((float) sb.pmx());
        out.writeFloat((float) sb.pmy());
        out.writeFloat((float) sb.pmz());
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
        if (namesConcat == null || namesConcat.length() == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(namesConcat.length());
            out.writeChars(namesConcat);
        }
    }
}
