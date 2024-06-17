/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.mesh;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import gaiasky.util.gdx.shader.ExtShaderProgram;
import net.jafama.FastMath;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class IntMesh implements Disposable {
    /** list of all meshes **/
    static final Map<Application, Array<IntMesh>> meshes = new HashMap<>();
    // Vertices with attributes
    final IntVertexData vertices;
    // Indices
    final IntIndexData indices;
    final boolean isVertexArray;
    final boolean isInstanced;
    private final Vector3 tmpV = new Vector3();
    boolean autoBind = true;

    /**
     * Creates a new IntMesh from the given mesh.
     *
     * @param other The mesh to copy.
     */
    public IntMesh(Mesh other) {
        short[] otherIndices = new short[other.getNumIndices()];
        float[] otherVertices = new float[other.getNumVertices() * other.getVertexSize() / 4];
        other.getIndices(otherIndices);
        other.getVertices(otherVertices);

        int[] myIndices = new int[otherIndices.length];
        for (int i = 0; i < otherIndices.length; i++) {
            myIndices[i] = otherIndices[i];
        }

        vertices = makeVertexBuffer(true, otherVertices.length, other.getVertexAttributes());
        indices = new IntIndexBufferObjectSubData(true, otherIndices.length);
        isVertexArray = false;
        isInstanced = false;

        indices.setIndices(myIndices, 0, myIndices.length);
        vertices.setVertices(otherVertices, 0, otherVertices.length);

        addManagedMesh(Gdx.app, this);
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the {@link VertexAttribute}s. Each vertex attribute defines one property of a vertex such as position,
     *                    normal or texture coordinate. In instanced mode, these are the common attributes (divisor=1)
     */
    public IntMesh(boolean isStatic,
                   int maxVertices,
                   int maxIndices,
                   VertexAttribute[] attributes) {
        this(isStatic, maxVertices, maxIndices, new VertexAttributes(attributes));
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic            whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices         the maximum number of vertices this mesh can hold
     * @param maxInstances        the maximum number of instances this mesh can hold
     * @param attributes          the {@link VertexAttribute}s. Each vertex attribute defines one property of a vertex such as position,
     *                            normal or texture coordinate. In instanced mode, these are the common attributes (divisor=1)
     * @param attributesInstanced vertex attributes for instanced mode. These have a divisor of 1
     */
    public IntMesh(boolean isStatic,
                   int maxVertices,
                   int maxInstances,
                   VertexAttribute[] attributes,
                   VertexAttribute[] attributesInstanced) {
        this.isInstanced = maxInstances > 0;
        this.vertices = makeVertexBuffer(isStatic, maxVertices, new VertexAttributes(attributes), maxInstances, new VertexAttributes(attributesInstanced));
        this.indices = null;
        this.isVertexArray = false;

        addManagedMesh(Gdx.app, this);
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic            whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices         the maximum number of vertices this mesh can hold
     * @param maxInstances        the maximum number of instances this mesh can hold
     * @param maxIndices          the maximum number of indices this mesh can hold
     * @param attributes          the {@link VertexAttribute}s. Each vertex attribute defines one property of a vertex such as position,
     *                            normal or texture coordinate. In instanced mode, these are the common attributes (divisor=1)
     * @param attributesInstanced vertex attributes for instanced mode. These have a divisor of 1
     */
    public IntMesh(boolean isStatic,
                   int maxVertices,
                   int maxInstances,
                   int maxIndices,
                   VertexAttribute[] attributes,
                   VertexAttribute[] attributesInstanced) {
        this.isInstanced = maxInstances > 0;
        this.vertices = makeVertexBuffer(isStatic, maxVertices, new VertexAttributes(attributes), maxInstances, new VertexAttributes(attributesInstanced));
        if (maxIndices > 0) {
            this.indices = new IntIndexBufferObject(isStatic, maxIndices);
        } else {
            this.indices = null;
        }
        this.isVertexArray = false;

        addManagedMesh(Gdx.app, this);
    }

    /**
     * Creates a new Mesh with the given attributes.
     *
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the {@link VertexAttributes}. Each vertex attribute defines one property of a vertex such as position,
     *                    normal or texture coordinate
     */
    public IntMesh(boolean isStatic,
                   int maxVertices,
                   int maxIndices,
                   VertexAttributes attributes) {
        this.vertices = makeVertexBuffer(isStatic, maxVertices, attributes);
        this.indices = new IntIndexBufferObject(isStatic, maxIndices);
        this.isVertexArray = false;
        this.isInstanced = false;

        addManagedMesh(Gdx.app, this);
    }

    /**
     * Creates a new Mesh with the given attributes. Adds extra optimizations for dynamic (frequently modified) meshes.
     *
     * @param staticVertices whether vertices of this mesh are static or not. Allows for internal optimizations.
     * @param staticIndices  whether indices of this mesh are static or not. Allows for internal optimizations.
     * @param maxVertices    the maximum number of vertices this mesh can hold
     * @param maxIndices     the maximum number of indices this mesh can hold
     * @param attributes     the {@link VertexAttributes}. Each vertex attribute defines one property of a vertex such as position,
     *                       normal or texture coordinate
     *
     * @author Jaroslaw Wisniewski
     **/
    public IntMesh(boolean staticVertices,
                   boolean staticIndices,
                   int maxVertices,
                   int maxIndices,
                   VertexAttributes attributes) {
        this.vertices = makeVertexBuffer(staticVertices, maxVertices, attributes);
        this.indices = new IntIndexBufferObject(staticIndices, maxIndices);
        this.isVertexArray = false;
        this.isInstanced = false;

        addManagedMesh(Gdx.app, this);
    }

    /**
     * Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
     *
     * @param type        the {@link VertexDataType} to be used, VBO or VA.
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the {@link VertexAttribute}s. Each vertex attribute defines one property of a vertex such as position,
     *                    normal or texture coordinate
     */
    public IntMesh(VertexDataType type,
                   boolean isStatic,
                   int maxVertices,
                   int maxIndices,
                   VertexAttribute[] attributes) {
        this(type, isStatic, maxVertices, maxIndices, new VertexAttributes(attributes));
    }

    /**
     * Creates a new Mesh with the given attributes. This is an expert method with no error checking. Use at your own risk.
     *
     * @param type        the {@link VertexDataType} to be used, VBO or VA.
     * @param isStatic    whether this mesh is static or not. Allows for internal optimizations.
     * @param maxVertices the maximum number of vertices this mesh can hold
     * @param maxIndices  the maximum number of indices this mesh can hold
     * @param attributes  the {@link VertexAttributes}.
     */
    public IntMesh(VertexDataType type,
                   boolean isStatic,
                   int maxVertices,
                   int maxIndices,
                   VertexAttributes attributes) {
        switch (type) {
        case VertexBufferObject -> {
            vertices = new VertexBufferObject(isStatic, maxVertices, attributes);
            indices = new IntIndexBufferObject(isStatic, maxIndices);
            isVertexArray = false;
            isInstanced = false;
        }
        case VertexBufferObjectSubData -> {
            vertices = new VertexBufferObjectSubData(isStatic, maxVertices, attributes);
            indices = new IntIndexBufferObjectSubData(isStatic, maxIndices);
            isVertexArray = false;
            isInstanced = false;
        }
        case VertexBufferObjectWithVAO -> {
            vertices = new VertexBufferObjectWithVAO(isStatic, maxVertices, attributes);
            indices = new IntIndexBufferObjectSubData(isStatic, maxIndices);
            isVertexArray = false;
            isInstanced = false;
        }
        default -> {
            vertices = new VertexArray(maxVertices, attributes);
            indices = new IntIndexArray(maxIndices);
            isVertexArray = true;
            isInstanced = false;
        }
        }

        addManagedMesh(Gdx.app, this);
    }

    private static void addManagedMesh(Application app,
                                       IntMesh mesh) {
        Array<IntMesh> managedResources = meshes.get(app);
        if (managedResources == null)
            managedResources = new Array<>();
        managedResources.add(mesh);
        meshes.put(app, managedResources);
    }

    /**
     * Invalidates all meshes so the next time they are rendered new VBO handles are generated.
     *
     * @param app The application
     */
    public static void invalidateAllMeshes(Application app) {
        Array<IntMesh> meshesArray = meshes.get(app);
        if (meshesArray == null)
            return;
        for (int i = 0; i < meshesArray.size; i++) {
            meshesArray.get(i).vertices.invalidate();
            meshesArray.get(i).indices.invalidate();
        }
    }

    /** Will clear the managed mesh cache. I wouldn't use this if i was you :) */
    public static void clearAllMeshes(Application app) {
        meshes.remove(app);
    }

    public static String getManagedStatus() {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        builder.append("Managed meshes/app: { ");
        for (Application app : meshes.keySet()) {
            builder.append(meshes.get(app).size);
            builder.append(" ");
        }
        builder.append("}");
        return builder.toString();
    }

    /**
     * Method to transform the positions in the float array. Normals will be kept as is. This is a potentially slow operation, use
     * with care.
     *
     * @param matrix     the transformation matrix
     * @param vertices   the float array
     * @param vertexSize the number of floats in each vertex
     * @param offset     the offset within a vertex to the position
     * @param dimensions the size of the position
     * @param start      the vertex to start with
     * @param count      the amount of vertices to transform
     */
    public static void transform(final Matrix4 matrix,
                                 final float[] vertices,
                                 int vertexSize,
                                 int offset,
                                 int dimensions,
                                 int start,
                                 int count) {
        if (offset < 0 || dimensions < 1 || (offset + dimensions) > vertexSize)
            throw new IndexOutOfBoundsException();
        if (start < 0 || count < 1 || ((start + count) * vertexSize) > vertices.length)
            throw new IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize + ", length = " + vertices.length);

        final Vector3 tmp = new Vector3();

        int idx = offset + (start * vertexSize);
        switch (dimensions) {
        case 1 -> {
            for (int i = 0; i < count; i++) {
                tmp.set(vertices[idx], 0, 0).mul(matrix);
                vertices[idx] = tmp.x;
                idx += vertexSize;
            }
        }
        case 2 -> {
            for (int i = 0; i < count; i++) {
                tmp.set(vertices[idx], vertices[idx + 1], 0).mul(matrix);
                vertices[idx] = tmp.x;
                vertices[idx + 1] = tmp.y;
                idx += vertexSize;
            }
        }
        case 3 -> {
            for (int i = 0; i < count; i++) {
                tmp.set(vertices[idx], vertices[idx + 1], vertices[idx + 2]).mul(matrix);
                vertices[idx] = tmp.x;
                vertices[idx + 1] = tmp.y;
                vertices[idx + 2] = tmp.z;
                idx += vertexSize;
            }
        }
        }
    }

    /**
     * Method to transform the texture coordinates (UV) in the float array. This is a potentially slow operation, use with care.
     *
     * @param matrix     the transformation matrix
     * @param vertices   the float array
     * @param vertexSize the number of floats in each vertex
     * @param offset     the offset within a vertex to the texture location
     * @param start      the vertex to start with
     * @param count      the amount of vertices to transform
     */
    public static void transformUV(final Matrix3 matrix,
                                   final float[] vertices,
                                   int vertexSize,
                                   int offset,
                                   int start,
                                   int count) {
        if (start < 0 || count < 1 || ((start + count) * vertexSize) > vertices.length)
            throw new IndexOutOfBoundsException("start = " + start + ", count = " + count + ", vertexSize = " + vertexSize + ", length = " + vertices.length);

        final Vector2 tmp = new Vector2();

        int idx = offset + (start * vertexSize);
        for (int i = 0; i < count; i++) {
            tmp.set(vertices[idx], vertices[idx + 1]).mul(matrix);
            vertices[idx] = tmp.x;
            vertices[idx + 1] = tmp.y;
            idx += vertexSize;
        }
    }

    private IntVertexData makeVertexBuffer(boolean isStatic,
                                           int maxVertices,
                                           VertexAttributes vertexAttributes) {
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectWithVAO(isStatic, maxVertices, vertexAttributes);
        } else {
            return new VertexBufferObject(isStatic, maxVertices, vertexAttributes);
        }
    }

    private IntVertexData makeVertexBuffer(boolean isStatic,
                                           int verticesGlobal,
                                           VertexAttributes attributesGlobal,
                                           int verticesInstance,
                                           VertexAttributes attributesInstance) {
        if (Gdx.gl30 != null) {
            return new VertexBufferObjectInstanced(isStatic, verticesGlobal, attributesGlobal, verticesInstance, attributesInstance);
        } else {
            throw new RuntimeException("Instanced rendering requires OpenGL 3.0+");
        }
    }

    /**
     * Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param vertices the vertices.
     *
     * @return the mesh for invocation chaining.
     */
    public IntMesh setVertices(float[] vertices) {
        this.vertices.setVertices(vertices, 0, vertices.length);

        return this;
    }

    /**
     * Sets the vertices of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param vertices the vertices.
     * @param offset   the offset into the vertices array
     * @param count    the number of floats to use
     *
     * @return the mesh for invocation chaining.
     */
    public IntMesh setVertices(float[] vertices,
                               int offset,
                               int count) {
        this.vertices.setVertices(vertices, offset, count);

        return this;
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the vertex data to update the mesh part with
     */
    public IntMesh updateVertices(int targetOffset,
                                  float[] source) {
        return updateVertices(targetOffset, source, 0, source.length);
    }

    /**
     * Update (a portion of) the vertices. Does not resize the backing buffer.
     *
     * @param targetOffset the offset in number of floats of the mesh part.
     * @param source       the vertex data to update the mesh part with
     * @param sourceOffset the offset in number of floats within the source array
     * @param count        the number of floats to update
     */
    public IntMesh updateVertices(int targetOffset,
                                  float[] source,
                                  int sourceOffset,
                                  int count) {
        this.vertices.updateVertices(targetOffset, source, sourceOffset, count);
        return this;
    }

    /**
     * Copies the vertices from the Mesh to the float array. The float array must be large enough to hold all the Mesh's vertices.
     *
     * @param vertices the array to copy the vertices to
     */
    public float[] getVertices(float[] vertices) {
        return getVertices(0, -1, vertices);
    }

    /**
     * Copies the the remaining vertices from the Mesh to the float array. The float array must be large enough to hold the
     * remaining vertices.
     *
     * @param srcOffset the offset (in number of floats) of the vertices in the mesh to copy
     * @param vertices  the array to copy the vertices to
     */
    public float[] getVertices(int srcOffset,
                               float[] vertices) {
        return getVertices(srcOffset, -1, vertices);
    }

    /**
     * Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold count vertices.
     *
     * @param srcOffset the offset (in number of floats) of the vertices in the mesh to copy
     * @param count     the amount of floats to copy
     * @param vertices  the array to copy the vertices to
     */
    public float[] getVertices(int srcOffset,
                               int count,
                               float[] vertices) {
        return getVertices(srcOffset, count, vertices, 0);
    }

    /**
     * Copies the specified vertices from the Mesh to the float array. The float array must be large enough to hold
     * destOffset+count vertices.
     *
     * @param srcOffset  the offset (in number of floats) of the vertices in the mesh to copy
     * @param count      the amount of floats to copy
     * @param vertices   the array to copy the vertices to
     * @param destOffset the offset (in floats) in the vertices array to start copying
     */
    public float[] getVertices(int srcOffset,
                               int count,
                               float[] vertices,
                               int destOffset) {
        // TODO: Perhaps this method should be vertexSize aware??
        final int max = getNumVertices() * getVertexSize() / 4;
        if (count == -1) {
            count = max - srcOffset;
            if (count > vertices.length - destOffset)
                count = vertices.length - destOffset;
        }
        if (srcOffset < 0 || count <= 0 || (srcOffset + count) > max || destOffset < 0 || destOffset >= vertices.length)
            throw new IndexOutOfBoundsException();
        if ((vertices.length - destOffset) < count)
            throw new IllegalArgumentException("not enough room in vertices array, has " + vertices.length + " floats, needs " + count);
        int pos = getVerticesBuffer().position();
        getVerticesBuffer().position(srcOffset);
        getVerticesBuffer().get(vertices, destOffset, count);
        getVerticesBuffer().position(pos);
        return vertices;
    }

    /**
     * Sets the per-instance attributes of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instance the vertices.
     *
     * @return the mesh for invocation chaining.
     */
    public IntMesh setInstance(float[] instance) {
        ((VertexBufferObjectInstanced) this.vertices).setInstance(instance, 0, instance.length);

        return this;
    }

    /**
     * Sets the per-instance attributes of this Mesh. The attributes are assumed to be given in float format.
     *
     * @param instance The vertices.
     * @param offset   The offset.
     * @param count    The number of floats to use.
     */
    public void setInstanceAttribs(float[] instance,
                                   int offset,
                                   int count) {
        ((VertexBufferObjectInstanced) this.vertices).setInstance(instance, offset, count);
    }

    /**
     * Sets the indices of this Mesh
     *
     * @param indices the indices
     *
     * @return the mesh for invocation chaining.
     */
    public IntMesh setIndices(int[] indices) {
        this.indices.setIndices(indices, 0, indices.length);

        return this;
    }

    /**
     * Sets the indices of this Mesh.
     *
     * @param indices the indices
     * @param offset  the offset into the indices array
     * @param count   the number of indices to copy
     *
     * @return the mesh for invocation chaining.
     */
    public IntMesh setIndices(int[] indices,
                              int offset,
                              int count) {
        this.indices.setIndices(indices, offset, count);

        return this;
    }

    /**
     * Copies the indices from the Mesh to the int array. The int array must be large enough to hold all the Mesh's indices.
     *
     * @param indices the array to copy the indices to
     */
    public void getIndices(int[] indices) {
        getIndices(indices, 0);
    }

    /**
     * Copies the indices from the Mesh to the int array. The int array must be large enough to hold destOffset + all the
     * Mesh's indices.
     *
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    public void getIndices(int[] indices,
                           int destOffset) {
        getIndices(0, indices, destOffset);
    }

    /**
     * Copies the remaining indices from the Mesh to the int array. The int array must be large enough to hold destOffset + all
     * the remaining indices.
     *
     * @param srcOffset  the zero-based offset of the first index to fetch
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    public void getIndices(int srcOffset,
                           int[] indices,
                           int destOffset) {
        getIndices(srcOffset, -1, indices, destOffset);
    }

    /**
     * Copies the indices from the Mesh to the int array. The int array must be large enough to hold destOffset + count
     * indices.
     *
     * @param srcOffset  the zero-based offset of the first index to fetch
     * @param count      the total amount of indices to copy
     * @param indices    the array to copy the indices to
     * @param destOffset the offset in the indices array to start copying
     */
    public void getIndices(int srcOffset,
                           int count,
                           int[] indices,
                           int destOffset) {
        int max = getNumIndices();
        if (count < 0)
            count = max - srcOffset;
        if (srcOffset < 0 || srcOffset >= max || srcOffset + count > max)
            throw new IllegalArgumentException("Invalid range specified, offset: " + srcOffset + ", count: " + count + ", max: " + max);
        if ((indices.length - destOffset) < count)
            throw new IllegalArgumentException("not enough room in indices array, has " + indices.length + " ints, needs " + count);
        int pos = getIndicesBuffer().position();
        getIndicesBuffer().position(srcOffset);
        getIndicesBuffer().get(indices, destOffset, count);
        getIndicesBuffer().position(pos);
    }

    /** @return the number of defined indices */
    public int getNumIndices() {
        return indices != null ? indices.getNumIndices() : 0;
    }

    /** @return the number of defined vertices */
    public int getNumVertices() {
        return vertices != null ? vertices.getNumVertices() : 0;
    }

    /** @return the maximum number of vertices this mesh can hold */
    public int getMaxVertices() {
        return vertices != null ? vertices.getNumMaxVertices() : 0;
    }

    /** @return the maximum number of indices this mesh can hold */
    public int getMaxIndices() {
        return indices != null ? indices.getNumMaxIndices() : 0;
    }

    /** @return the size of a single vertex in bytes */
    public int getVertexSize() {
        return vertices.getAttributes().vertexSize;
    }

    /**
     * Sets whether to bind the underlying {@link VertexArray} or {@link VertexBufferObject} automatically on a call to one of the
     * render methods. Usually you want to use autobind. Manual binding is an expert functionality. There is a driver bug on the
     * MSM720xa chips that will fuck up memory if you manipulate the vertices and indices of a Mesh multiple times while it is
     * bound. Keep this in mind.
     *
     * @param autoBind whether to autobind meshes.
     */
    public void setAutoBind(boolean autoBind) {
        this.autoBind = autoBind;
    }

    /**
     * Binds the underlying {@link VertexBufferObject} and {@link IntIndexBufferObject} if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     *
     * @param shader the shader (does not bind the shader)
     */
    public void bind(final ExtShaderProgram shader) {
        bind(shader, null);
    }

    /**
     * Binds the underlying {@link VertexBufferObject} and {@link IntIndexBufferObject} if indices where given. Use this with OpenGL
     * ES 2.0 and when auto-bind is disabled.
     *
     * @param shader    the shader (does not bind the shader)
     * @param locations array containing the attribute locations.
     */
    public void bind(final ExtShaderProgram shader,
                     final int[] locations) {
        vertices.bind(shader, locations);
        if (indices != null && indices.getNumIndices() > 0)
            indices.bind();
    }

    /**
     * Unbinds the underlying {@link VertexBufferObject} and {@link IntIndexBufferObject} is indices were given. Use this with OpenGL
     * ES 1.x and when auto-bind is disabled.
     *
     * @param shader the shader (does not unbind the shader)
     */
    public void unbind(final ExtShaderProgram shader) {
        unbind(shader, null);
    }

    /**
     * Unbinds the underlying {@link VertexBufferObject} and {@link IntIndexBufferObject} is indices were given. Use this with OpenGL
     * ES 1.x and when auto-bind is disabled.
     *
     * @param shader    the shader (does not unbind the shader)
     * @param locations array containing the attribute locations.
     */
    public void unbind(final ExtShaderProgram shader,
                       final int[] locations) {
        vertices.unbind(shader, locations);
        if (indices != null && indices.getNumIndices() > 0)
            indices.unbind();
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type. If indices are set for this mesh then getNumIndices() / #vertices per
     * primitive primitives are rendered. If no indices are set then getNumVertices() / #vertices per primitive are rendered.
     * </p>
     *
     * <p>
     * This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     * </p>
     *
     * <p>
     * This method must only be called after the {@link ExtShaderProgram#begin()} method has been called!
     * </p>
     *
     * <p>
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     * </p>
     *
     * @param primitiveType the primitive type
     */
    public void render(ExtShaderProgram shader,
                       int primitiveType) {
        render(shader, primitiveType, 0, indices.getNumMaxIndices() > 0 ? getNumIndices() : getNumVertices(), autoBind);
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     *
     * <p>
     * This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     * </p>
     *
     * <p>
     * This method must only be called after the {@link ExtShaderProgram#begin()} method has been called!
     * </p>
     *
     * <p>
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     * </p>
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     */
    public void render(ExtShaderProgram shader,
                       int primitiveType,
                       int offset,
                       int count) {
        render(shader, primitiveType, offset, count, autoBind);
    }

    /**
     * <p>
     * Renders the mesh with instanced rendering using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     * @param instanceCount number of instances
     */
    public void render(ExtShaderProgram shader,
                       int primitiveType,
                       int offset,
                       int count,
                       int instanceCount) {
        render(shader, primitiveType, offset, count, instanceCount, autoBind);
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     *
     * <p>
     * This method will automatically bind each vertex attribute as specified at construction time via {@link VertexAttributes} to
     * the respective shader attributes. The binding is based on the alias defined for each VertexAttribute.
     * </p>
     *
     * <p>
     * This method must only be called after the {@link ExtShaderProgram#begin()} method has been called!
     * </p>
     *
     * <p>
     * This method is intended for use with OpenGL ES 2.0 and will throw an IllegalStateException when OpenGL ES 1.x is used.
     * </p>
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     * @param autoBind      overrides the autoBind member of this Mesh
     */
    public void render(ExtShaderProgram shader,
                       int primitiveType,
                       int offset,
                       int count,
                       boolean autoBind) {
        if (count == 0)
            return;

        if (autoBind)
            bind(shader);

        if (isVertexArray) {
            if (indices.getNumIndices() > 0) {
                IntBuffer buffer = indices.getBuffer();
                int oldPosition = buffer.position();
                int oldLimit = buffer.limit();
                buffer.position(offset);
                buffer.limit(offset + count);
                Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_INT, buffer);
                buffer.position(oldPosition);
                buffer.limit(oldLimit);
            } else {
                Gdx.gl20.glDrawArrays(primitiveType, offset, count);
            }
        } else {
            if (indices.getNumIndices() > 0) {
                if (count + offset > indices.getNumMaxIndices()) {
                    throw new GdxRuntimeException("Mesh attempting to access memory outside of the index buffer (count: " + count + ", offset: " + offset + ", max: "
                                                          + indices.getNumMaxIndices() + ")");
                }

                Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_INT, offset * 4);
            } else {
                Gdx.gl20.glDrawArrays(primitiveType, offset, count);
            }
        }

        if (autoBind)
            unbind(shader);
    }

    /**
     * <p>
     * Renders the mesh using the given primitive type using instanced rendering. offset specifies the offset into either the vertex buffer or the index
     * buffer depending on whether indices are defined. count specifies the number of vertices or indices to use thus count /
     * #vertices per primitive primitives are rendered.
     * </p>
     *
     * @param shader        the shader to be used
     * @param primitiveType the primitive type
     * @param offset        the offset into the vertex or index buffer
     * @param count         number of vertices or indices to use
     * @param instanceCount number of instances
     * @param autoBind      overrides the autoBind member of this Mesh
     */
    public void render(ExtShaderProgram shader,
                       int primitiveType,
                       int offset,
                       int count,
                       int instanceCount,
                       boolean autoBind) {
        if (count == 0)
            return;

        if (autoBind)
            bind(shader);

        if (indices != null && indices.getNumIndices() > 0) {
            if (count + offset > indices.getNumMaxIndices()) {
                throw new GdxRuntimeException("Mesh attempting to access memory outside of the index buffer (count: " + count + ", offset: " + offset + ", max: "
                                                      + indices.getNumMaxIndices() + ")");
            }
            Gdx.gl30.glDrawElementsInstanced(primitiveType, count, GL20.GL_UNSIGNED_INT, offset * 4, instanceCount);
        } else {
            Gdx.gl30.glDrawArraysInstanced(primitiveType, offset, count, instanceCount);
        }

        if (autoBind)
            unbind(shader);
    }

    /** Frees all resources associated with this Mesh */
    public void dispose() {
        if (meshes.get(Gdx.app) != null)
            meshes.get(Gdx.app).removeValue(this, true);
        vertices.dispose();
        if (indices != null)
            indices.dispose();
    }

    /**
     * Returns the first {@link VertexAttribute} having the given {@link Usage}.
     *
     * @param usage the Usage.
     *
     * @return the VertexAttribute or null if no attribute with that usage was found.
     */
    public VertexAttribute getVertexAttribute(int usage) {
        VertexAttributes attributes = vertices.getAttributes();
        int len = attributes.size();
        for (int i = 0; i < len; i++)
            if (attributes.get(i).usage == usage)
                return attributes.get(i);

        return null;
    }

    public VertexAttribute getInstancedAttribute(int usage) {
        VertexAttributes attributes = ((VertexBufferObjectInstanced) vertices).getInstanceAttributes();
        int len = attributes.size();
        for (int i = 0; i < len; i++)
            if (attributes.get(i).usage == usage)
                return attributes.get(i);

        return null;
    }

    /** @return the vertex attributes of this Mesh */
    public VertexAttributes getVertexAttributes() {
        return vertices.getAttributes();
    }

    /** @return the vertex attributes of this Mesh */
    public VertexAttributes getInstanceAttributes() {
        return ((VertexBufferObjectInstanced) vertices).getInstanceAttributes();
    }

    /** @return the backing FloatBuffer holding the vertices. Does not have to be a direct buffer on Android! */
    public FloatBuffer getVerticesBuffer() {
        return vertices.getBuffer();
    }

    /**
     * Calculates the {@link BoundingBox} of the vertices contained in this mesh. In case no vertices are defined yet a
     * {@link GdxRuntimeException} is thrown. This method creates a new BoundingBox instance.
     *
     * @return the bounding box.
     */
    public BoundingBox calculateBoundingBox() {
        BoundingBox bbox = new BoundingBox();
        calculateBoundingBox(bbox);
        return bbox;
    }

    /**
     * Calculates the {@link BoundingBox} of the vertices contained in this mesh. In case no vertices are defined yet a
     * {@link GdxRuntimeException} is thrown.
     *
     * @param bbox the bounding box to store the result in.
     */
    public void calculateBoundingBox(BoundingBox bbox) {
        final int numVertices = getNumVertices();
        if (numVertices == 0)
            throw new GdxRuntimeException("No vertices defined");

        final FloatBuffer verts = vertices.getBuffer();
        bbox.inf();
        final VertexAttribute posAttrib = getVertexAttribute(Usage.Position);
        final int offset = posAttrib.offset / 4;
        final int vertexSize = vertices.getAttributes().vertexSize / 4;
        int idx = offset;

        switch (posAttrib.numComponents) {
        case 1:
            for (int i = 0; i < numVertices; i++) {
                bbox.ext(verts.get(idx), 0, 0);
                idx += vertexSize;
            }
            break;
        case 2:
            for (int i = 0; i < numVertices; i++) {
                bbox.ext(verts.get(idx), verts.get(idx + 1), 0);
                idx += vertexSize;
            }
            break;
        case 3:
            for (int i = 0; i < numVertices; i++) {
                bbox.ext(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                idx += vertexSize;
            }
            break;
        }
    }

    /**
     * Calculate the {@link BoundingBox} of the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     *
     * @return the value specified by out.
     */
    public BoundingBox calculateBoundingBox(final BoundingBox out,
                                            int offset,
                                            int count) {
        return extendBoundingBox(out.inf(), offset, count);
    }

    /**
     * Calculate the {@link BoundingBox} of the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     *
     * @return the value specified by out.
     */
    public BoundingBox calculateBoundingBox(final BoundingBox out,
                                            int offset,
                                            int count,
                                            final Matrix4 transform) {
        return extendBoundingBox(out.inf(), offset, count, transform);
    }

    /**
     * Extends the specified {@link BoundingBox} with the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     *
     * @return the value specified by out.
     */
    public BoundingBox extendBoundingBox(final BoundingBox out,
                                         int offset,
                                         int count) {
        return extendBoundingBox(out, offset, count, null);
    }

    /**
     * Extends the specified {@link BoundingBox} with the specified part.
     *
     * @param out    the bounding box to store the result in.
     * @param offset the start of the part.
     * @param count  the size of the part.
     *
     * @return the value specified by out.
     */
    public BoundingBox extendBoundingBox(final BoundingBox out,
                                         int offset,
                                         int count,
                                         final Matrix4 transform) {
        final int numIndices = getNumIndices();
        final int numVertices = getNumVertices();
        final int max = numIndices == 0 ? numVertices : numIndices;
        if (offset < 0 || count < 1 || offset + count > max)
            throw new GdxRuntimeException("Invalid part specified ( offset=" + offset + ", count=" + count + ", max=" + max + " )");

        final FloatBuffer verts = vertices.getBuffer();
        final IntBuffer index = indices.getBuffer();
        final VertexAttribute posAttrib = getVertexAttribute(Usage.Position);
        final int posoff = posAttrib.offset / 4;
        final int vertexSize = vertices.getAttributes().vertexSize / 4;
        final int end = offset + count;

        switch (posAttrib.numComponents) {
        case 1:
            if (numIndices > 0) {
                for (int i = offset; i < end; i++) {
                    final int idx = index.get(i) * vertexSize + posoff;
                    tmpV.set(verts.get(idx), 0, 0);
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            } else {
                for (int i = offset; i < end; i++) {
                    final int idx = i * vertexSize + posoff;
                    tmpV.set(verts.get(idx), 0, 0);
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            }
            break;
        case 2:
            if (numIndices > 0) {
                for (int i = offset; i < end; i++) {
                    final int idx = index.get(i) * vertexSize + posoff;
                    tmpV.set(verts.get(idx), verts.get(idx + 1), 0);
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            } else {
                for (int i = offset; i < end; i++) {
                    final int idx = i * vertexSize + posoff;
                    tmpV.set(verts.get(idx), verts.get(idx + 1), 0);
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            }
            break;
        case 3:
            if (numIndices > 0) {
                for (int i = offset; i < end; i++) {
                    final int idx = index.get(i) * vertexSize + posoff;
                    tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            } else {
                for (int i = offset; i < end; i++) {
                    final int idx = i * vertexSize + posoff;
                    tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                    if (transform != null)
                        tmpV.mul(transform);
                    out.ext(tmpV);
                }
            }
            break;
        }
        return out;
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadiusSquared(final float centerX,
                                        final float centerY,
                                        final float centerZ,
                                        int offset,
                                        int count,
                                        final Matrix4 transform) {
        int numIndices = getNumIndices();
        if (offset < 0 || count < 1 || offset + count > numIndices)
            throw new GdxRuntimeException("Not enough indices");

        final FloatBuffer verts = vertices.getBuffer();
        final IntBuffer index = indices.getBuffer();
        final VertexAttribute posAttrib = getVertexAttribute(Usage.Position);
        final int posoff = posAttrib.offset / 4;
        final int vertexSize = vertices.getAttributes().vertexSize / 4;
        final int end = offset + count;

        float result = 0;

        switch (posAttrib.numComponents) {
        case 1:
            for (int i = offset; i < end; i++) {
                final int idx = index.get(i) * vertexSize + posoff;
                tmpV.set(verts.get(idx), 0, 0);
                if (transform != null)
                    tmpV.mul(transform);
                final float r = tmpV.sub(centerX, centerY, centerZ).len2();
                if (r > result)
                    result = r;
            }
            break;
        case 2:
            for (int i = offset; i < end; i++) {
                final int idx = index.get(i) * vertexSize + posoff;
                tmpV.set(verts.get(idx), verts.get(idx + 1), 0);
                if (transform != null)
                    tmpV.mul(transform);
                final float r = tmpV.sub(centerX, centerY, centerZ).len2();
                if (r > result)
                    result = r;
            }
            break;
        case 3:
            for (int i = offset; i < end; i++) {
                final int idx = index.get(i) * vertexSize + posoff;
                tmpV.set(verts.get(idx), verts.get(idx + 1), verts.get(idx + 2));
                if (transform != null)
                    tmpV.mul(transform);
                final float r = tmpV.sub(centerX, centerY, centerZ).len2();
                if (r > result)
                    result = r;
            }
            break;
        }
        return result;
    }

    /**
     * Calculates the radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     *
     * @return the radius of the bounding sphere.
     */
    public float calculateRadius(final float centerX,
                                 final float centerY,
                                 final float centerZ,
                                 int offset,
                                 int count,
                                 final Matrix4 transform) {
        return (float) FastMath.sqrt(calculateRadiusSquared(centerX, centerY, centerZ, offset, count, transform));
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadius(final Vector3 center,
                                 int offset,
                                 int count,
                                 final Matrix4 transform) {
        return calculateRadius(center.x, center.y, center.z, offset, count, transform);
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     * @param offset  the start index of the part.
     * @param count   the amount of indices the part contains.
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadius(final float centerX,
                                 final float centerY,
                                 final float centerZ,
                                 int offset,
                                 int count) {
        return calculateRadius(centerX, centerY, centerZ, offset, count, null);
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     * @param offset the start index of the part.
     * @param count  the amount of indices the part contains.
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadius(final Vector3 center,
                                 int offset,
                                 int count) {
        return calculateRadius(center.x, center.y, center.z, offset, count, null);
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param centerX The X coordinate of the center of the bounding sphere
     * @param centerY The Y coordinate of the center of the bounding sphere
     * @param centerZ The Z coordinate of the center of the bounding sphere
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadius(final float centerX,
                                 final float centerY,
                                 final float centerZ) {
        return calculateRadius(centerX, centerY, centerZ, 0, getNumIndices(), null);
    }

    /**
     * Calculates the squared radius of the bounding sphere around the specified center for the specified part.
     *
     * @param center The center of the bounding sphere
     *
     * @return the squared radius of the bounding sphere.
     */
    public float calculateRadius(final Vector3 center) {
        return calculateRadius(center.x, center.y, center.z, 0, getNumIndices(), null);
    }

    /** @return the backing intbuffer holding the indices. Does not have to be a direct buffer on Android! */
    public IntBuffer getIndicesBuffer() {
        return indices.getBuffer();
    }

    /**
     * Method to scale the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with care.
     * It will also create a temporary float[] which will be garbage collected.
     *
     * @param scaleX scale on x
     * @param scaleY scale on y
     * @param scaleZ scale on z
     */
    public void scale(float scaleX,
                      float scaleY,
                      float scaleZ) {
        final VertexAttribute posAttr = getVertexAttribute(Usage.Position);
        final int offset = posAttr.offset / 4;
        final int numComponents = posAttr.numComponents;
        final int numVertices = getNumVertices();
        final int vertexSize = getVertexSize() / 4;

        final float[] vertices = new float[numVertices * vertexSize];
        getVertices(vertices);

        int idx = offset;
        switch (numComponents) {
        case 1:
            for (int i = 0; i < numVertices; i++) {
                vertices[idx] *= scaleX;
                idx += vertexSize;
            }
            break;
        case 2:
            for (int i = 0; i < numVertices; i++) {
                vertices[idx] *= scaleX;
                vertices[idx + 1] *= scaleY;
                idx += vertexSize;
            }
            break;
        case 3:
            for (int i = 0; i < numVertices; i++) {
                vertices[idx] *= scaleX;
                vertices[idx + 1] *= scaleY;
                vertices[idx + 2] *= scaleZ;
                idx += vertexSize;
            }
            break;
        }

        setVertices(vertices);
    }

    /**
     * Method to transform the positions in the mesh. Normals will be kept as is. This is a potentially slow operation, use with
     * care. It will also create a temporary float[] which will be garbage collected.
     *
     * @param matrix the transformation matrix
     */
    public void transform(final Matrix4 matrix) {
        transform(matrix, 0, getNumVertices());
    }

    // TODO: Protected for now, because transforming a portion works but still copies all vertices
    public void transform(final Matrix4 matrix,
                          final int start,
                          final int count) {
        final VertexAttribute posAttr = getVertexAttribute(Usage.Position);
        final int posOffset = posAttr.offset / 4;
        final int stride = getVertexSize() / 4;
        final int numComponents = posAttr.numComponents;
        final int numVertices = getNumVertices();

        final float[] vertices = new float[count * stride];
        getVertices(start * stride, count * stride, vertices);
        // getVertices(0, vertices.length, vertices);
        transform(matrix, vertices, stride, posOffset, numComponents, 0, count);
        // setVertices(vertices, 0, vertices.length);
        updateVertices(start * stride, vertices);
    }

    /**
     * Method to transform the texture coordinates in the mesh. This is a potentially slow operation, use with care. It will also
     * create a temporary float[] which will be garbage collected.
     *
     * @param matrix the transformation matrix
     */
    public void transformUV(final Matrix3 matrix) {
        transformUV(matrix, 0, getNumVertices());
    }

    // TODO: Protected for now, because transforming a portion works but still copies all vertices
    protected void transformUV(final Matrix3 matrix,
                               final int start,
                               final int count) {
        final VertexAttribute posAttr = getVertexAttribute(Usage.TextureCoordinates);
        final int offset = posAttr.offset / 4;
        final int vertexSize = getVertexSize() / 4;
        final int numVertices = getNumVertices();

        final float[] vertices = new float[numVertices * vertexSize];
        // TODO: getVertices(vertices, start * vertexSize, count * vertexSize);
        getVertices(0, vertices.length, vertices);
        transformUV(matrix, vertices, vertexSize, offset, start, count);
        setVertices(vertices, 0, vertices.length);
        // TODO: setVertices(start * vertexSize, vertices, 0, vertices.length);
    }

    /**
     * Copies this mesh optionally removing duplicate vertices and/or reducing the amount of attributes.
     *
     * @param isStatic         whether the new mesh is static or not. Allows for internal optimizations.
     * @param removeDuplicates whether to remove duplicate vertices if possible. Only the vertices specified by usage are checked.
     * @param usage            which attributes (if available) to copy
     *
     * @return the copy of this mesh
     */
    public IntMesh copy(boolean isStatic,
                        boolean removeDuplicates,
                        final int[] usage) {
        // TODO move this to a copy constructor?
        // TODO duplicate the buffers without double copying the data if possible.
        // TODO perhaps move this code to JNI if it turns out being too slow.
        final int vertexSize = getVertexSize() / 4;
        int numVertices = getNumVertices();
        float[] vertices = new float[numVertices * vertexSize];
        getVertices(0, vertices.length, vertices);
        int[] checks = null;
        VertexAttribute[] attrs = null;
        int newVertexSize = 0;
        if (usage != null) {
            int size = 0;
            int as = 0;
            for (int i = 0; i < usage.length; i++)
                if (getVertexAttribute(usage[i]) != null) {
                    size += getVertexAttribute(usage[i]).numComponents;
                    as++;
                }
            if (size > 0) {
                attrs = new VertexAttribute[as];
                checks = new int[size];
                int idx = -1;
                int ai = -1;
                for (int i = 0; i < usage.length; i++) {
                    VertexAttribute a = getVertexAttribute(usage[i]);
                    if (a == null)
                        continue;
                    for (int j = 0; j < a.numComponents; j++)
                        checks[++idx] = (a.offset + j);
                    attrs[++ai] = a.copy();
                    newVertexSize += a.numComponents;
                }
            }
        }
        if (checks == null) {
            checks = new int[vertexSize];
            for (int i = 0; i < vertexSize; i++)
                checks[i] = i;
            newVertexSize = vertexSize;
        }

        int numIndices = getNumIndices();
        int[] indices = null;
        if (numIndices > 0) {
            indices = new int[numIndices];
            getIndices(indices);
            if (removeDuplicates || newVertexSize != vertexSize) {
                float[] tmp = new float[vertices.length];
                int size = 0;
                for (int i = 0; i < numIndices; i++) {
                    final int idx1 = indices[i] * vertexSize;
                    int newIndex = -1;
                    if (removeDuplicates) {
                        for (int j = 0; j < size && newIndex < 0; j++) {
                            final int idx2 = j * newVertexSize;
                            boolean found = true;
                            for (int k = 0; k < checks.length && found; k++) {
                                if (tmp[idx2 + k] != vertices[idx1 + checks[k]])
                                    found = false;
                            }
                            if (found)
                                newIndex = j;
                        }
                    }
                    if (newIndex > 0)
                        indices[i] = newIndex;
                    else {
                        final int idx = size * newVertexSize;
                        for (int j = 0; j < checks.length; j++)
                            tmp[idx + j] = vertices[idx1 + checks[j]];
                        indices[i] = size;
                        size++;
                    }
                }
                vertices = tmp;
                numVertices = size;
            }
        }

        IntMesh result;
        if (attrs == null)
            result = new IntMesh(isStatic, numVertices, indices == null ? 0 : indices.length, getVertexAttributes());
        else
            result = new IntMesh(isStatic, numVertices, indices == null ? 0 : indices.length, attrs);
        result.setVertices(vertices, 0, numVertices * newVertexSize);
        if (indices != null)
            result.setIndices(indices);
        return result;
    }

    /**
     * Copies this mesh.
     *
     * @param isStatic whether the new mesh is static or not. Allows for internal optimizations.
     *
     * @return the copy of this mesh
     */
    public IntMesh copy(boolean isStatic) {
        return copy(isStatic, false, null);
    }

    public enum VertexDataType {
        VertexArray,
        VertexBufferObject,
        VertexBufferObjectSubData,
        VertexBufferObjectWithVAO
    }
}
