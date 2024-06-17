/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.io;

import net.jafama.FastMath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
    private ByteBuffer byteBuffer;

    public ByteBufferInputStream() {
    }

    /** Creates a stream with a new non-direct buffer of the specified size. The position and limit of the buffer is zero. */
    public ByteBufferInputStream(int bufferSize) {
        this(ByteBuffer.allocate(bufferSize));
        byteBuffer.flip();
    }

    /** Creates an uninitialized stream that cannot be used until {@link #setByteBuffer(ByteBuffer)} is called. */
    public ByteBufferInputStream(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int read() throws IOException {
        if (!byteBuffer.hasRemaining())
            return -1;
        return byteBuffer.get() & 0xFF;
    }

    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0)
            return 0;
        int count = FastMath.min(byteBuffer.remaining(), length);
        if (count == 0)
            return -1;
        byteBuffer.get(bytes, offset, count);
        return count;
    }

    public int available() throws IOException {
        return byteBuffer.remaining();
    }
}
