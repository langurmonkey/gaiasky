/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scene.api.IParticleRecord;

import java.io.DataOutputStream;
import java.io.IOException;

public class BinaryVersion0 extends BinaryIOBase {

    public BinaryVersion0() {
        super(9, 4, true);
    }

    @Override
    public void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException {
        // 9 doubles
        out.writeDouble(sb.x());
        out.writeDouble(sb.y());
        out.writeDouble(sb.z());
        out.writeDouble(sb.pmx());
        out.writeDouble(sb.pmy());
        out.writeDouble(sb.pmz());
        out.writeDouble(sb.mualpha());
        out.writeDouble(sb.mudelta());
        out.writeDouble(sb.radvel());

        // 4 floats
        out.writeFloat(sb.appMag());
        out.writeFloat(sb.absMag());
        out.writeFloat(sb.col());
        out.writeFloat(sb.size());

        // HIP
        out.writeInt(sb.hip());

        // TYCHO
        if (tychoIds) {
            // 3 integers, keep compatibility
            out.writeInt(-1);
            out.writeInt(-1);
            out.writeInt(-1);
        }

        // ID
        out.writeLong(sb.id());

        // NAME
        String namesConcat = sb.namesConcat();
        if (namesConcat == null || namesConcat.length() == 0) {
            out.writeInt(0);
        } else {
            out.writeInt(namesConcat.length());
            out.writeChars(namesConcat);
        }
    }

}
