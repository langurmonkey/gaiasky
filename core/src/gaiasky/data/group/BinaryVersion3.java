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
import gaiasky.util.parse.Parser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

/**
 * The binary version 3 includes the effective temperature, t_eff, and does not have the hip number as a
 * floating point number, as it is already in the names array.
 */
public class BinaryVersion3 implements BinaryIO {

    protected BinaryVersion3() {
        super();
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
        for (int i = 0; i < 3; i++) {
            // Goes to double array
            dataD[i] = in.readDouble();
            dataD[i] *= factor * Constants.DISTANCE_SCALE_FACTOR;
        }
        // Float
        for (int i = 0; i < 11; i++) {
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            // Scale proper motions and size
            if (idx <= 2 || idx == 9)
                dataF[idx] *= (float) Constants.DISTANCE_SCALE_FACTOR;
        }
        // The last one is actually the TEFF.
        dataF[11] = dataF[10];
        dataF[10] = -1;

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

        // Version 3: we take the HIP number from the names array.
        // HIP from names.
        var hipName = Arrays.stream(names)
                .filter(name -> name.startsWith("HIP "))
                .toList();
        if (!hipName.isEmpty()) {
            var name = hipName.get(0)
                    .trim();
            // We parse the hip id from the string (e.g. we take "2334" from "HIP 2334").
            if (name.length() > 4) {
                try {
                    dataF[10] = Parser.parseIntException(name.substring(4));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return new ParticleStar(id, names, dataD[0], dataD[1], dataD[2], dataF[3], dataF[4], dataF[5], dataF[0], dataF[1], dataF[2], dataF[6],
                                dataF[7], dataF[8], dataF[9], (int) dataF[10], dataF[11], null);
    }

    @Override
    public void writeParticleRecord(IParticleRecord sb,
                                    DataOutputStream out) throws IOException {
        // 3 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());

        // 11 floats
        out.writeFloat(sb.vx());
        out.writeFloat(sb.vy());
        out.writeFloat(sb.vz());
        out.writeFloat(sb.muAlpha());
        out.writeFloat(sb.muDelta());
        out.writeFloat(sb.radVel());
        out.writeFloat(sb.appMag());
        out.writeFloat(sb.absMag());
        out.writeFloat(sb.color());
        out.writeFloat(sb.size());
        out.writeFloat(sb.tEff());

        // ID
        out.writeLong(sb.id());

        // NAME
        String namesConcat = sb.namesConcat();
        if (namesConcat == null || namesConcat.isEmpty()) {
            out.writeInt(0);
        } else {
            out.writeInt(namesConcat.length());
            out.writeChars(namesConcat);
        }
    }
}
