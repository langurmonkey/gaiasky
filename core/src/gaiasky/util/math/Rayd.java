/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import java.io.Serializable;

/**
 * Encapsulates a Rayd having a starting position and a unit length direction.
 *
 * @author badlogicgames@gmail.com
 */
public class Rayd implements Serializable {
    private static final long serialVersionUID = -620692054835390878L;
    static Vector3d tmp = new Vector3d();
    public final Vector3d origin = new Vector3d();
    public final Vector3d direction = new Vector3d();

    /**
     * Constructor, sets the starting position of the Rayd and the direction.
     *
     * @param origin    The starting position
     * @param direction The direction
     */
    public Rayd(Vector3d origin, Vector3d direction) {
        this.origin.set(origin);
        this.direction.set(direction).nor();
    }

    /** @return a copy of this Rayd. */
    public Rayd cpy() {
        return new Rayd(this.origin, this.direction);
    }

    /**
     * @param distance The distance from the end point to the start point.
     *
     * @return The end point
     *
     * @deprecated Use {@link #getEndPoint(Vector3d, float)} instead. Returns the endpoint given the distance. This is calculated as
     * startpoint + distance * direction.
     */
    @Deprecated
    public Vector3d getEndPoint(float distance) {
        return getEndPoint(new Vector3d(), distance);
    }

    /**
     * Returns the endpoint given the distance. This is calculated as startpoint + distance * direction.
     *
     * @param out      The vector to set to the result
     * @param distance The distance from the end point to the start point.
     *
     * @return The out param
     */
    public Vector3d getEndPoint(final Vector3d out, final float distance) {
        return out.set(direction).scl(distance).add(origin);
    }

    /**
     * Multiplies the Rayd by the given matrix. Use this to transform a Rayd into another coordinate system.
     *
     * @param matrix The matrix
     *
     * @return This Rayd for chaining.
     */
    public Rayd mul(Matrix4d matrix) {
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
    public Rayd set(Vector3d origin, Vector3d direction) {
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
    public Rayd set(float x, float y, float z, float dx, float dy, float dz) {
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
    public Rayd set(Rayd Rayd) {
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
        Rayd r = (Rayd) o;
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
