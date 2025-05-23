/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.collision.BoundingBox;
import net.jafama.FastMath;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class BoundingBoxDouble implements Serializable {
    @Serial private static final long serialVersionUID = -1286036817192127343L;

    private final static Vector3D tmpVector = new Vector3D();

    public final Vector3D min = new Vector3D();
    public final Vector3D max = new Vector3D();

    private final Vector3D cnt = new Vector3D();
    private final Vector3D dim = new Vector3D();

    @Deprecated
    private Vector3D[] corners;

    /**
     * Constructs a new bounding box with the minimum and maximum vector set to
     * zeros.
     */
    public BoundingBoxDouble() {
        clr();
    }

    /**
     * Constructs a new bounding box from the given bounding box.
     *
     * @param bounds The bounding box to copy
     */
    public BoundingBoxDouble(BoundingBoxDouble bounds) {
        this.set(bounds);
    }

    /**
     * Constructs the new bounding box using the given minimum and maximum
     * vector.
     *
     * @param minimum The minimum vector
     * @param maximum The maximum vector
     */
    public BoundingBoxDouble(Vector3D minimum, Vector3D maximum) {
        this.set(minimum, maximum);
    }

    static double min(final double a, final double b) {
        return FastMath.min(a, b);
    }

    static double max(final double a, final double b) {
        return FastMath.max(a, b);
    }

    /**
     * @return the center of the bounding box
     *
     * @deprecated Use {@link #getCenter(Vector3D)}
     */
    @Deprecated
    public Vector3D getCenter() {
        return cnt;
    }

    /**
     * @param out The {@link Vector3D} to receive the center of the bounding
     *            box.
     *
     * @return The vector specified with the out argument.
     */
    public Vector3D getCenter(Vector3D out) {
        return out.set(cnt);
    }

    public double getCenterX() {
        return cnt.x;
    }

    public double getCenterY() {
        return cnt.y;
    }

    public double getCenterZ() {
        return cnt.z;
    }

    @Deprecated
    protected void updateCorners() {
    }

    /**
     * @return the corners of this bounding box
     *
     * @deprecated Use the getCornerXYZ methods instead
     */
    @Deprecated
    public Vector3D[] getCorners() {
        if (corners == null) {
            corners = new Vector3D[8];
            for (int i = 0; i < 8; i++)
                corners[i] = new Vector3D();
        }
        corners[0].set(min.x, min.y, min.z);
        corners[1].set(max.x, min.y, min.z);
        corners[2].set(max.x, max.y, min.z);
        corners[3].set(min.x, max.y, min.z);
        corners[4].set(min.x, min.y, max.z);
        corners[5].set(max.x, min.y, max.z);
        corners[6].set(max.x, max.y, max.z);
        corners[7].set(min.x, max.y, max.z);
        return corners;
    }

    public Vector3D getCorner000(final Vector3D out) {
        return out.set(min.x, min.y, min.z);
    }

    public Vector3D getCorner001(final Vector3D out) {
        return out.set(min.x, min.y, max.z);
    }

    public Vector3D getCorner010(final Vector3D out) {
        return out.set(min.x, max.y, min.z);
    }

    public Vector3D getCorner011(final Vector3D out) {
        return out.set(min.x, max.y, max.z);
    }

    public Vector3D getCorner100(final Vector3D out) {
        return out.set(max.x, min.y, min.z);
    }

    public Vector3D getCorner101(final Vector3D out) {
        return out.set(max.x, min.y, max.z);
    }

    public Vector3D getCorner110(final Vector3D out) {
        return out.set(max.x, max.y, min.z);
    }

    public Vector3D getCorner111(final Vector3D out) {
        return out.set(max.x, max.y, max.z);
    }

    /**
     * @return The dimensions of this bounding box on all three axis
     *
     * @deprecated Use {@link #getDimensions(Vector3D)} instead
     */
    @Deprecated
    public Vector3D getDimensions() {
        return dim;
    }

    /**
     * @param out The {@link Vector3D} to receive the dimensions of this
     *            bounding box on all three axis.
     *
     * @return The vector specified with the out argument
     */
    public Vector3D getDimensions(final Vector3D out) {
        return out.set(dim);
    }

    public double getVolume() {
        return dim.x * dim.y * dim.z;
    }

    public double getWidth() {
        return dim.x;
    }

    public double getHeight() {
        return dim.y;
    }

    public double getDepth() {
        return dim.z;
    }

    public double getGreatestDim() {
       return FastMath.max(Math.max(getWidth(), getHeight()), getDepth());
    }

    /**
     * @return The minimum vector
     *
     * @deprecated Use {@link #getMin(Vector3D)} instead.
     */
    @Deprecated
    public Vector3D getMin() {
        return min;
    }

    /**
     * @param out The {@link Vector3D} to receive the minimum values.
     *
     * @return The vector specified with the out argument
     */
    public Vector3D getMin(final Vector3D out) {
        return out.set(min);
    }

    /**
     * @return The maximum vector
     *
     * @deprecated Use {@link #getMax(Vector3D)} instead
     */
    @Deprecated
    public Vector3D getMax() {
        return max;
    }

    /**
     * @param out The {@link Vector3D} to receive the maximum values.
     *
     * @return The vector specified with the out argument
     */
    public Vector3D getMax(final Vector3D out) {
        return out.set(max);
    }

    /**
     * Sets the given bounding box.
     *
     * @param bounds The bounds.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble set(BoundingBoxDouble bounds) {
        return this.set(bounds.min, bounds.max);
    }

    public BoundingBox put(BoundingBox bounds) {
        return bounds.set(this.min.toVector3(), this.max.toVector3());
    }

    /**
     * Sets the given minimum and maximum vector.
     *
     * @param minimum The minimum vector
     * @param maximum The maximum vector
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble set(Vector3D minimum, Vector3D maximum) {
        min.set(minimum.x < maximum.x ? minimum.x : maximum.x, minimum.y < maximum.y ? minimum.y : maximum.y, minimum.z < maximum.z ? minimum.z : maximum.z);
        max.set(minimum.x > maximum.x ? minimum.x : maximum.x, minimum.y > maximum.y ? minimum.y : maximum.y, minimum.z > maximum.z ? minimum.z : maximum.z);
        cnt.set(min).add(max).scl(0.5f);
        dim.set(max).sub(min);
        return this;
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     *
     * @param points The points.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble set(Vector3D[] points) {
        this.inf();
        for (Vector3D l_point : points)
            this.ext(l_point);
        return this;
    }

    /**
     * Sets the bounding box minimum and maximum vector from the given points.
     *
     * @param points The points.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble set(List<Vector3D> points) {
        this.inf();
        for (Vector3D l_point : points)
            this.ext(l_point);
        return this;
    }

    /**
     * Sets the minimum and maximum vector to positive and negative infinity.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble inf() {
        min.set(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        max.set(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        cnt.set(0, 0, 0);
        dim.set(0, 0, 0);
        return this;
    }

    /**
     * Extends the bounding box to incorporate the given {@link Vector3D}.
     *
     * @param point The vector
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble ext(Vector3D point) {
        return this.set(min.set(min(min.x, point.x), min(min.y, point.y), min(min.z, point.z)), max.set(Math.max(max.x, point.x), FastMath.max(max.y, point.y), FastMath.max(max.z, point.z)));
    }

    /**
     * Sets the minimum and maximum vector to zeros.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble clr() {
        return this.set(min.set(0, 0, 0), max.set(0, 0, 0));
    }

    /**
     * Returns whether this bounding box is valid. This means that {@link #max}
     * is greater than {@link #min}.
     *
     * @return True in case the bounding box is valid, false otherwise
     */
    public boolean isValid() {
        return min.x < max.x && min.y < max.y && min.z < max.z;
    }

    /**
     * Extends this bounding box by the given bounding box.
     *
     * @param a_bounds The bounding box
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble ext(BoundingBoxDouble a_bounds) {
        return this.set(min.set(min(min.x, a_bounds.min.x), min(min.y, a_bounds.min.y), min(min.z, a_bounds.min.z)), max.set(max(max.x, a_bounds.max.x), max(max.y, a_bounds.max.y), max(max.z, a_bounds.max.z)));
    }

    /**
     * Extends this bounding box by the given transformed bounding box.
     *
     * @param bounds    The bounding box
     * @param transform The transformation matrix to apply to bounds, before using it
     *                  to extend this bounding box.
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble ext(BoundingBoxDouble bounds, Matrix4D transform) {
        ext(tmpVector.set(bounds.min.x, bounds.min.y, bounds.min.z).mul(transform));
        ext(tmpVector.set(bounds.min.x, bounds.min.y, bounds.max.z).mul(transform));
        ext(tmpVector.set(bounds.min.x, bounds.max.y, bounds.min.z).mul(transform));
        ext(tmpVector.set(bounds.min.x, bounds.max.y, bounds.max.z).mul(transform));
        ext(tmpVector.set(bounds.max.x, bounds.min.y, bounds.min.z).mul(transform));
        ext(tmpVector.set(bounds.max.x, bounds.min.y, bounds.max.z).mul(transform));
        ext(tmpVector.set(bounds.max.x, bounds.max.y, bounds.min.z).mul(transform));
        ext(tmpVector.set(bounds.max.x, bounds.max.y, bounds.max.z).mul(transform));
        return this;
    }

    /**
     * Multiplies the bounding box by the given matrix. This is achieved by
     * multiplying the 8 corner points and then calculating the minimum and
     * maximum vectors from the transformed points.
     *
     * @param transform The matrix
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble mul(Matrix4D transform) {
        final double x0 = min.x, y0 = min.y, z0 = min.z, x1 = max.x, y1 = max.y, z1 = max.z;
        inf();
        ext(tmpVector.set(x0, y0, z0).mul(transform));
        ext(tmpVector.set(x0, y0, z1).mul(transform));
        ext(tmpVector.set(x0, y1, z0).mul(transform));
        ext(tmpVector.set(x0, y1, z1).mul(transform));
        ext(tmpVector.set(x1, y0, z0).mul(transform));
        ext(tmpVector.set(x1, y0, z1).mul(transform));
        ext(tmpVector.set(x1, y1, z0).mul(transform));
        ext(tmpVector.set(x1, y1, z1).mul(transform));
        return this;
    }

    /**
     * Returns whether the given bounding box is contained in this bounding box.
     *
     * @param b The bounding box
     *
     * @return Whether the given bounding box is contained
     */
    public boolean contains(BoundingBoxDouble b) {
        return !isValid() || (min.x <= b.min.x && min.y <= b.min.y && min.z <= b.min.z && max.x >= b.max.x && max.y >= b.max.y && max.z >= b.max.z);
    }

    /**
     * Returns whether the given bounding box is intersecting this bounding box
     * (at least one point in).
     *
     * @param b The bounding box
     *
     * @return Whether the given bounding box is intersected
     */
    public boolean intersects(BoundingBoxDouble b) {
        if (!isValid())
            return false;

        // test using SAT (separating axis theorem)

        double lx = FastMath.abs(this.cnt.x - b.cnt.x);
        double sumx = (this.dim.x / 2.0f) + (b.dim.x / 2.0f);

        double ly = FastMath.abs(this.cnt.y - b.cnt.y);
        double sumy = (this.dim.y / 2.0f) + (b.dim.y / 2.0f);

        double lz = FastMath.abs(this.cnt.z - b.cnt.z);
        double sumz = (this.dim.z / 2.0f) + (b.dim.z / 2.0f);

        return (lx <= sumx && ly <= sumy && lz <= sumz);

    }

    /**
     * Returns whether the given vector is contained in this bounding box.
     *
     * @param v The vector
     *
     * @return Whether the vector is contained or not.
     */
    public boolean contains(Vector3D v) {
        return min.x <= v.x && max.x >= v.x && min.y <= v.y && max.y >= v.y && min.z <= v.z && max.z >= v.z;
    }

    /**
     * Returns whether the given position [xyz] is contained in this bounding
     * box.
     *
     * @param x The x component
     * @param y The y component
     * @param z The z component
     *
     * @return Whether it is contained in this box
     */
    public boolean contains(double x, double y, double z) {
        return min.x <= x && max.x >= x && min.y <= y && max.y >= y && min.z <= z && max.z >= z;
    }

    @Override
    public String toString() {
        return "[" + min + "|" + max + "]";
        //return "[" + min.x * Constants.U_TO_KPC + "," + min.y * Constants.U_TO_KPC + "," + min.z * Constants.U_TO_KPC + "|" + max.x * Constants.U_TO_KPC + "," + max.y * Constants.U_TO_KPC + "," + max.z * Constants.U_TO_KPC + "]";
    }

    /**
     * Extends the bounding box by the given vector.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param z The z-coordinate
     *
     * @return This bounding box for chaining.
     */
    public BoundingBoxDouble ext(double x, double y, double z) {
        return this.set(min.set(min(min.x, x), min(min.y, y), min(min.z, z)), max.set(max(max.x, x), max(max.y, y), max(max.z, z)));
    }
}
