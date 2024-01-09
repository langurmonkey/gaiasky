/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.BinaryIO;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleRecord;
import gaiasky.scene.record.ParticleRecord.ParticleRecordType;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads and writes binary data into particle groups ({@link ParticleRecordType#PARTICLE_EXT}).
 * The format is:
 *
 * <ul>
 *     <li>int (4 bytes) -- number of records</li>
 *     <li>byte (1 byte [0|1]) -- has size</li>
 *     <li>byte (1 byte [0|1]) -- has packed color</li>
 *     <li>for each record:
 *     <ul>
 *         <li>long (8 bytes) -- id</li>
 *         <li>short (2 bytes) -- nameLength</li>
 *         <li>char (2 bytes) * nameLength -- name characters</li>
 *         <li>double (8 bytes) -- X</li>
 *         <li>double (8 bytes) -- Y</li>
 *         <li>double (8 bytes) -- Z</li>
 *         <li>float (4 bytes) -- size (if has size)</li>
 *         <li>float (4 bytes) -- packed color (if has color)</li>
 *
 *     </ul>
 *     </li>
 * </ul>
 */
public class BinaryPointDataProvider implements IParticleGroupDataProvider, BinaryIO {
    protected static Logger.Log logger = Logger.getLogger(BinaryPointDataProvider.class);

    private int particleNumberCap = -1;
    protected List<IParticleRecord> list;

    // State booleans used in loading and writing data.
    private boolean hasSize, hasColor;
    private boolean writeNames = true;

    // Dataset options, may be null
    private DatasetOptions datasetOptions;


    public BinaryPointDataProvider() {
    }

    public void setDatasetOptions(DatasetOptions datasetOptions) {
        this.datasetOptions = datasetOptions;
    }

    @Override
    public List<IParticleRecord> loadData(String file) {
        return loadData(file, 1.0);
    }

    @Override
    public List<IParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.msg("notif.datafile", file));
        loadDataMapped(file, factor);
        logger.info(I18n.msg("notif.nodeloader", list.size(), file));

        return list;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is, double factor) {
        list = readData(is, factor);
        return list;
    }

    public List<IParticleRecord> readData(InputStream in, double factor) {
        List<IParticleRecord> data = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            data_in.mark(0);
            int size = data_in.readInt();
            hasSize = data_in.readBoolean();
            hasColor = data_in.readBoolean();

            data = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (particleNumberCap < 0 || i < particleNumberCap) {
                    data.add(readParticleRecord(data_in, factor));
                }
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

    @Override
    public List<IParticleRecord> loadDataMapped(String file, double factor) {
        try (var raf = new RandomAccessFile(Settings.settings.data.dataFile(file), "r")) {
            FileChannel fc = raf.getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            mem.mark();
            int size = mem.getInt();
            hasSize = mem.get() == (byte) 1;
            hasColor = mem.get() == (byte) 1;

            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                if (particleNumberCap < 0 || i < particleNumberCap) {
                    list.add(readParticleRecord(mem, factor));
                }
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }


    @Override
    public void setFileNumberCap(int cap) {
        // Does not apply.
    }

    @Override
    public void setStarNumberCap(int cap) {
        this.particleNumberCap = cap;
    }

    @Override
    public void setProviderParams(Map<String, Object> params) {
        // Not used.
    }

    @Override
    public void setTransformMatrix(Matrix4d matrix) {
        // Not used.
    }

    @Override
    public ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor) {
        var pType = ((datasetOptions != null && datasetOptions.type != null) ?
                (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES_EXT ? ParticleRecordType.PARTICLE_EXT : ParticleRecordType.PARTICLE)
                : (hasColor || hasSize ? ParticleRecordType.PARTICLE_EXT : ParticleRecordType.PARTICLE));
        double[] dataD = new double[pType.doubleArraySize];
        float[] dataF = new float[pType.floatArraySize];

        // ID
        long id = mem.getLong();

        // NAME
        int nameLength = mem.getInt();
        String[] names;
        if (nameLength == 0) {
            names = new String[]{Long.toString(id)};
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(mem.getChar());
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        // XYZ
        var pos = new Vector3d(mem.getDouble(), mem.getDouble(), mem.getDouble());

        // MAGNITUDE
        final double distPc = pos.len() * Constants.U_TO_PC;
        final float appMag = (float) Constants.DEFAULT_MAG;
        final float absMag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, appMag);

        // SIZE
        float size;
        if (hasSize) {
            size = mem.getFloat();
        } else {
            double sizePc = AstroUtils.absoluteMagnitudeToPseudoSize(absMag);
            size = (float) sizePc;
        }

        // COLOR
        float color;
        if (hasColor) {
            color = mem.getFloat();
        } else {
            color = Float.NaN;
        }

        ParticleRecord particle = null;
        switch (pType) {
            case PARTICLE -> {
                particle = new ParticleRecord(pType);
                particle.setId(id);
                particle.setNames(names);
                particle.setPos(pos.x, pos.y, pos.z);
            }
            case PARTICLE_EXT -> {
                particle = new ParticleRecord(pType, dataD, dataF, id, names);
                particle.setMag(appMag, absMag);
                particle.setSize(size);
                particle.setCol(color);
            }
        }

        return particle;
    }

    @Override
    public ParticleRecord readParticleRecord(DataInputStream in, double factor) throws IOException {
        double[] dataD = new double[ParticleRecordType.PARTICLE_EXT.doubleArraySize];
        float[] dataF = new float[ParticleRecordType.PARTICLE_EXT.floatArraySize];

        // ID
        Long id = in.readLong();

        // NAME
        int nameLength = in.readInt();
        String[] names;
        if (nameLength == 0) {
            names = new String[]{id.toString()};
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(in.readChar());
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        // XYZ
        dataD[0] = in.readDouble();
        dataD[1] = in.readDouble();
        dataD[2] = in.readDouble();

        var particle = new ParticleRecord(ParticleRecordType.PARTICLE_EXT, dataD, dataF, id, names);

        // SIZE
        if (hasSize) {
            particle.setSize(in.readFloat());
        }

        // COLOR
        if (hasColor) {
            particle.setCol(in.readFloat());
        }

        return particle;
    }

    public void writeData(List<IParticleRecord> data, OutputStream out, boolean writeSizes, boolean writeColors, boolean writeNames) {
        // Wrap the FileOutputStream with a DataOutputStream
        DataOutputStream data_out = new DataOutputStream(out);
        try {
            // Number of particles
            data_out.writeInt(data.size());
            data_out.writeBoolean(writeSizes);
            data_out.writeBoolean(writeColors);
            this.hasSize = writeSizes;
            this.hasColor = writeColors;
            this.writeNames = writeNames;
            for (IParticleRecord sb : data) {
                writeParticleRecord(sb, data_out);
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

    @Override
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // ID.
        out.writeLong(sb.id());

        // NAME.
        if (!writeNames) {
            out.writeInt(0);
        } else {
            var namesConcat = sb.namesConcat();
            if (namesConcat == null || namesConcat.isEmpty()) {
                out.writeInt(0);
            } else {
                out.writeInt(namesConcat.length());
                out.writeChars(namesConcat);
            }
        }

        // XYZ
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());

        // SIZE.
        if (hasSize) {
            out.writeFloat(sb.size());
        }

        // COLOR.
        if (hasColor) {
            out.writeFloat(sb.col());
        }

    }
}
