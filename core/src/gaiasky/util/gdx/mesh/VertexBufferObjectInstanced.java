/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.mesh;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.IntArray;
import gaiasky.util.gdx.shader.ExtShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * <p>
 * A {@link VertexData} implementation that uses vertex buffer objects and vertex array objects to
 * construct a global buffer and an instance buffer for instanced rendering.
 * The global buffer holds the attributes that do not change (i.e. position, uv, etc.), the instance buffer holds attributes
 * that change for every instance.
 * (This is required for OpenGL 3.0+ core profiles. In particular, the default VAO has been
 * deprecated, as has the use of client memory for passing vertex attributes.) Use of VAOs should
 * give a slight performance benefit since you don't have to bind the attributes on every draw
 * anymore.
 * </p>
 *
 * <p>
 * If the OpenGL ES context was lost you can call {@link #invalidate()} to recreate a new OpenGL vertex buffer object.
 * </p>
 *
 * <p>
 * VertexBufferObjectInstanced objects must be disposed via the {@link #dispose()} method when no longer needed
 * </p>
 * <p>
 * Code adapted from {@link VertexBufferObject}.
 *
 * @author mzechner, Dave Clayton <contact@redskyforge.com>, Nate Austin <nate.austin gmail>
 */
public class VertexBufferObjectInstanced implements IntVertexData {
    final static IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

    final VertexAttributes globalAttributes;
    final VertexAttributes instanceAttributes;
    final FloatBuffer bufferGlobal;
    final ByteBuffer byteBufferGlobal;
    int globalBufferHandle;
    final FloatBuffer bufferInstance;
    final ByteBuffer byteBufferInstance;
    int instanceBufferHandle;
    final boolean isStatic;
    final int usage;
    boolean isDirtyGlobal = false;
	boolean isDirtyInstance = false;
	boolean isBoundGlobal = false;
    boolean isBoundInstance = false;
    int meshVAO = -1;
    IntArray cachedLocationsGlobal = new IntArray();
    IntArray cachedLocationsInstance = new IntArray();

    /**
     * Constructs a new interleaved VertexBufferObjectWithVAO.
     *
     * @param isStatic         whether the vertex data is static.
     * @param numGlobal        the maximum number of vertices
     * @param globalAttributes the {@link VertexAttributes}.
     */
    public VertexBufferObjectInstanced(boolean isStatic, int numGlobal, VertexAttributes globalAttributes, int numInstance, VertexAttributes instanceAttributes) {
        this.isStatic = isStatic;
        this.globalAttributes = globalAttributes;
        this.instanceAttributes = instanceAttributes;

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

        usage = isStatic ? GL20.GL_STATIC_DRAW : GL20.GL_DYNAMIC_DRAW;
        createVAO();
    }

    @Override
    public VertexAttributes getAttributes() {
        return globalAttributes;
    }

    public VertexAttributes getInstanceAttributes() {
        return instanceAttributes;
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

    @Override
    public void updateVertices(int targetOffset, float[] vertices, int sourceOffset, int count) {
        isDirtyGlobal = true;
        final int pos = byteBufferGlobal.position();
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
        isDirtyGlobal = bindData(gl, globalBufferHandle, isDirtyGlobal, bufferGlobal, byteBufferGlobal);
        isBoundGlobal = true;

        // Instance attributes
		bindAttributes(shader, locations, instanceAttributes, instanceBufferHandle, cachedLocationsInstance, 1);
        isDirtyInstance = bindData(gl, instanceBufferHandle, isDirtyInstance, bufferInstance, byteBufferInstance);
        isBoundInstance = true;

    }

    private void bindAttributes(ExtShaderProgram shader, int[] locations, VertexAttributes attributes, int bufferHandle, IntArray cachedLocations, int divisor) {
        boolean stillValid = cachedLocations.size != 0;
        final int numAttributes = attributes.size();

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

        for(int i =0; i < numAttributes; i++) {
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

    private boolean bindData(GL20 gl, int bufferHandle, boolean isDirty, FloatBuffer buffer, ByteBuffer byteBuffer) {
        if (isDirty) {
            gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
            byteBuffer.limit(buffer.limit() * 4);
            gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
        }
        return false;
    }

    /**
     * Unbinds this VertexBufferObject.
     *
     * @param shader the shader
     */
    @Override
    public void unbind(final ExtShaderProgram shader) {
        unbind(shader, null);
    }

    @Override
    public void unbind(final ExtShaderProgram shader, final int[] locations) {
        GL30 gl = Gdx.gl30;
        gl.glBindVertexArray(0);
        isBoundGlobal = false;
        isBoundInstance = false;
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
