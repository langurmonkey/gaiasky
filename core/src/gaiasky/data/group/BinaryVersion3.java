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
import gaiasky.scene.record.ParticleRecord;
import gaiasky.scene.record.ParticleRecord.ParticleRecordType;
import gaiasky.util.Constants;
import gaiasky.util.parse.Parser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * The binary version 3 includes the effective temperature, t_eff, and does not have the hip number (already in names
 * array).
 */
public class BinaryVersion3 implements BinaryIO {

    protected BinaryVersion3() {
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
        for (int i = 0; i < 3; i++) {
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
        for (int i = 0; i < 10; i++) {
            // Skip radial velocity, it comes at the end.
            if (i == ParticleRecord.I_FRADVEL) {
                i++;
            }
            int idx = i + floatOffset;
            dataF[idx] = in.readFloat();
            if (idx == ParticleRecord.I_FSIZE)
                dataF[idx] *= (float) Constants.DISTANCE_SCALE_FACTOR;
        }

        // Extra floats
        // Read byte mask
        byte b = in.readByte();
        BitSet bs = BitSet.valueOf(new byte[] { b, 0, 0, 0 });

        float radvel = Float.NaN;
        float teff = Float.NaN;

        // Read values.
        if (bs.get(7)) {
            radvel = in.readFloat();
        }
        if (bs.get(7 - 1)) {
            teff = in.readFloat();
        }
        dataF[ParticleRecord.I_FRADVEL] = radvel;
        dataF[ParticleRecord.I_FTEFF] = teff;

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

        // Version 3: we take the HIP number from the names array.
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

        return new ParticleRecord(ParticleRecordType.STAR, dataD, dataF, id, names);
    }

    @Override
    public void writeParticleRecord(IParticleRecord sb,
                                    DataOutputStream out) throws IOException {
        // 3 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());

        // 9 floats
        out.writeFloat((float) sb.pmx());
        out.writeFloat((float) sb.pmy());
        out.writeFloat((float) sb.pmz());
        out.writeFloat(sb.mualpha());
        out.writeFloat(sb.mudelta());
        out.writeFloat(sb.appMag());
        out.writeFloat(sb.absMag());
        out.writeFloat(sb.col());
        out.writeFloat(sb.size());

        // Extra floats mask.
        BitSet bs = new BitSet(8);
        if (Float.isFinite(sb.radvel())) {
            bs.set(7);
        }
        if (Float.isFinite(sb.teff())) {
            bs.set(7 - 1);
        }
        out.writeByte(bs.toByteArray()[0]);
        // Write extra floats.
        if (Float.isFinite(sb.radvel())) {
            out.writeFloat(sb.radvel());
        }
        if (Float.isFinite(sb.teff())) {
            out.writeFloat(sb.teff());
        }

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
