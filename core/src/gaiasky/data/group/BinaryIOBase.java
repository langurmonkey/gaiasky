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
import gaiasky.scene.record.Particle;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.Constants;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

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
        double[] dataD = new double[ParticleType.STAR.doubleArraySize];
        float[] dataF = new float[ParticleType.STAR.floatArraySize];
        int floatOffset = 0;
        // Double
        for (int i = 0; i < nDoubles; i++) {
            if (i < ParticleType.STAR.doubleArraySize) {
                // Goes to double array
                dataD[i] = in.readDouble();
                dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
            } else {
                // Goes to float array
                int idx = i - ParticleType.STAR.doubleArraySize;
                dataF[idx] = (float) in.readDouble();
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            // Scale proper motions and size
            if (idx <= Particle.I_FPMZ || idx == Particle.I_FSIZE)
                dataF[idx] *= (float) Constants.DISTANCE_SCALE_FACTOR;
        }
        // Version 2: we have the HIP number in the data file.
        if (hipId) {
            // HIP
            dataF[Particle.I_FHIP] = in.readInt();
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
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        return new Particle(id, names, dataD, dataF);
    }
}
