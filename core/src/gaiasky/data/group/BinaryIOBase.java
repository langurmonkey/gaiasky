/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.BinaryIO;
import gaiasky.data.group.reader.IDataReader;
import gaiasky.data.group.reader.InputStreamDataReader;
import gaiasky.data.group.reader.MappedBufferDataReader;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleStar;
import gaiasky.util.Constants;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Implements the base data loading for binary versions 0, 1, and 2.
 */
public abstract class BinaryIOBase implements BinaryIO {
    protected final int nDoubles;
    protected final int nFloats;

    protected boolean hipId;
    protected boolean tychoIds;

    protected BinaryIOBase(int nDoubles,
                           int nFloats,
                           boolean hipId,
                           boolean tychoIds) {
        this.hipId = hipId;
        this.tychoIds = tychoIds;
        this.nDoubles = nDoubles;
        this.nFloats = nFloats;
    }

    @Override
    public IParticleRecord readParticleRecord(MappedByteBuffer mem,
                                              double factor) throws IOException {
        return readParticleRecord(new MappedBufferDataReader(mem), factor);
    }

    @Override
    public IParticleRecord readParticleRecord(DataInputStream in,
                                              double factor) throws IOException {
        return readParticleRecord(new InputStreamDataReader(in), factor);
    }

    public IParticleRecord readParticleRecord(IDataReader in,
                                              double factor) throws IOException {
        double[] dataD = new double[3];
        float[] dataF = new float[12];
        int floatOffset = 0;
        // Double
        for (int i = 0; i < nDoubles; i++) {
            if (i < dataD.length) {
                // Goes to double array
                dataD[i] = in.readDouble();
                dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
            } else {
                // Goes to float array
                int idx = i - dataD.length;
                dataF[idx] = (float) in.readDouble();
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            // Scale proper motions and size
            if (idx <= 2 || idx == 9)
                dataF[idx] *= (float) Constants.DISTANCE_SCALE_FACTOR;
        }
        // Version 2: we have the HIP number in the data file.
        if (hipId) {
            // HIP
            dataF[10] = in.readInt();
        }

        // TYCHO
        if (tychoIds) {
            // Skip unused tycho numbers, 3 Integers
            in.readInt();
            in.readInt();
            in.readInt();
        }

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

        return new ParticleStar(id, names, dataD[0], dataD[1], dataD[2], dataF[3], dataF[4], dataF[5], dataF[0], dataF[1], dataF[2], dataF[6],
                                dataF[7], dataF[8], dataF[9], (int) dataF[10], dataF[11], null);
    }
}
