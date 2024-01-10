/*
 * Copyright (c) 2024 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group.reader;

import java.io.IOException;

public interface IDataReader {
    float readFloat() throws IOException;

    double readDouble() throws IOException;

    char readChar() throws IOException;

    int readInt() throws IOException;

    long readLong() throws IOException;
}
