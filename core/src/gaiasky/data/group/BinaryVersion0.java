package gaiasky.data.group;

import gaiasky.scene.api.IParticleRecord;

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
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // 9 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());
        out.writeDouble(sb.pmx());
        out.writeDouble(sb.pmy());
        out.writeDouble(sb.pmz());
        out.writeDouble(sb.mualpha());
        out.writeDouble(sb.mudelta());
        out.writeDouble(sb.radvel());

        // 4 floats
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
