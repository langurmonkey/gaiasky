/*
 * Copyright (c) 2023-2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.render.gdx.mesh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntArray;
import gaiasky.render.gdx.shader.ExtShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class VertexBufferObjectInstanced implements IntVertexData {
    final static IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

    final VertexAttributes globalAttributes;
    final VertexAttributes instanceAttributes;
    /**
     * Per-instance attributes that change every frame, in a buffer of their own. Null when the mesh
     * has none.
     **/
    final VertexAttributes dynamicAttributes;
    final FloatBuffer bufferGlobal;
    final ByteBuffer byteBufferGlobal;
    final FloatBuffer bufferInstance;
    final ByteBuffer byteBufferInstance;
    final FloatBuffer bufferDynamic;
    final ByteBuffer byteBufferDynamic;
    final boolean isStatic;
    final int usage;
    /** Usage hint of the dynamic group: rewritten in full on every frame. **/
    final int usageDynamic;
    int globalBufferHandle;
    int instanceBufferHandle;
    int dynamicBufferHandle;
    boolean isDirtyGlobal;
    boolean isDirtyInstance;
    boolean isDirtyDynamic;
    boolean isBoundGlobal;
    boolean isBoundInstance;
    boolean isBoundDynamic;
    int meshVAO = -1;
    IntArray cachedLocationsGlobal = new IntArray();
    IntArray cachedLocationsInstance = new IntArray();
    IntArray cachedLocationsDynamic = new IntArray();

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO.
     *
     * @param isStatic         whether the vertex data is static.
     * @param numGlobal        the maximum number of vertices
     * @param globalAttributes the {@link VertexAttributes}.
     */
    public VertexBufferObjectInstanced(boolean isStatic, int numGlobal, VertexAttributes globalAttributes, int numInstance, VertexAttributes instanceAttributes) {
        this(isStatic, numGlobal, globalAttributes, numInstance, instanceAttributes, null);
    }

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO with an extra group of per-instance
     * attributes, kept in its own buffer.
     */
    public VertexBufferObjectInstanced(boolean isStatic, int numGlobal, VertexAttributes globalAttributes, int numInstance, VertexAttributes instanceAttributes, VertexAttributes dynamicAttributes) {
        this.isStatic = isStatic;
        this.globalAttributes = globalAttributes;
        this.instanceAttributes = instanceAttributes;
        this.dynamicAttributes = dynamicAttributes != null && dynamicAttributes.size() > 0 ? dynamicAttributes : null;

        byteBufferGlobal = BufferUtils.newUnsafeByteBuffer(this.globalAttributes.vertexSize * numGlobal);
        bufferGlobal = byteBufferGlobal.asFloatBuffer();
        bufferGlobal.flip();
        byteBufferGlobal.flip();
        globalBufferHandle = Gdx.gl20.glGenBuffer();

        byteBufferInstance = BufferUtils.newUnsafeByteBuffer(this.instanceAttributes.vertexSize * numInstance);
        bufferInstance = byteBufferInstance.asFloatBuffer();
        bufferInstance.flip();
        byteBufferInstance.flip();
        instanceBufferHandle = Gdx.gl20.glGenBuffer();

        if (this.dynamicAttributes != null) {
            byteBufferDynamic = BufferUtils.newUnsafeByteBuffer(this.dynamicAttributes.vertexSize * numInstance);
            bufferDynamic = byteBufferDynamic.asFloatBuffer();
            bufferDynamic.flip();
            byteBufferDynamic.flip();
            dynamicBufferHandle = Gdx.gl20.glGenBuffer();
        } else {
            byteBufferDynamic = null;
            bufferDynamic = null;
        }

        usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
        usageDynamic = GL20.GL_STREAM_DRAW;
        createVAO();
    }

    @Override
    public VertexAttributes getAttributes() {
        return globalAttributes;
    }

    public VertexAttributes getInstanceAttributes() {
        return instanceAttributes;
    }

    /**
     * @return The attributes of the per-frame group, or null if this mesh has none.
     */
    public VertexAttributes getDynamicAttributes() {
        return dynamicAttributes;
    }

    @Override
    public int getNumVertices() {
        return bufferGlobal.limit() * 4 / globalAttributes.vertexSize;
    }

    @Override
    public int getNumMaxVertices() {
        return byteBufferGlobal.capacity() / globalAttributes.vertexSize;
    }

    @Override
    public FloatBuffer getBuffer() {
        isDirtyGlobal = true;
        return bufferGlobal;
    }

    private boolean bufferChanged(ByteBuffer byteBuffer, boolean isBound, boolean isDirty) {
        if (isBound) {
            Gdx.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
            return false;
        }
        return isDirty;
    }

    @Override
    public void setVertices(float[] vertices, int offset, int count) {
        isDirtyGlobal = true;
        BufferUtils.copy(vertices, byteBufferGlobal, count, offset);
        bufferGlobal.position(0);
        bufferGlobal.limit(count);
        isDirtyGlobal = bufferChanged(byteBufferGlobal, isBoundGlobal, isDirtyGlobal);
    }

    public void setInstance(float[] vertices, int offset, int count) {
        isDirtyInstance = true;
        BufferUtils.copy(vertices, byteBufferInstance, count, offset);
        bufferInstance.position(0);
        bufferInstance.limit(count);
        isDirtyInstance = bufferChanged(byteBufferInstance, isBoundInstance, isDirtyInstance);
    }

    /**
     * Uploads the dynamic per-instance attributes. The buffer is rewritten in full, so the driver
     * is told to expect a stream.
     */
    public void setDynamic(float[] vertices, int offset, int count) {
        if (byteBufferDynamic == null) {
            return;
        }
        isDirtyDynamic = true;
        // Clear first: the float view takes its capacity from the byte buffer's remaining bytes.
        byteBufferDynamic.clear();
        BufferUtils.copy(vertices, byteBufferDynamic, count, offset);
        bufferDynamic.position(0);
        bufferDynamic.limit(count);
        if (isBoundDynamic) {
            byteBufferDynamic.limit(count * 4);
            Gdx.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, byteBufferDynamic.limit(), byteBufferDynamic, usageDynamic);
            isDirtyDynamic = false;
        }
    }

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        isDirtyGlobal = true;
        int pos = byteBufferGlobal.position();
        byteBufferGlobal.position(targetOffset * 4);
        BufferUtils.copy(vertices, sourceOffset, count, byteBufferGlobal);
        byteBufferGlobal.position(pos);
        bufferGlobal.position(0);
        isDirtyGlobal = bufferChanged(byteBufferGlobal, isBoundGlobal, isDirtyGlobal);
    }

    /**
     * Binds this VertexBufferObject for rendering via glDrawArrays or glDrawElements
     *
     * @param shader the shader
     */
    @Override
    public void bind(ExtShaderProgram shader) {
        bind(shader, null);
    }

    @Override
    public void bind(ExtShaderProgram shader, int[] locations) {
        GL30 gl = Gdx.gl30;

        gl.glBindVertexArray(meshVAO);

        // Non-instanced attributes
        bindAttributes(shader, locations, globalAttributes, globalBufferHandle, cachedLocationsGlobal, 0);
        isDirtyGlobal = bindData(gl, globalBufferHandle, isDirtyGlobal, bufferGlobal, byteBufferGlobal, usage);
        isBoundGlobal = true;

        // Instance attributes
        bindAttributes(shader, locations, instanceAttributes, instanceBufferHandle, cachedLocationsInstance, 1);
        isDirtyInstance = bindData(gl, instanceBufferHandle, isDirtyInstance, bufferInstance, byteBufferInstance, usage);
        isBoundInstance = true;

        // Per-frame instance attributes
        if (dynamicAttributes != null) {
            bindAttributes(shader, locations, dynamicAttributes, dynamicBufferHandle, cachedLocationsDynamic, 1);
            isDirtyDynamic = bindData(gl, dynamicBufferHandle, isDirtyDynamic, bufferDynamic, byteBufferDynamic, usageDynamic);
            isBoundDynamic = true;
        }
    }

    private void bindAttributes(ExtShaderProgram shader, int[] locations, VertexAttributes attributes, int bufferHandle, IntArray cachedLocations, int divisor) {
        boolean stillValid = cachedLocations.size != 0;
        int numAttributes = attributes.size();

        if (stillValid) {
            if (locations == null) {
                for (int i = 0; stillValid && i < numAttributes; i++) {
                    VertexAttribute attribute = attributes.get(i);
                    int location = shader.getAttributeLocation(attribute.alias);
                    stillValid = location == cachedLocations.get(i);
                }
            } else {
                stillValid = locations.length == cachedLocations.size;
                for (int i = 0; stillValid && i < numAttributes; i++) {
                    stillValid = locations[i] == cachedLocations.get(i);
                }
            }
        }

        if (!stillValid) {
            Gdx.gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
            unbindAttributes(shader, attributes, cachedLocations);
            cachedLocations.clear();

            for (int i = 0; i < numAttributes; i++) {
                VertexAttribute attribute = attributes.get(i);
                if (locations == null) {
                    cachedLocations.add(shader.getAttributeLocation(attribute.alias));
                } else {
                    cachedLocations.add(locations[i]);
                }

                int location = cachedLocations.get(i);
                if (location < 0) {
                    continue;
                }

                shader.enableVertexAttribute(location);
                shader.setVertexAttribute(location, attribute.numComponents, attribute.type, attribute.normalized, attributes.vertexSize, attribute.offset);
            }
        }

        for (int i = 0; i < numAttributes; i++) {
            int location = cachedLocations.get(i);
            if (location < 0) {
                continue;
            }
            if (divisor >= 0) {
                Gdx.gl30.glVertexAttribDivisor(location, divisor);
            }
        }
    }

    private void unbindAttributes(ExtShaderProgram shaderProgram, VertexAttributes attributes, IntArray cachedLocations) {
        if (cachedLocations.size == 0) {
            return;
        }
        int numAttributes = attributes.size();
        for (int i = 0; i < numAttributes; i++) {
            int location = cachedLocations.get(i);
            if (location < 0) {
                continue;
            }
            shaderProgram.disableVertexAttribute(location);
        }
    }

    private boolean bindData(GL20 gl, int bufferHandle, boolean isDirty, FloatBuffer buffer, ByteBuffer byteBuffer, int bufferUsage) {
        if (isDirty) {
            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
            byteBuffer.limit(buffer.limit() * 4);
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, bufferUsage);
        }
        return false;
    }

    /**
     * Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    @Override
    public void unbind(ExtShaderProgram shader) {
        unbind(shader, null);
    }

    @Override
    public void unbind(ExtShaderProgram shader, int[] locations) {
        GL30 gl = Gdx.gl30;
        gl.glBindVertexArray(0);
        isBoundGlobal = false;
        isBoundInstance = false;
        isBoundDynamic = false;
    }

    /**
     * Invalidates the VertexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.
     */
    @Override
    public void invalidate() {
        globalBufferHandle = Gdx.gl30.glGenBuffer();
        createVAO();
        isDirtyGlobal = true;
    }

    /**
     * Disposes of all resources this VertexBufferObject uses.
     */
    @Override
    public void dispose() {
        GL30 gl = Gdx.gl30;

        gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

        gl.glDeleteBuffer(globalBufferHandle);
        globalBufferHandle = 0;
        BufferUtils.disposeUnsafeByteBuffer(byteBufferGlobal);

        gl.glDeleteBuffer(instanceBufferHandle);
        instanceBufferHandle = 0;
        BufferUtils.disposeUnsafeByteBuffer(byteBufferInstance);

        if (byteBufferDynamic != null) {
            gl.glDeleteBuffer(dynamicBufferHandle);
            dynamicBufferHandle = 0;
            BufferUtils.disposeUnsafeByteBuffer(byteBufferDynamic);
        }

        deleteVAO();
    }

    private void createVAO() {
        tmpHandle.clear();
        Gdx.gl30.glGenVertexArrays(1, tmpHandle);
        meshVAO = tmpHandle.get();
    }

    private void deleteVAO() {
        if (meshVAO != -1) {
            tmpHandle.clear();
            tmpHandle.put(meshVAO);
            tmpHandle.flip();
            Gdx.gl30.glDeleteVertexArrays(1, tmpHandle);
            meshVAO = -1;
        }
    }
}
