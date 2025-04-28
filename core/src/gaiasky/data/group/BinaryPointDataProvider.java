/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.BinaryIO;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.data.group.reader.IDataReader;
import gaiasky.data.group.reader.InputStreamDataReader;
import gaiasky.data.group.reader.MappedBufferDataReader;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.Particle;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads and writes binary data into particle groups ({@link ParticleType#PARTICLE_EXT}).
 * The format is:
 *
 * <ul>
 *     <li>int (4 bytes) -- number of records</li>
 *     <li>byte (1 byte [0|1]) -- particle record type: true - PARTICLE_EXT, false - PARTICLE</li>
 *     <li>for each record:
 *     <ul>
 *         <li>long (8 bytes) -- id</li>
 *         <li>short (2 bytes) -- nameLength</li>
 *         <li>char (2 bytes) * nameLength -- name characters</li>
 *         <li>double (8 bytes) -- alpha [deg]</li>
 *         <li>double (8 bytes) -- delta [deg]</li>
 *         <li>double (8 bytes) -- distance [pc]</li>
 *         <li>if type == PARTICLE_EXT
 *         <ul>
 *             <li>float (4 bytes) -- mu alpha</li>
 *             <li>float (4 bytes) -- mu delta</li>
 *             <li>float (4 bytes) -- radial velocity</li>
 *             <li>float (4 bytes) -- apparent magnitude</li>
 *             <li>float (4 bytes) -- packed color</li>
 *             <li>float (4 bytes) -- size</li>
 *         </ul>
 *         </li>
 *     </ul>
 *     </li>
 * </ul>
 */
public class BinaryPointDataProvider implements IParticleGroupDataProvider, BinaryIO {
    protected static Logger.Log logger = Logger.getLogger(BinaryPointDataProvider.class);

    private int particleNumberCap = -1;
    protected List<IParticleRecord> list;
    /** Whether to use PARTICLE_EXT or PARTICLE. **/
    private final AtomicBoolean extra = new AtomicBoolean();

    private final Vector3d aux3d1 = new Vector3d();
    private final Vector3d aux3d2 = new Vector3d();


    public BinaryPointDataProvider() {
    }

    public void setDatasetOptions(DatasetOptions datasetOptions) {
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

        try (DataInputStream data_in = new DataInputStream(in)) {
            try {
                data_in.mark(0);
                int size = data_in.readInt();
                boolean extra = data_in.readBoolean();
                this.extra.set(extra);

                data = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    if (particleNumberCap < 0 || i < particleNumberCap) {
                        data.add(readParticleRecord(data_in, factor));
                    }
                }

            } catch (IOException e) {
                logger.error(e);
            }
        } catch (IOException e) {
            logger.error(e);
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
            boolean extra = mem.get() == (byte) 1;
            this.extra.set(extra);

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
    public IParticleRecord readParticleRecord(MappedByteBuffer mem, double factor) throws IOException {
        return readParticleRecord(new MappedBufferDataReader(mem), factor);
    }

    @Override
    public IParticleRecord readParticleRecord(DataInputStream in, double factor) throws IOException {
        return readParticleRecord(new InputStreamDataReader(in), factor);
    }

    public IParticleRecord readParticleRecord(IDataReader in, double factor) throws IOException {
        var pType = extra.get() ? ParticleType.PARTICLE_EXT : ParticleType.PARTICLE;

        // ID
        long id = in.readLong();

        // NAME
        int nameLength = in.readInt();
        String[] names;
        if (nameLength == 0) {
            names = new String[]{Long.toString(id)};
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(in.readChar());
            names = namesConcat.toString()
                    .split(Constants.nameSeparatorRegex);
        }

        // XYZ
        final double alphaDeg = in.readDouble();
        final double deltaDeg = in.readDouble();
        final double distPc = in.readDouble();
        var pos = Coordinates.sphericalToCartesian(alphaDeg * MathUtilsDouble.degRad,
                                                   deltaDeg * MathUtilsDouble.degRad,
                                                   distPc * Constants.PC_TO_U,
                                                   aux3d1);
        pos.scl(factor);

        // PROPER MOTION
        float muAlpha = 0;
        float muDelta = 0;
        float radVel = 0;
        if (extra.get()) {
            muAlpha = in.readFloat();
            muDelta = in.readFloat();
            radVel = in.readFloat();
        }

        // VELOCITY VECTOR
        Vector3d velocityVector = AstroUtils.properMotionsToCartesian(muAlpha, muDelta, radVel, FastMath.toRadians(alphaDeg), FastMath.toRadians(deltaDeg),
                                                                      distPc, aux3d2);

        // MAGNITUDE
        float appMag;
        if (extra.get()) {
            appMag = in.readFloat();
        } else {
            appMag = (float) Constants.DEFAULT_MAG;
        }
        final float absMag = (float) AstroUtils.apparentToAbsoluteMagnitude(distPc, appMag);

        // COLOR
        float color;
        if (extra.get()) {
            color = in.readFloat();
        } else {
            color = Float.NaN;
        }

        // SIZE
        float size;
        if (extra.get()) {
            size = in.readFloat();
        } else {
            double sizePc = AstroUtils.absoluteMagnitudeToPseudoSize(absMag);
            size = (float) sizePc;
        }


        Particle p = null;
        switch (pType) {
            case PARTICLE -> {
                p = new Particle(id, names, pos.x, pos.y, pos.z);
            }
            case PARTICLE_EXT -> {
                p = new Particle(id, names, pos.x, pos.y, pos.z, muAlpha, muDelta, radVel, (float) velocityVector.x, (float) velocityVector.y,
                                 (float) velocityVector.z, appMag, absMag, size, color);
            }
        }

        return p;
    }


    public void writeData(List<IParticleRecord> data, OutputStream out, boolean extra) {
        // Wrap the FileOutputStream with a DataOutputStream
        try (DataOutputStream dataOut = new DataOutputStream(out)) {
            try {
                // Number of particles
                dataOut.writeInt(data.size());
                dataOut.writeBoolean(extra);
                this.extra.set(extra);
                for (IParticleRecord sb : data) {
                    writeParticleRecord(sb, dataOut);
                }

            } catch (Exception e) {
                logger.error(e);
            }
        } catch (IOException e) {
            logger.error(e);
        }

    }

    @Override
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // ID.
        out.writeLong(sb.id());

        // NAME.
        if (sb.names().length == 1 && sb.names()[0].equals(Long.toString(sb.id()))) {
            // If name equals ID, skip it.
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

        // POSITION.
        Vector3d cartPos = sb.pos(aux3d1);
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartPos, aux3d2);
        out.writeDouble(sphPos.x * MathUtilsDouble.radDeg);
        out.writeDouble(sphPos.y * MathUtilsDouble.radDeg);
        out.writeDouble(sb.distance() * Constants.U_TO_PC);

        if (extra.get()) {
            // PROPER MOTION.
            out.writeFloat(sb.mualpha());
            out.writeFloat(sb.mudelta());
            out.writeFloat(sb.radvel());

            // MAGNITUDE.
            out.writeFloat(sb.appMag());

            // COLOR.
            out.writeFloat(sb.col());

            // SIZE.
            out.writeFloat(sb.size());
        }

    }
}
