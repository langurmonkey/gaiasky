/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import java.io.Serializable;

public class RayDouble implements Serializable {
    private static final long serialVersionUID = -620692054835390878L;
    static Vector3D tmp = new Vector3D();
    public final Vector3D origin = new Vector3D();
    public final Vector3D direction = new Vector3D();

    /**
     * Constructor, sets the starting position of the Rayd and the direction.
     *
     * @param origin    The starting position
     * @param direction The direction
     */
    public RayDouble(Vector3D origin, Vector3D direction) {
        this.origin.set(origin);
        this.direction.set(direction).nor();
    }

    /** @return a copy of this Rayd. */
    public RayDouble cpy() {
        return new RayDouble(this.origin, this.direction);
    }

    /**
     * @param distance The distance from the end point to the start point.
     *
     * @return The end point
     *
     * @deprecated Use {@link #getEndPoint(Vector3D, float)} instead. Returns the endpoint given the distance. This is calculated as
     * startpoint + distance * direction.
     */
    @Deprecated
    public Vector3D getEndPoint(float distance) {
        return getEndPoint(new Vector3D(), distance);
    }

    /**
     * Returns the endpoint given the distance. This is calculated as startpoint + distance * direction.
     *
     * @param out      The vector to set to the result
     * @param distance The distance from the end point to the start point.
     *
     * @return The out param
     */
    public Vector3D getEndPoint(final Vector3D out, final float distance) {
        return out.set(direction).scl(distance).add(origin);
    }

    /**
     * Multiplies the Rayd by the given matrix. Use this to transform a Rayd into another coordinate system.
     *
     * @param matrix The matrix
     *
     * @return This Rayd for chaining.
     */
    public RayDouble mul(Matrix4D matrix) {
        tmp.set(origin).add(direction);
        tmp.mul(matrix);
        origin.mul(matrix);
        direction.set(tmp.sub(origin));
        return this;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Rayd [" + origin + ":" + direction + "]";
    }

    /**
     * Sets the starting position and the direction of this Rayd.
     *
     * @param origin    The starting position
     * @param direction The direction
     *
     * @return this Rayd for chaining
     */
    public RayDouble set(Vector3D origin, Vector3D direction) {
        this.origin.set(origin);
        this.direction.set(direction);
        return this;
    }

    /**
     * Sets this Rayd from the given starting position and direction.
     *
     * @param x  The x-component of the starting position
     * @param y  The y-component of the starting position
     * @param z  The z-component of the starting position
     * @param dx The x-component of the direction
     * @param dy The y-component of the direction
     * @param dz The z-component of the direction
     *
     * @return this Rayd for chaining
     */
    public RayDouble set(float x, float y, float z, float dx, float dy, float dz) {
        this.origin.set(x, y, z);
        this.direction.set(dx, dy, dz);
        return this;
    }

    /**
     * Sets the starting position and direction from the given Rayd
     *
     * @param Rayd The Rayd
     *
     * @return This Rayd for chaining
     */
    public RayDouble set(RayDouble Rayd) {
        this.origin.set(Rayd.origin);
        this.direction.set(Rayd.direction);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o == null || o.getClass() != this.getClass())
            return false;
        RayDouble r = (RayDouble) o;
        return this.direction.equals(r.direction) && this.origin.equals(r.origin);
    }

    @Override
    public int hashCode() {
        final int prime = 73;
        int result = 1;
        result = prime * result + this.direction.hashCode();
        result = prime * result + this.origin.hashCode();
        return result;
    }
}
