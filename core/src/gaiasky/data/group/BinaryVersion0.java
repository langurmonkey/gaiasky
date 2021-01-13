package gaiasky.data.group;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Original binary version (0), used in DR1 and DR2.
 * Contains 9 doubles, 4 floats, 1 integer (hip), 3 integers for the tycho identifiers, a long (id) and the name.
 **/
public class BinaryVersion0 extends BinaryIOBase {

    public BinaryVersion0() {
        super(9, 4, true);
    }

    @Override
    public void writeParticleRecord(ParticleRecord sb, DataOutputStream out) throws IOException {
        // Double
        int floatOffset = 0;
        for (int i = 0; i < nDoubles; i++) {
            if (i < sb.dataD.length) {
                // Write from double
                out.writeDouble(sb.dataD[i]);
            } else {
                // Write from float
                int idx = i - sb.dataD.length;
                out.writeDouble(sb.dataF[idx]);
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            out.writeFloat(sb.dataF[idx]);
        }

        // HIP
        out.writeInt((int) sb.dataF[ParticleRecord.I_FHIP]);

        // TYCHO
        if (tychoIds) {
            // 3 integers, keep compatibility
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }

        // ID
        out.writeLong(sb.id);

        // NAME
        String namesConcat = sb.namesConcat();
        out.writeInt(namesConcat.length());
        out.writeChars(namesConcat);
    }

}
