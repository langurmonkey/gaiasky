/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.mesh;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.Disposable;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.nio.FloatBuffer;

public interface IntVertexData extends Disposable {
    /** @return the number of vertices this VertexData stores */
    int getNumVertices();

    /** @return the number of vertices this VertedData can store */
    int getNumMaxVertices();

    /** @return the {@link VertexAttributes} as specified during construction. */
    VertexAttributes getAttributes();

    /**
     * Sets the vertices of this VertexData, discarding the old vertex data. The count must equal the number of floats per vertex
     * times the number of vertices to be copied to this VertexData. The order of the vertex attributes must be the same as
     * specified at construction time via {@link VertexAttributes}.
     * <p>
     * This can be called in between calls to bind and unbind. The vertex data will be updated instantly.
     *
     * @param vertices the vertex data
     * @param offset   the offset to start copying the data from
     * @param count    the number of floats to copy
     */
    void setVertices(float[] vertices, int offset, int count);

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param vertices     the vertex data
     * @param sourceOffset the offset to start copying the data from
     * @param count        the number of floats to copy
     */
    void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count);

    /**
     * Returns the underlying FloatBuffer and marks it as dirty, causing the buffer contents to be uploaded on the next call to
     * bind. If you need immediate uploading use {@link #setVertices(float[], int, int)}; Any modifications made to the Buffer
     * *after* the call to bind will not automatically be uploaded.
     *
     * @return the underlying FloatBuffer holding the vertex data.
     */
    FloatBuffer getBuffer();

    /** Binds this VertexData for rendering via glDrawArrays or glDrawElements. */
    void bind(ExtShaderProgram shader);

    /**
     * Binds this VertexData for rendering via glDrawArrays or glDrawElements.
     *
     * @param locations array containing the attribute locations.
     */
    void bind(ExtShaderProgram shader, int[] locations);

    /** Unbinds this VertexData. */
    void unbind(ExtShaderProgram shader);

    /**
     * Unbinds this VertexData.
     *
     * @param locations array containing the attribute locations.
     */
    void unbind(ExtShaderProgram shader, int[] locations);

    /** Invalidates the VertexData if applicable. Use this in case of a context loss. */
    void invalidate();

    /** Disposes this VertexData and all its associated OpenGL resources. */
    void dispose();
}
