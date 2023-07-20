/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.mesh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IntIndexBufferObject implements IntIndexData {
    final IntBuffer buffer;
    final ByteBuffer byteBuffer;
    final boolean isDirect;
    final int usage;
    // used to work around bug: https://android-review.googlesource.com/#/c/73175/
    private final boolean empty;
    int bufferHandle;
    boolean isDirty = true;
    boolean isBound = false;

    /**
     * Creates a new static IntIndexBufferObject to be used with vertex arrays.
     *
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    public IntIndexBufferObject(int maxIndices) {
        this(true, maxIndices);
    }

    /**
     * Creates a new IntIndexBufferObject.
     *
     * @param isStatic   whether the index buffer is static
     * @param maxIndices the maximum number of indices this buffer can hold
     */
    public IntIndexBufferObject(boolean isStatic, int maxIndices) {

        empty = maxIndices == 0;
        if (empty) {
            maxIndices = 1; // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
        }

        byteBuffer = BufferUtils.newUnsafeByteBuffer(maxIndices * 4);
        isDirect = true;

        buffer = byteBuffer.asIntBuffer();
        buffer.flip();
        byteBuffer.flip();
        bufferHandle = Gdx.gl20.glGenBuffer();
        usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
    }

    /** @return the number of indices currently stored in this buffer */
    public int getNumIndices() {
        return empty ? 0 : buffer.limit();
    }

    /** @return the maximum number of indices this IntIndexBufferObject can store. */
    public int getNumMaxIndices() {
        return empty ? 0 : buffer.capacity();
    }

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
     * @param count   the number of integers to copy
     */
    public void setIndices(int[] indices, int offset, int count) {
        isDirty = true;
        buffer.clear();
        buffer.put(indices, offset, count);
        buffer.flip();
        byteBuffer.position(0);
        byteBuffer.limit(count << 2);

        if (isBound) {
            Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            isDirty = false;
        }
    }

    public void setIndices(IntBuffer indices) {
        isDirty = true;
        int pos = indices.position();
        buffer.clear();
        buffer.put(indices);
        buffer.flip();
        indices.position(pos);
        byteBuffer.position(0);
        byteBuffer.limit(buffer.limit() << 2);

        if (isBound) {
            Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            isDirty = false;
        }
    }

    @Override
    public void updateIndices(int targetOffset, int[] indices, int offset, int count) {
        isDirty = true;
        final int pos = byteBuffer.position();
        byteBuffer.position(targetOffset * 4);
        BufferUtils.copy(indices, offset, byteBuffer, count);
        byteBuffer.position(pos);
        buffer.position(0);

        if (isBound) {
            Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            isDirty = false;
        }
    }

    /**
     * <p>
     * Returns the underlying IntBuffer. If you modify the buffer contents they will be uploaded on the call to {@link #bind()}.
     * If you need immediate uploading use {@link #setIndices(int[], int, int)}.
     * </p>
     *
     * @return the underlying int buffer.
     */
    public IntBuffer getBuffer() {
        isDirty = true;
        return buffer;
    }

    /** Binds this IntIndexBufferObject for rendering with glDrawElements. */
    public void bind() {
        if (bufferHandle == 0)
            throw new GdxRuntimeException("No buffer allocated!");

        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle);
        if (isDirty) {
            byteBuffer.limit(buffer.limit() * 4);
            Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            isDirty = false;
        }
        isBound = true;
    }

    /** Unbinds this IntIndexBufferObject. */
    public void unbind() {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
        isBound = false;
    }

    /** Invalidates the IntIndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
    public void invalidate() {
        bufferHandle = Gdx.gl20.glGenBuffer();
        isDirty = true;
    }

    /** Disposes this IntIndexBufferObject and all its associated OpenGL resources. */
    public void dispose() {
        Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
        Gdx.gl20.glDeleteBuffer(bufferHandle);
        bufferHandle = 0;
        try {
            BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
