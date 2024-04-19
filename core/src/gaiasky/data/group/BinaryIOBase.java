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
import gaiasky.scene.record.ParticleRecord;
import gaiasky.scene.record.ParticleRecord.ParticleRecordType;
import gaiasky.util.Constants;
import gaiasky.util.parse.Parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

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
                                             double factor) throws IOException {
        return readParticleRecord(new MappedBufferDataReader(mem), factor);
    }

    @Override
    public ParticleRecord readParticleRecord(DataInputStream in,
                                             double factor) throws IOException {
        return readParticleRecord(new InputStreamDataReader(in), factor);
    }

    public ParticleRecord readParticleRecord(IDataReader in,
                                             double factor) throws IOException {
        double[] dataD = new double[ParticleRecordType.STAR.doubleArraySize];
        float[] dataF = new float[ParticleRecordType.STAR.floatArraySize];
        int floatOffset = 0;
        // Double
        for (int i = 0; i < nDoubles; i++) {
            if (i < ParticleRecordType.STAR.doubleArraySize) {
                // Goes to double array
                dataD[i] = in.readDouble();
                dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
            } else {
                // Goes to float array
                int idx = i - ParticleRecordType.STAR.doubleArraySize;
                dataF[idx] = (float) in.readDouble();
                floatOffset = idx + 1;
            }
        }
        // Float
        for (int i = 0; i < nFloats; i++) {
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            if (idx == ParticleRecord.I_FSIZE)
                dataF[idx] *= (float) Constants.DISTANCE_SCALE_FACTOR;
        }
        // Version 2: we have the HIP number in the data file.
        if (nFloats == 10) {
            // HIP
            dataF[ParticleRecord.I_FHIP] = in.readInt();
        }

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
            names = new String[]{id.toString()};
        } else {
            StringBuilder namesConcat = new StringBuilder();
            for (int i = 0; i < nameLength; i++)
                namesConcat.append(in.readChar());
            names = namesConcat.toString().split(Constants.nameSeparatorRegex);
        }

        // Version 3: we take the HIP number from the names array.
        if (nFloats == 11) {
            // HIP from names.
            var hipName = Arrays.stream(names).filter(name -> name.startsWith("HIP ")).toList();
            if (!hipName.isEmpty()) {
                var name = hipName.get(0).trim();
                // We parse the hip id from the string (e.g. we take "2334" from "HIP 2334").
                if (name.length() > 4) {
                    try {
                        dataF[ParticleRecord.I_FHIP] = Parser.parseIntException(name.substring(4));
                    } catch (NumberFormatException e) {
                        dataF[ParticleRecord.I_FHIP] = -1;
                    }
                } else {
                    dataF[ParticleRecord.I_FHIP] = -1;
                }
            } else {
                dataF[ParticleRecord.I_FHIP] = -1;
            }
        }

        return new ParticleRecord(ParticleRecordType.STAR, dataD, dataF, id, names);
    }
}
