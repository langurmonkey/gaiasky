/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.math;

import com.badlogic.gdx.math.Vector3;
import net.jafama.FastMath;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Vector of arbitrary precision floating point numbers using {@link BigDecimal}s.
 *
 * @author Toni Sagrista
 */
public class Vector3b implements Serializable {
    private static final long serialVersionUID = 3840054589595372522L;
    private static final MathContext mc = MathContext.DECIMAL128;

    /** the x-component of this vector **/
    public BigDecimal x;
    /** the y-component of this vector **/
    public BigDecimal y;
    /** the z-component of this vector **/
    public BigDecimal z;

    public final static Vector3b X = new Vector3b(1, 0, 0);
    public final static Vector3b Y = new Vector3b(0, 1, 0);
    public final static Vector3b Z = new Vector3b(0, 0, 1);
    public final static Vector3b Zero = new Vector3b(0, 0, 0);

    private final static Matrix4d tmpMat = new Matrix4d();

    public static Vector3b getUnitX() {
        return X.cpy();
    }

    public static Vector3b getUnitY() {
        return Y.cpy();
    }

    public static Vector3b getUnitZ() {
        return Z.cpy();
    }

    /** Constructs a vector at (0,0,0) */
    public Vector3b() {
        this.x = BigDecimal.ZERO;
        this.y = BigDecimal.ZERO;
        this.z = BigDecimal.ZERO;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(BigDecimal x, BigDecimal y, BigDecimal z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a vector with the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     */
    public Vector3b(double x, double y, double z) {
        this.x = BigDecimal.valueOf(x);
        this.y = BigDecimal.valueOf(y);
        this.z = BigDecimal.valueOf(z);
    }

    /**
     * Creates a vector from the given vector
     *
     * @param vector The vector
     */
    public Vector3b(final Vector3b vector) {
        this.set(vector);
    }

    /**
     * Creates a vector from the given array. The array must have at least 3
     * elements.
     *
     * @param values The array
     */
    public Vector3b(final double[] values) {
        this.set(values[0], values[1], values[2]);
    }

    public double x() {
        return x.doubleValue();
    }

    public BigDecimal xb() {
        return x;
    }

    public double y() {
        return y.doubleValue();
    }

    public BigDecimal yb() {
        return y;
    }

    public double z() {
        return z.doubleValue();
    }

    public BigDecimal zb() {
        return z;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(float x, float y, float z) {
        this.x = BigDecimal.valueOf(x);
        this.y = BigDecimal.valueOf(y);
        this.z = BigDecimal.valueOf(z);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(double x, double y, double z) {
        this.x = BigDecimal.valueOf(x);
        this.y = BigDecimal.valueOf(y);
        this.z = BigDecimal.valueOf(z);
        return this;
    }

    /**
     * Sets the vector to the given components
     *
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @return this vector for chaining
     */
    public Vector3b set(BigDecimal x, BigDecimal y, BigDecimal z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3b set(final Vector3b vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3b set(final Vector3 vec) {
        if (vec != null)
            return this.set(vec.x, vec.y, vec.z);
        return this;
    }

    public Vector3 put(final Vector3 vec) {
        return vec.set(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3 tov3() {
        return new Vector3(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3d put(final Vector3d vec) {
        return vec.set(this.x.doubleValue(), this.y.doubleValue(), this.z.doubleValue());
    }

    public Vector3d tov3d() {
        return new Vector3d(this.x.floatValue(), this.y.floatValue(), this.z.floatValue());
    }

    public Vector3b put(final Vector3b vec) {
        return vec.set(this.x, this.y, this.z);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     * @return this vector for chaining
     */
    public Vector3b set(final double[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the array. The array must have at least 3 elements
     *
     * @param vals The array
     * @return this vector for chaining
     */
    public Vector3b set(final float[] vals) {
        return this.set(vals[0], vals[1], vals[2]);
    }

    /**
     * Sets the components from the given spherical coordinate
     *
     * @param azimuthalAngle The angle between x-axis in radians [0, 2pi]
     * @param polarAngle     The angle between z-axis in radians [0, pi]
     * @return This vector for chaining
     */
    public Vector3b setFromSpherical(double azimuthalAngle, double polarAngle) {
        double cosPolar = MathUtilsd.cos(polarAngle);
        double sinPolar = MathUtilsd.sin(polarAngle);

        double cosAzim = MathUtilsd.cos(azimuthalAngle);
        double sinAzim = MathUtilsd.sin(azimuthalAngle);

        return this.set(cosAzim * sinPolar, sinAzim * sinPolar, cosPolar);
    }

    public Vector3b setToRandomDirection() {
        double u = MathUtilsd.random();
        double v = MathUtilsd.random();

        double theta = MathUtilsd.PI2 * u; // azimuthal angle
        double phi = Math.acos(2f * v - 1f); // polar angle

        return this.setFromSpherical(theta, phi);
    }

    public Vector3b cpy() {
        return new Vector3b(this);
    }

    public Vector3b add(final Vector3b vec) {
        this.x = this.x.add(vec.x, mc);
        this.y = this.y.add(vec.y, mc);
        this.z = this.z.add(vec.z, mc);
        return this;
    }

    public Vector3b add(final Vector3 vec) {
        this.x = this.x.add(BigDecimal.valueOf(vec.x), mc);
        this.y = this.y.add(BigDecimal.valueOf(vec.y), mc);
        this.z = this.z.add(BigDecimal.valueOf(vec.z), mc);
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining.
     */
    public Vector3b add(double x, double y, double z) {
        this.x = this.x.add(BigDecimal.valueOf(x), mc);
        this.y = this.y.add(BigDecimal.valueOf(y), mc);
        this.z = this.z.add(BigDecimal.valueOf(z), mc);
        return this;
    }

    /**
     * Adds the given vector to this component
     *
     * @param vals The 3-value double vector.
     * @return This vector for chaining.
     */
    public Vector3b add(double... vals) {
        assert vals.length == 3 : "vals must contain 3 values";
        this.x = this.x.add(BigDecimal.valueOf(vals[0]), mc);
        this.y = this.y.add(BigDecimal.valueOf(vals[1]), mc);
        this.z = this.z.add(BigDecimal.valueOf(vals[2]), mc);
        return this;
    }

    /**
     * Adds the given value to all three components of the vector.
     *
     * @param value The value
     * @return This vector for chaining
     */
    public Vector3b add(double value) {
        var val = BigDecimal.valueOf(value);
        x = x.add(val, mc);
        y = y.add(val, mc);
        z = z.add(val, mc);
        return this;
    }

    public Vector3b sub(final Vector3b vec) {
        x = x.subtract(vec.x, mc);
        y = y.subtract(vec.y, mc);
        z = z.subtract(vec.z, mc);
        return this;
    }

    public Vector3b sub(final Vector3 a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    public Vector3b sub(final Vector3d a_vec) {
        return this.sub(a_vec.x, a_vec.y, a_vec.z);
    }

    /**
     * Subtracts the other vector from this vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vector3b sub(double x, double y, double z) {
        this.x = this.x.subtract(BigDecimal.valueOf(x), mc);
        this.y = this.y.subtract(BigDecimal.valueOf(y), mc);
        this.z = this.z.subtract(BigDecimal.valueOf(z), mc);
        return this;
    }

    /**
     * Subtracts the given value from all components of this vector
     *
     * @param value The value
     * @return This vector for chaining
     */
    public Vector3b sub(double value) {
        var val = BigDecimal.valueOf(value);
        x = x.subtract(val, mc);
        y = y.subtract(val, mc);
        z = z.subtract(val, mc);
        return this;
    }

    public Vector3b scl(BigDecimal scl) {
        x = x.multiply(scl, mc);
        y = y.multiply(scl, mc);
        z = z.multiply(scl, mc);
        return this;
    }

    public Vector3b scl(double scalar) {
        var scl = BigDecimal.valueOf(scalar);
        x = x.multiply(scl, mc);
        y = y.multiply(scl, mc);
        z = z.multiply(scl, mc);
        return this;
    }

    public Vector3b scl(final Vector3b vec) {
        x = x.multiply(vec.x, mc);
        y = y.multiply(vec.y, mc);
        z = z.multiply(vec.z, mc);
        return this;
    }

    /**
     * Scales this vector by the given values
     *
     * @param x X value
     * @param y Y value
     * @param z Z value
     * @return This vector for chaining
     */
    public Vector3b scl(double x, double y, double z) {
        this.x = this.x.multiply(BigDecimal.valueOf(x), mc);
        this.y = this.y.multiply(BigDecimal.valueOf(y), mc);
        this.z = this.z.multiply(BigDecimal.valueOf(z), mc);
        return this;
    }

    public Vector3b mulAdd(Vector3b vec, double scalar) {
        BigDecimal scl = BigDecimal.valueOf(scalar);
        x = x.add(vec.x.multiply(scl, mc), mc);
        y = y.add(vec.y.multiply(scl, mc), mc);
        z = z.add(vec.z.multiply(scl, mc), mc);
        return this;
    }

    public Vector3b mulAdd(Vector3b vec, Vector3b mulVec) {
        x = x.add(vec.x.multiply(mulVec.x, mc), mc);
        y = y.add(vec.y.multiply(mulVec.y, mc), mc);
        z = z.add(vec.z.multiply(mulVec.z, mc), mc);
        return this;
    }

    public Vector3b mul(Vector3b vec) {
        x = x.multiply(vec.x, mc);
        y = y.multiply(vec.y, mc);
        z = z.multiply(vec.z, mc);
        return this;
    }

    public Vector3b div(Vector3b vec) {
        x = x.divide(vec.x, mc);
        y = y.divide(vec.y, mc);
        z = z.divide(vec.z, mc);
        return this;
    }

    /** @return The euclidian length */
    public static double len(final double x, final double y, final double z) {
        return FastMath.sqrt(x * x + y * y + z * z);
    }

    public double lend() {
        return this.len().doubleValue();
    }

    public BigDecimal len() {
        BigDecimal sumSq = x.multiply(x, mc).add(y.multiply(y, mc), mc).add(z.multiply(z, mc), mc);
        return sumSq.sqrt(mc);
    }

    /** @return The squared euclidian length */
    public static double len2(final double x, final double y, final double z) {
        return x * x + y * y + z * z;
    }

    public double len2d() {
        return this.len2().doubleValue();
    }

    public BigDecimal len2() {
        return x.multiply(x, mc).add(y.multiply(y, mc), mc).add(z.multiply(z, mc), mc);
    }

    /**
     * @param vec The other vector
     * @return Wether this and the other vector are equal
     */
    public boolean idt(final Vector3b vec) {
        return x == vec.x && y == vec.y && z == vec.z;
    }

    public double dstd(final Vector3b vec) {
        return dst(vec).doubleValue();
    }

    public BigDecimal dst(final Vector3b vec) {
        BigDecimal a = vec.x.subtract(this.x, mc);
        BigDecimal b = vec.y.subtract(this.y, mc);
        BigDecimal c = vec.z.subtract(this.z, mc);
        return a.pow(2, mc).add(b.pow(2, mc), mc).add(c.pow(2, mc), mc).sqrt(mc);
    }

    public double dstd(double x, double y, double z) {
        return dst(x, y, z).doubleValue();
    }

    /** @return the distance between this point and the given point */
    public BigDecimal dst(double x, double y, double z) {
        BigDecimal a = BigDecimal.valueOf(x).subtract(this.x, mc);
        BigDecimal b = BigDecimal.valueOf(y).subtract(this.y, mc);
        BigDecimal c = BigDecimal.valueOf(z).subtract(this.z, mc);
        return a.pow(2, mc).add(b.pow(2, mc), mc).add(c.pow(2, mc), mc).sqrt(mc);
    }

    public double dst2d(Vector3b vec) {
        return this.dst2(vec).doubleValue();
    }

    public BigDecimal dst2(Vector3b vec) {
        BigDecimal a = vec.x.subtract(this.x, mc);
        BigDecimal b = vec.y.subtract(this.y, mc);
        BigDecimal c = vec.z.subtract(this.z, mc);
        return a.pow(2, mc).add(b.pow(2, mc), mc).add(c.pow(2, mc), mc);
    }

    public double dst2d(double x, double y, double z) {
        return dst2(x, y, z).doubleValue();
    }

    /**
     * Returns the squared distance between this point and the given point
     *
     * @param x The x-component of the other point
     * @param y The y-component of the other point
     * @param z The z-component of the other point
     * @return The squared distance
     */
    public BigDecimal dst2(double x, double y, double z) {
        BigDecimal a = BigDecimal.valueOf(x).subtract(this.x, mc);
        BigDecimal b = BigDecimal.valueOf(y).subtract(this.y, mc);
        BigDecimal c = BigDecimal.valueOf(z).subtract(this.z, mc);
        return a.pow(2, mc).add(b.pow(2, mc), mc).add(c.pow(2, mc), mc);
    }

    public Vector3b nor() {
        final BigDecimal len2 = this.len2();
        final double len2d = len2.doubleValue();
        if (len2d == 0f || len2d == 1f)
            return this;
        return this.scl(BigDecimal.ONE.divide(len2.sqrt(mc), mc));
    }

    public double dotd(final Vector3b vec) {
        return this.dot(vec).doubleValue();
    }

    public BigDecimal dot(final Vector3b vec) {
        return this.x.multiply(vec.x, mc).add(this.y.multiply(vec.y, mc), mc).add(this.z.multiply(vec.z, mc), mc);
    }

    public double dotd(double x, double y, double z) {
        return this.dot(BigDecimal.valueOf(x), BigDecimal.valueOf(y), BigDecimal.valueOf(z)).doubleValue();
    }

    /**
     * Returns the dot product between this and the given vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return The dot product
     */
    public BigDecimal dot(BigDecimal x, BigDecimal y, BigDecimal z) {
        return this.x.multiply(x, mc).add(this.y.multiply(y, mc), mc).add(this.z.multiply(z, mc), mc);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param vec The other vector
     * @return This vector for chaining
     */
    public Vector3b crs(final Vector3b vec) {
        return this.set(this.y.multiply(vec.z, mc).subtract(this.z.multiply(vec.y, mc), mc), this.z.multiply(vec.x, mc).subtract(this.x.multiply(vec.z, mc), mc), this.x.multiply(vec.y, mc).subtract(this.y.multiply(vec.x, mc), mc));
    }

    /**
     * Calculates the outer product of two given vectors <code>v</code> and
     * <code>w</code> and returns the result as a new <code>GVector3d</code>.
     *
     * @param v left operand
     * @param w right operand
     * @return outer product of <code>v</code> and <code>w</code>
     */
    static public Vector3b crs(final Vector3b v, final Vector3b w) {
        final Vector3b res = new Vector3b(v);

        return res.crs(w);
    }

    /**
     * Sets this vector to the cross product between it and the other vector.
     *
     * @param x The x-component of the other vector
     * @param y The y-component of the other vector
     * @param z The z-component of the other vector
     * @return This vector for chaining
     */
    public Vector3b crs(double x, double y, double z) {
        BigDecimal vx = BigDecimal.valueOf(x);
        BigDecimal vy = BigDecimal.valueOf(y);
        BigDecimal vz = BigDecimal.valueOf(z);
        return this.set(this.y.multiply(vz, mc).subtract(this.z.multiply(vy, mc), mc), this.z.multiply(vx, mc).subtract(this.x.multiply(vz, mc), mc), this.x.multiply(vy, mc).subtract(this.y.multiply(vx, mc), mc));
    }

    public boolean isUnit() {
        return isUnit(0.000000001);
    }

    public boolean isUnit(final double margin) {
        return Math.abs(len2d() - 1f) < margin;
    }

    public boolean isZero() {
        return x.doubleValue() == 0 && y.doubleValue() == 0 && z.doubleValue() == 0;
    }

    public boolean isZero(final double margin) {
        return len2d() < margin;
    }

    public String toString() {
        return x + "," + y + "," + z;
    }

    public Vector3b setLength(double len) {
        return setLength2(len * len);
    }

    public Vector3b setLength2(double len2) {
        double oldLen2 = len2d();
        return (oldLen2 == 0 || oldLen2 == len2) ? this : scl(Math.sqrt(len2 / oldLen2));
    }

    public Vector3b clamp(double min, double max) {
        final double l2 = len2d();
        if (l2 == 0f)
            return this;
        if (l2 > max * max)
            return nor().scl(max);
        if (l2 < min * min)
            return nor().scl(min);
        return this;
    }

    public BigDecimal[] values() {
        return new BigDecimal[] { x, y, z };
    }

    public double[] valuesd() {
        return new double[] { x.doubleValue(), y.doubleValue(), z.doubleValue() };
    }

    public float[] valuesf() {
        return new float[] { x.floatValue(), y.floatValue(), z.floatValue() };
    }

    public float[] valuesf(float[] vec) {
        vec[0] = x.floatValue();
        vec[1] = y.floatValue();
        vec[2] = z.floatValue();
        return vec;
    }

    /**
     * Scales a given vector with a scalar and add the result to this one, i.e.
     * <code>this = this + s*v</code>.
     *
     * @param s scalar scaling factor
     * @param v vector to scale
     * @return vector modified in place
     */
    public Vector3b scaleAdd(final double s, final Vector3b v) {
        return this.add(v.scl(s));
    }

    /**
     * Returns a vector3 representation of this vector by casting the doubles to
     * floats. This creates a new object
     *
     * @return The vector3 representation of this vector3d
     */
    public Vector3 toVector3() {
        return tov3();
    }

    /**
     * Returns a vector3d representation of this vector by casting the BigDecimals to
     * doubles. This creates a new object
     *
     * @return The vector3d representation of this vector3b
     */
    public Vector3d toVector3d() {
        return tov3d();
    }

    /**
     * Returns set v to this vector by casting doubles to floats.
     *
     * @return The float vector v.
     */
    public Vector3 setVector3(Vector3 v) {
        return v.set(x.floatValue(), y.floatValue(), z.floatValue());
    }

    /**
     * Returns set v to this vector by casting BigDecimals to doubles.
     *
     * @return The double vector v.
     */
    public Vector3d setVector3d(Vector3d v) {
        return v.set(x.doubleValue(), y.doubleValue(), z.doubleValue());
    }

    /** Gets the angle in degrees between the two vectors **/
    public double angle(Vector3b v) {
        return MathUtilsd.radiansToDegrees * FastMath.acos(this.dotd(v) / (this.lend() * v.lend()));
    }

    /** Gets the angle in degrees between the two vectors **/
    public double anglePrecise(Vector3b v) {
        return MathUtilsd.radiansToDegrees * Math.acos(this.dotd(v) / (this.lend() * v.lend()));
    }

    @Override
    public int hashCode() {
        final long prime = 31;
        long result = 1;
        result = prime * result + x.hashCode();
        result = prime * result + y.hashCode();
        result = prime * result + z.hashCode();
        return (int) result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vector3b other = (Vector3b) obj;
        if (x.hashCode() != other.x.hashCode())
            return false;
        if (y.hashCode() != other.y.hashCode())
            return false;
        if (z.hashCode() != other.z.hashCode())
            return false;
        return true;
    }

    public Vector3b setZero() {
        this.x = BigDecimal.ZERO;
        this.y = BigDecimal.ZERO;
        this.z = BigDecimal.ZERO;
        return this;
    }

}
