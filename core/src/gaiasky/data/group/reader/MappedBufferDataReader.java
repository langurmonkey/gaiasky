/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group.reader;

import java.nio.MappedByteBuffer;

public class MappedBufferDataReader implements IDataReader {

    private final MappedByteBuffer is;

    public MappedBufferDataReader(MappedByteBuffer is) {
        this.is = is;
    }

    @Override
    public float readFloat() {
        return is.getFloat();
    }

    @Override
    public double readDouble() {
        return is.getDouble();
    }

    @Override
    public char readChar() {
        return is.getChar();
    }

    @Override
    public int readInt() {
        return is.getInt();
    }

    @Override
    public long readLong() {
        return is.getLong();
    }

    @Override
    public byte readByte() {
        return is.get();
    }
}
