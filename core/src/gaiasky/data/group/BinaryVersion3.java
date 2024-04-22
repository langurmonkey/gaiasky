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
public class BinaryVersion3 extends BinaryIOBase {

    protected BinaryVersion3() {
        super(3, 11, false, false);
    }
    @Override
    public void writeParticleRecord(IParticleRecord sb,
                                    DataOutputStream out) throws IOException {
        // 3 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());

        // 11 floats
        out.writeFloat((float) sb.pmx());
        out.writeFloat((float) sb.pmy());
        out.writeFloat((float) sb.pmz());
        out.writeFloat(sb.mualpha());
        out.writeFloat(sb.mudelta());
        out.writeFloat(sb.radvel());
        out.writeFloat(sb.appMag());
        out.writeFloat(sb.absMag());
        out.writeFloat(sb.col());
        out.writeFloat(sb.size());
        out.writeFloat(sb.teff());

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
