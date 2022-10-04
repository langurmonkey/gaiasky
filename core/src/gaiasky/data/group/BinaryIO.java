/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleRecord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Interface to read and write stars, to be implemented by different binary format versions.
 */
public interface BinaryIO {

    /**
     * Read a star from the mapped buffer.
     *
     * @param mem    Mapped memory buffer to read from.
     * @param factor Scale factor to apply to the positions.
     * @return The star.
     */
    ParticleRecord readParticleRecord(MappedByteBuffer mem, double factor);

    /**
     * Read a star from the input stream.
     *
     * @param in Input stream.
     * @param factor Scale factor to apply to the positions.
     * @return The star.
     * @throws IOException If the read fails.
     */
    ParticleRecord readParticleRecord(DataInputStream in, double factor) throws IOException;

    /**
     * Write the star bean to the output stream.
     *
     * @param sb  The star bean.
     * @param out The output stream.
     * @throws IOException If the write operation fails.
     */
    void writeParticleRecord(IParticleRecord sb, DataOutputStream out) throws IOException;
}
