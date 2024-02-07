/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group.reader;

import java.io.IOException;
import java.nio.MappedByteBuffer;

public class MappedBufferDataReader implements IDataReader {

    private final MappedByteBuffer is;

    public MappedBufferDataReader(MappedByteBuffer is) {
        this.is = is;
    }

    @Override
    public float readFloat() throws IOException {
        return is.getFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return is.getDouble();
    }

    @Override
    public char readChar() throws IOException {
        return is.getChar();
    }

    @Override
    public int readInt() throws IOException {
        return is.getInt();
    }

    @Override
    public long readLong() throws IOException {
        return is.getLong();
    }
}