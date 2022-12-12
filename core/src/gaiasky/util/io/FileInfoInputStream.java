/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileInfoInputStream extends FileInputStream {

    private long bytesRead = 0;

    public FileInfoInputStream(String file) throws FileNotFoundException {
        super(file);
    }

    public FileInfoInputStream(File file) throws FileNotFoundException {
        super(file);
    }

    public long getBytesRead() {
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1)
            bytesRead++;
        return b;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        int b = super.read(bytes);
        if (b != -1)
            bytesRead += b;
        return b;
    }

    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        int b = super.read(bytes, i, i1);
        if (b != -1)
            bytesRead += b;
        return b;
    }

}
