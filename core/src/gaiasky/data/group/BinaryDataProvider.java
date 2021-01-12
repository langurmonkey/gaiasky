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

    private static final int COMPATIBILITY_INT_MARKER = 12343210;
    private static final char COMPATIBILITY_CHAR_TRUE = 't';
    private static final char COMPATIBILITY_CHAR_FALSE = 'f';

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
        writeData(data, out, false);
    }

    public void writeData(List<ParticleRecord> data, OutputStream out, boolean compatibility) {
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);
        try {
            // In new version, write compatibility mode
            // The readers will check for this int, and use the next char as
            // compatibility ('t' or 'f'). If the int is not there, compat is set to true.
            data_out.writeInt(COMPATIBILITY_INT_MARKER);
            data_out.writeChar(COMPATIBILITY_CHAR_FALSE);
            // Size of stars
            data_out.writeInt(data.size());
            for (ParticleRecord sb : data) {
                writeParticleRecord(sb, data_out, compatibility);
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
     * @param sb     The star bean
     * @param out    The output stream
     * @param compatibility Use compatibility with DR1/DR2 model (with tycho ids)
     * @throws IOException
     */
    protected void writeParticleRecord(ParticleRecord sb, DataOutputStream out, boolean compatibility) throws IOException {
        // Double
        for (int i = 0; i < sb.dataD.length; i++) {
            out.writeDouble(sb.dataD[i]);
        }
        // Float
        for (int i = 0; i < sb.dataF.length - 1; i++) {
            out.writeFloat(sb.dataF[i]);
        }
        // Int
        out.writeInt((int) sb.dataF[ParticleRecord.I_FHIP]);

        if (compatibility) {
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
            boolean compat = true;
            data_in.mark(0);
            int markerInt = data_in.readInt();
            if(markerInt == COMPATIBILITY_INT_MARKER){
               compat = data_in.readChar() == COMPATIBILITY_CHAR_TRUE;
            } else {
                // Rewind
                data_in.reset();
            }
            // Read size of stars
            int size = data_in.readInt();
            data = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                data.add(readParticleRecord(data_in, compat));
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
     * @param in     Input stream
     * @param compat Use compatibility with DR1/DR2 model (with tycho ids)
     * @return The star bean
     * @throws IOException
     */
    protected ParticleRecord readParticleRecord(DataInputStream in, boolean compat) throws IOException {
        double[] dataD = new double[ParticleRecord.STAR_SIZE_D];
        float[] dataF = new float[ParticleRecord.STAR_SIZE_F];
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

        if (compat) {
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

            boolean compat = false;
            mem.mark();
            int markerInt = mem.getInt();
            if(markerInt == COMPATIBILITY_INT_MARKER){
                compat = mem.getChar() == COMPATIBILITY_CHAR_TRUE;
            } else {
                // Rewind
                mem.reset();
            }
            // Read size of stars
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(readParticleRecord(mem, factor, compat));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    public ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor, boolean compat) {
        double[] dataD = new double[ParticleRecord.STAR_SIZE_D];
        float[] dataF = new float[ParticleRecord.STAR_SIZE_F];
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

        if (compat) {
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
