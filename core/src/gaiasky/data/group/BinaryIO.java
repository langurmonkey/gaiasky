package gaiasky.data.group;

import gaiasky.scenegraph.ParticleGroup.ParticleRecord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Interface to read and write stars, to be implemented by different binary format versions
 */
public interface BinaryIO {

    /**
     * Read a star from the mapped buffer
     *
     * @param mem    Mapped memory buffer to read from
     * @param factor Scale factor to apply to positions
     * @return The star
     */
    ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor);

    /**
     * Read a star from the input stream
     *
     * @param in Input stream
     * @param factor Scale factor to apply to positions
     * @return The star
     * @throws IOException
     */
    ParticleRecord readParticleRecord(DataInputStream in, double factor) throws IOException;

    /**
     * Write the star bean to the output stream
     *
     * @param sb  The star bean
     * @param out The output stream
     * @throws IOException
     */
    void writeParticleRecord(ParticleRecord sb, DataOutputStream out) throws IOException;
}
