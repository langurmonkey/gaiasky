/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.data.api.BinaryIO;
import gaiasky.scene.record.ParticleRecord;
import gaiasky.util.Constants;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

public abstract class BinaryIOBase implements BinaryIO {
    protected final int nDoubles;
    protected final int nFloats;

    protected boolean tychoIds;

    protected BinaryIOBase(int nDoubles,
                           int nFloats,
                           boolean tychoIds) {
        this.tychoIds = tychoIds;
        this.nDoubles = nDoubles;
        this.nFloats = nFloats;
    }

    @Override
    public ParticleRecord readParticleRecord(MappedByteBuffer mem,
                                             double factor) {
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
                // Goes to float array.
                int idx = i - ParticleRecord.STAR_SIZE_D;
                dataF[idx] = (float) mem.getDouble();
                if (idx < 3) {
                    // Proper motions.
                    dataF[idx] *= Constants.DISTANCE_SCALE_FACTOR;
                }
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = mem.getFloat();
            if (idx == ParticleRecord.I_FSIZE || (nFloats == 10  && idx < 3)) {
                // 0-2 - Proper motions (only in version 2).
                // 9 | 3 - Size (version 2 | 0-1).
                dataF[idx] *= Constants.DISTANCE_SCALE_FACTOR;
            }
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
    public ParticleRecord readParticleRecord(DataInputStream in,
                                             double factor) throws IOException {
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
