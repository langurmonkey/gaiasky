/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.mesh;

import com.badlogic.gdx.utils.Disposable;

import java.nio.IntBuffer;

public interface IntIndexData extends Disposable {
    /** @return the number of indices currently stored in this buffer */
    int getNumIndices();

    /** @return the maximum number of indices this IntIndexBufferObject can store. */
    int getNumMaxIndices();

    /**
     * <p>
     * Sets the indices of this IntIndexBufferObject, discarding the old indices. The count must equal the number of indices to be
     * copied to this IntIndexBufferObject.
     * </p>
     *
     * <p>
     * This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated instantly.
     * </p>
     *
     * @param indices the index data
     * @param offset  the offset to start copying the data from
     * @param count   the number of ints to copy
     */
    void setIndices(int[] indices, int offset, int count);

    /**
     * Copies the specified indices to the indices of this IntIndexBufferObject, discarding the old indices. Copying start at the
     * current {@link IntBuffer#position()} of the specified buffer and copied the {@link IntBuffer#remaining()} amount of
     * indices. This can be called in between calls to {@link #bind()} and {@link #unbind()}. The index data will be updated
     * instantly.
     *
     * @param indices the index data to copy
     */
    void setIndices(IntBuffer indices);

    /**
     * Update (a portion of) the indices.
     *
     * @param targetOffset offset in indices buffer
     * @param indices      the index data
     * @param offset       the offset to start copying the data from
     * @param count        the number of ints to copy
     */
    void updateIndices(int targetOffset, int[] indices, int offset, int count);

    /**
     * <p>
     * Returns the underlying IntBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
     * If you need immediate uploading use {@link #setIndices(int[], int, int)}.
     * </p>
     *
     * @return the underlying int buffer.
     */
    IntBuffer getBuffer();

    /** Binds this IntIndexBufferObject for rendering with glDrawElements. */
    void bind();

    /** Unbinds this IntIndexBufferObject. */
    void unbind();

    /** Invalidates the IntIndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
    void invalidate();

    /** Disposes this IndexDatat and all its associated OpenGL resources. */
    void dispose();
}
