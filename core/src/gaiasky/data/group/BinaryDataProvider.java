/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.GlobalConf;
import gaiasky.util.I18n;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads arrays of star beans from binary files, usually to go in an octree.
 *
 * @author tsagrista
 */
public class BinaryDataProvider extends AbstractStarGroupDataProvider {

    public BinaryDataProvider() {
        super();
    }

    @Override
    public List<ParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.bundle.format("notif.datafile", file));
        loadDataMapped(file, factor);
        logger.info(I18n.bundle.format("notif.nodeloader", list.size(), file));

        return list;
    }

    @Override
    public List<ParticleRecord> loadData(InputStream is, double factor) {
        list = readData(is);
        return list;
    }

    public void writeData(List<ParticleRecord> data, OutputStream out) {
        writeData(data, out, 2);
    }

    public void writeData(List<ParticleRecord> data, OutputStream out, int version) {
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);
        try {
            if (version >= 2) {
                // In new version, write token as negative int. Version afterwards
                data_out.writeInt(-1);
                data_out.writeInt(version);
            }
            // Number of stars
            data_out.writeInt(data.size());
            for (ParticleRecord sb : data) {
                writeParticleRecord(sb, data_out, version);
            }

        } catch (Exception e) {
            logger.error(e);
        } finally {
            try {
                data_out.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

    }

    /**
     * Write the star bean to the output stream
     *
     * @param sb      The star bean
     * @param out     The output stream
     * @param version Format version number (0 - nine doubles, four floats, one int with tycho ids, 1 - same, no tycho ids, 2 - six doubles, seven floats, one int)
     * @throws IOException
     */
    protected void writeParticleRecord(ParticleRecord sb, DataOutputStream out, int version) throws IOException {
        int ds = version < 2 ? 9 : ParticleRecord.STAR_SIZE_D;
        int fs = version < 2 ? 4 : ParticleRecord.STAR_SIZE_F;
        // Double
        for (int i = 0; i < ds; i++) {
            out.writeDouble(sb.dataD[i]);
        }
        // Float
        for (int i = 0; i < fs; i++) {
            out.writeFloat(sb.dataF[i]);
        }
        // Int
        out.writeInt((int) sb.dataF[ParticleRecord.I_FHIP]);

        if (version == 0) {
            // 3 integers, keep compatibility
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }

        // Long
        out.writeLong(sb.id);

        String namesConcat = sb.namesConcat();
        out.writeInt(namesConcat.length());
        out.writeChars(namesConcat);
    }

    public List<ParticleRecord> readData(InputStream in) {
        List<ParticleRecord> data = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            int version = 1;
            data_in.mark(0);
            int versionToken = data_in.readInt();
            if (versionToken < 0) {
                version = data_in.readInt();
            } else {
                // Rewind
                data_in.reset();
            }
            // Read size of stars
            int size = data_in.readInt();
            data = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                data.add(readParticleRecord(data_in, version));
            }

        } catch (IOException e) {
            logger.error(e);
        } finally {
            try {
                data_in.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }

        return data;
    }

    /**
     * Read a star bean from input stream
     *
     * @param in      Input stream
     * @param version Format version number (0 - nine doubles, four floats, one int with tycho ids, 1 - same, no tycho ids, 2 - six doubles, seven floats, one int)
     * @return The star bean
     * @throws IOException
     */
    protected ParticleRecord readParticleRecord(DataInputStream in, int version) throws IOException {
        int ds = version < 2 ? 9 : ParticleRecord.STAR_SIZE_D;
        int fs = version < 2 ? 4 : ParticleRecord.STAR_SIZE_F;

        double[] dataD = new double[ds];
        float[] dataF = new float[fs];
        // Double
        for (int i = 0; i < dataD.length; i++) {
            dataD[i] = in.readDouble();
            if (i < 6)
                dataD[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Float
        for (int i = 0; i < dataF.length - 1; i++) {
            dataF[i] = in.readFloat();
            if (i == ParticleRecord.I_FSIZE)
                dataF[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Int
        dataF[ParticleRecord.I_FHIP] = in.readInt();

        if (version == 0) {
            // Skip unused tycho numbers, 3 Integers
            in.readInt();
            in.readInt();
            in.readInt();
        }

        Long id = in.readLong();
        int nameLength = in.readInt();
        StringBuilder namesConcat = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            namesConcat.append(in.readChar());
        String[] names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        return new ParticleRecord(dataD, dataF, id, names);
    }

    @Override
    public List<ParticleRecord> loadDataMapped(String file, double factor) {
        try {
            FileChannel fc = new RandomAccessFile(GlobalConf.data.dataFile(file), "r").getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            int version = 1;
            mem.mark();
            int versionToken = mem.getInt();
            if (versionToken < 0) {
                version = mem.getInt();
            } else {
                // Rewind
                mem.reset();
            }
            // Read size of stars
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(readParticleRecord(mem, factor, version));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    /**
     * Loads data mapped with a version hint.
     * @param file The file to load
     * @param factor Distance factor, if any
     * @param versionHint Data version number, in case of version 0 or 1, since these formats were
     *                    not annotated. If version >=2, it is read from the data themselves
     * @return
     */
    public List<ParticleRecord> loadDataMapped(String file, double factor, int versionHint) {
        try {
            FileChannel fc = new RandomAccessFile(GlobalConf.data.dataFile(file), "r").getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            int version = versionHint;
            mem.mark();
            int versionToken = mem.getInt();
            if (versionToken < 0) {
                version = mem.getInt();
            } else {
                // Rewind
                mem.reset();
            }
            // Read size of stars
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(readParticleRecord(mem, factor, version));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor, int version) {
        int ds = version < 2 ? 9 : ParticleRecord.STAR_SIZE_D;
        int fs = version < 2 ? 4 : ParticleRecord.STAR_SIZE_F;

        double[] dataD = new double[ds];
        float[] dataF = new float[fs];
        // Double
        for (int i = 0; i < dataD.length; i++) {
            dataD[i] = mem.getDouble();
            if (i < 3)
                dataD[i] *= factor;
            if (i < 6)
                dataD[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Float
        for (int i = 0; i < dataF.length - 1; i++) {
            dataF[i] = mem.getFloat();
            if (i == ParticleRecord.I_FSIZE)
                dataF[i] *= Constants.DISTANCE_SCALE_FACTOR;
        }
        // Int
        dataF[ParticleRecord.I_FHIP] = mem.getInt();

        if (version == 0) {
            // Skip unused tycho numbers, 3 Integers
            mem.getInt();
            mem.getInt();
            mem.getInt();
        }

        Long id = mem.getLong();
        int nameLength = mem.getInt();
        StringBuilder namesConcat = new StringBuilder();
        for (int i = 0; i < nameLength; i++)
            namesConcat.append(mem.getChar());
        String[] names = namesConcat.toString().split(Constants.nameSeparatorRegex);

        return new ParticleRecord(dataD, dataF, id, names);
    }

}
