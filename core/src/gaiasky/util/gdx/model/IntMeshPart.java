/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gdx.model;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.shader.ExtShaderProgram;

public class IntMeshPart {
    /** Temporary static {@link BoundingBox} instance, used in the {@link #update()} method. **/
    private final static BoundingBox bounds = new BoundingBox();
    /** The offset to the center of the bounding box of the shape, only valid after the call to {@link #update()}. **/
    public final Vector3 center = new Vector3();
    /**
     * The location, relative to {@link #center}, of the corner of the axis aligned bounding box of the shape. Or, in other words:
     * half the dimensions of the bounding box of the shape, where {@link Vector3#x} is half the width, {@link Vector3#y} is half
     * the height and {@link Vector3#z} is half the depth. Only valid after the call to {@link #update()}.
     **/
    public final Vector3 halfExtents = new Vector3();
    /** Unique id within model, may be null. Will be ignored by {@link #equals(IntMeshPart)} **/
    public String id;
    /**
     * The primitive type, OpenGL constant e.g: {@link GL20#GL_TRIANGLES}, {@link GL20#GL_POINTS}, {@link GL20#GL_LINES},
     * {@link GL20#GL_LINE_STRIP}, {@link GL20#GL_TRIANGLE_STRIP}
     **/
    public int primitiveType;
    /**
     * The offset in the {@link #mesh} to this part. If the mesh is indexed ({@link Mesh#getNumIndices()} > 0), this is the offset
     * in the indices array, otherwise it is the offset in the vertices array.
     **/
    public int offset;
    /**
     * The size (in total number of vertices) of this part in the {@link #mesh}. When the mesh is indexed (
     * {@link Mesh#getNumIndices()} > 0), this is the number of indices, otherwise it is the number of vertices.
     **/
    public int size;
    /** The Mesh the part references, also stored in {@link IntModel} **/
    public IntMesh mesh;
    /**
     * The radius relative to {@link #center} of the bounding sphere of the shape, or negative if not calculated yet. This is the
     * same as the length of the {@link #halfExtents} member. See {@link #update()}.
     **/
    public float radius = -1;

    /** Construct a new MeshPart, with null values. The MeshPart is unusable until you set all members. **/
    public IntMeshPart() {
    }

    public IntMeshPart(MeshPart other) {
        this.primitiveType = other.primitiveType;
        this.center.set(other.center);
        this.halfExtents.set(other.halfExtents);
        this.id = other.id;
        this.offset = other.offset;
        this.size = other.size;
        this.mesh = new IntMesh(other.mesh);
        this.radius = other.radius;
    }

    /**
     * Construct a new MeshPart and set all its values.
     *
     * @param id     The id of the new part, may be null.
     * @param mesh   The mesh which holds all vertices and (optional) indices of this part.
     * @param offset The offset within the mesh to this part.
     * @param size   The size (in total number of vertices) of the part.
     * @param type   The primitive type of the part (e.g. GL_TRIANGLES, GL_LINE_STRIP, etc.).
     */
    public IntMeshPart(final String id, final IntMesh mesh, final int offset, final int size, final int type) {
        set(id, mesh, offset, size, type);
    }

    /**
     * Construct a new MeshPart which is an exact copy of the provided MeshPart.
     *
     * @param copyFrom The MeshPart to copy.
     */
    public IntMeshPart(final IntMeshPart copyFrom) {
        set(copyFrom);
    }

    /**
     * Set this MeshPart to be a copy of the other MeshPart
     *
     * @param other The MeshPart from which to copy the values
     *
     * @return this MeshPart, for chaining
     */
    public IntMeshPart set(final IntMeshPart other) {
        this.id = other.id;
        this.mesh = other.mesh;
        this.offset = other.offset;
        this.size = other.size;
        this.primitiveType = other.primitiveType;
        this.center.set(other.center);
        this.halfExtents.set(other.halfExtents);
        this.radius = other.radius;
        return this;
    }

    /**
     * Set this MeshPart to given values, does not {@link #update()} the bounding box values.
     *
     * @return this MeshPart, for chaining.
     */
    public IntMeshPart set(final String id, final IntMesh mesh, final int offset, final int size, final int type) {
        this.id = id;
        this.mesh = mesh;
        this.offset = offset;
        this.size = size;
        this.primitiveType = type;
        this.center.set(0, 0, 0);
        this.halfExtents.set(0, 0, 0);
        this.radius = -1f;
        return this;
    }

    /**
     * Calculates and updates the {@link #center}, {@link #halfExtents} and {@link #radius} values. This is considered a costly
     * operation and should not be called frequently. All vertices (points) of the shape are traversed to calculate the maximum and
     * minimum x, y and z coordinate of the shape. Note that MeshPart is not aware of any transformation that might be applied when
     * rendering. It calculates the untransformed (not moved, not scaled, not rotated) values.
     */
    public void update() {
        mesh.calculateBoundingBox(bounds, offset, size);
        bounds.getCenter(center);
        bounds.getDimensions(halfExtents).scl(0.5f);
        radius = halfExtents.len();
    }

    /**
     * Compares this MeshPart to the specified MeshPart and returns true if they both reference the same {@link IntMesh} and the
     * {@link #offset}, {@link #size} and {@link #primitiveType} members are equal. The {@link #id} member is ignored.
     *
     * @param other The other IntMeshPart to compare this IntMeshPart to.
     *
     * @return True when this IntMeshPart equals the other IntMeshPart (ignoring the {@link #id} member), false otherwise.
     */
    public boolean equals(final IntMeshPart other) {
        return other == this
                || (other != null && other.mesh == mesh && other.primitiveType == primitiveType && other.offset == offset && other.size == size);
    }

    @Override
    public boolean equals(final Object arg0) {
        if (arg0 == null)
            return false;
        if (arg0 == this)
            return true;
        if (!(arg0 instanceof IntMeshPart))
            return false;
        return equals((IntMeshPart) arg0);
    }

    /**
     * Renders the mesh part using the specified shader, must be called in between {@link ExtShaderProgram#begin()} and
     * {@link ExtShaderProgram#end()}.
     *
     * @param shader   the shader to be used
     * @param autoBind overrides the autoBind member of the Mesh
     */
    public void render(ExtShaderProgram shader, boolean autoBind) {
        mesh.render(shader, primitiveType, offset, size, autoBind);
    }

    /**
     * Renders the mesh part using the specified shader, must be called in between {@link ExtShaderProgram#begin()} and
     * {@link ExtShaderProgram#end()}.
     *
     * @param shader the shader to be used
     */
    public void render(ExtShaderProgram shader) {
        mesh.render(shader, primitiveType, offset, size);
    }
}
