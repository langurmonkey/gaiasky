package gaiasky.data.group;

import gaiasky.scene.record.ParticleRecord;
import gaiasky.util.Constants;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Base implementation of binary version, which accommodates most versions.
 */
public abstract class BinaryIOBase implements BinaryIO {
    protected final int nDoubles;
    protected final int nFloats;

    protected boolean tychoIds;

    protected BinaryIOBase(int nDoubles, int nFloats, boolean tychoIds) {
        this.tychoIds = tychoIds;
        this.nDoubles = nDoubles;
        this.nFloats = nFloats;
    }

    @Override
    public ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor) {
        double[] dataD = new double[ParticleRecord.STAR_SIZE_D];
        float[] dataF = new float[ParticleRecord.STAR_SIZE_F];
        int floatOffset = 0;
        // Double
        for (int i = 0; i < nDoubles; i++) {
            if (i < ParticleRecord.STAR_SIZE_D) {
                // Goes to double array
                dataD[i] = mem.getDouble();
                dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
            } else {
                // Goes to float array
                int idx = i - ParticleRecord.STAR_SIZE_D;
                dataF[idx] = (float) mem.getDouble();
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = mem.getFloat();
            if (idx == ParticleRecord.I_FSIZE)
                dataF[idx] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // HIP
        dataF[ParticleRecord.I_FHIP] = mem.getInt();

        // TYCHO
        if (tychoIds) {
            // Skip unused tycho numbers, 3 Integers
            mem.getInt();
            mem.getInt();
            mem.getInt();
        }

        // ID
        Long id = mem.getLong();

        // NAME
        int nameLength = mem.getInt();
        String[] names;
        if (nameLength == 0) {
            names = new String[] { id.toString() };
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(mem.getChar());
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        return new ParticleRecord(dataD, dataF, id, names);
    }


    @Override
    public ParticleRecord readParticleRecord(DataInputStream in, double factor) throws IOException {
        double[] dataD = new double[ParticleRecord.STAR_SIZE_D];
        float[] dataF = new float[ParticleRecord.STAR_SIZE_F];
        int floatOffset = 0;
        // Double
        for (int i = 0; i < nDoubles; i++) {
            if (i < ParticleRecord.STAR_SIZE_D) {
                // Goes to double array
                dataD[i] = in.readDouble();
                dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
            } else {
                // Goes to float array
                int idx = i - ParticleRecord.STAR_SIZE_D;
                dataF[idx] = (float) in.readDouble();
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            if (idx == ParticleRecord.I_FSIZE)
                dataF[idx] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // HIP
        dataF[ParticleRecord.I_FHIP] = in.readInt();

        // TYCHO
        if (tychoIds) {
            // Skip unused tycho numbers, 3 Integers
            in.readInt();
            in.readInt();
            in.readInt();
        }

        // ID
        Long id = in.readLong();

        // NAME
        int nameLength = in.readInt();
        String[] names;
        if (nameLength == 0) {
            names = new String[] { id.toString() };
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(in.readChar());
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        return new ParticleRecord(dataD, dataF, id, names);
    }

}
