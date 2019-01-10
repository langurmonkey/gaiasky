package gaia.cu9.ari.gaiaorbit.util.io;

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

    public long getBytesRead(){
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
