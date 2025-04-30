/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.BinaryIO;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads binary files using the infrastructure under {@link BinaryIO}. The format includes a header token
 * and a version number. Depending on the version number a different loader is activated.
 */
public class BinaryDataProvider extends AbstractStarGroupDataProvider {
    static {
        logger = Logger.getLogger(BinaryDataProvider.class);
    }

    /** The default output format version to use for writing **/
    public static int DEFAULT_OUTPUT_VERSION = 3;

    public static int MIN_OUTPUT_VERSION = 0;
    public static int MAX_OUTPUT_VERSION = DEFAULT_OUTPUT_VERSION;
    /**
     * Binary IO for the different format versions
     */
    private final BinaryIO[] binaryVersions;
    /** The output format version for writing **/
    private int outputVersion = -1;

    public BinaryDataProvider() {
        super();

        binaryVersions = new BinaryIO[4];
        binaryVersions[0] = new BinaryVersion0();
        binaryVersions[1] = new BinaryVersion1();
        binaryVersions[2] = new BinaryVersion2();
        binaryVersions[3] = new BinaryVersion3();
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

    public void writeData(List<IParticleRecord> data, OutputStream out) {
        int version = (outputVersion < MIN_OUTPUT_VERSION || outputVersion > MAX_OUTPUT_VERSION) ? DEFAULT_OUTPUT_VERSION : outputVersion;
        writeData(data, out, version);
    }

    public void writeData(List<IParticleRecord> data, OutputStream out, int version) {
        // Wrap the FileOutputStream with a DataOutputStream.
        try (DataOutputStream data_out = new DataOutputStream(out)) {
            try {
                if (version >= 2) {
                    // In new version, write token as negative int. version afterward.
                    data_out.writeInt(-1);
                    data_out.writeInt(version);
                }
                // Number of stars.
                data_out.writeInt(data.size());
                for (IParticleRecord sb : data) {
                    binaryVersions[version].writeParticleRecord(sb, data_out);
                }

            } catch (Exception e) {
                logger.error(e);
            }
        } catch (IOException e) {
            logger.error(e);
        }

    }

    public List<IParticleRecord> readData(InputStream in, double factor) {
        List<IParticleRecord> data = null;
        DataInputStream data_in = new DataInputStream(in);

        try {
            int version = 1;
            data_in.mark(0);
            int versionToken = data_in.readInt();
            if (versionToken < 0) {
                version = data_in.readInt();
            } else {
                // Rewind.
                data_in.reset();
            }
            // Read size of stars.
            int size = data_in.readInt();
            data = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                data.add(binaryVersions[version].readParticleRecord(data_in, factor));
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

            int version = 1;
            mem.mark();
            int versionToken = mem.getInt();
            if (versionToken < 0) {
                version = mem.getInt();
            } else {
                // Rewind.
                mem.reset();
            }
            // Read size of stars.
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(binaryVersions[version].readParticleRecord(mem, factor));
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
     *
     * @param file        The file to load
     * @param factor      Distance factor, if any
     * @param versionHint Data version number, in case of version 0 or 1, since these formats were
     *                    not annotated. If version >=2, the version number is read from the file header
     *
     * @return The list of particle records.
     */
    public List<IParticleRecord> loadDataMapped(String file, double factor, int versionHint) {
        try (var raf = new RandomAccessFile(Settings.settings.data.dataFile(file), "r")) {
            FileChannel fc = raf.getChannel();

            MappedByteBuffer mem = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            int version = versionHint;
            mem.mark();
            int versionToken = mem.getInt();
            if (versionToken < 0) {
                version = mem.getInt();
            } else {
                // Rewind.
                mem.reset();
            }
            // Read size of stars.
            int size = mem.getInt();
            list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(binaryVersions[version].readParticleRecord(mem, factor));
            }

            fc.close();

            return list;

        } catch (Exception e) {
            logger.error(e);
        }
        return null;
    }

    @Override
    public void setOutputFormatVersion(int version) {
        this.outputVersion = version;
    }
}
