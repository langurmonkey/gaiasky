package gaia.cu9.ari.gaiaorbit.util.math;

import java.io.Serializable;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

import net.jafama.FastMath;

/**
 * Copy of libgdx's Vector3d class using doubles for some precision
 * calculations.
 * 
 * @author Toni Sagrista
 *
 */
public class Vector3d implements Serializable {
	private static final long serialVersionUID = 3840054589595372522L;

	/** the x-component of this vector **/
	public double x;
	/** the y-component of this vector **/
	public double y;
	/** the z-component of this vector **/
	public double z;

	public final static Vector3d X = new Vector3d(1, 0, 0);
	public final static Vector3d Y = new Vector3d(0, 1, 0);
	public final static Vector3d Z = new Vector3d(0, 0, 1);
	public final static Vector3d Zero = new Vector3d(0, 0, 0);

	private final static Matrix4d tmpMat = new Matrix4d();

	public static Vector3d getUnitX() {
		return X.cpy();
	}

	public static Vector3d getUnitY() {
		return Y.cpy();
	}

	public static Vector3d getUnitZ() {
		return Z.cpy();
	}

	/** Constructs a vector at (0,0,0) */
	public Vector3d() {
	}

	/**
	 * Creates a vector with the given components
	 * 
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 */
	public Vector3d(double x, double y, double z) {
		this.set(x, y, z);
	}

	/**
	 * Creates a vector from the given vector
	 * 
	 * @param vector The vector
	 */
	public Vector3d(final Vector3d vector) {
		this.set(vector);
	}

	/**
	 * Creates a vector from the given array. The array must have at least 3
	 * elements.
	 *
	 * @param values The array
	 */
	public Vector3d(final double[] values) {
		this.set(values[0], values[1], values[2]);
	}

	/**
	 * Sets the vector to the given components
	 *
	 * @param x The x-component
	 * @param y The y-component
	 * @param z The z-component
	 * @return this vector for chaining
	 */
	public Vector3d set(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public Vector3d set(final Vector3d vector) {
		if (vector != null)
			return this.set(vector.x, vector.y, vector.z);
		return this;
	}

	public Vector3d set(final Vector3 vector) {
		if (vector != null)
			return this.set(vector.x, vector.y, vector.z);
		return this;
	}

	public Vector3 put(final Vector3 vector) {
		return vector.set((float) this.x, (float) this.y, (float) this.z);
	}

	public Vector3d put(final Vector3d vector) {
		return vector.set(this.x, this.y, this.z);
	}

	public Vector3d setZero() {
		return this.set(0, 0, 0);
	}

	/**
	 * Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining
	 */
	public Vector3d set(final double[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	/**
	 * Sets the components from the array. The array must have at least 3 elements
	 *
	 * @param values The array
	 * @return this vector for chaining
	 */
	public Vector3d set(final float[] values) {
		return this.set(values[0], values[1], values[2]);
	}

	public Vector3d cpy() {
		return new Vector3d(this);
	}

	public Vector3d add(final Vector3d vector) {
		this.x += vector.x;
		this.y += vector.y;
		this.z += vector.z;
		return this;
	}

	public Vector3d add(final Vector3 vector) {
		this.x += vector.x;
		this.y += vector.y;
		this.z += vector.z;
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
	public Vector3d add(double x, double y, double z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}

	/**
	 * Adds the given vector to this component
	 * 
	 * @param vals The 3-value double vector.
	 * @return This vector for chaining.
	 */
	public Vector3d add(double... vals) {
		assert vals.length == 3 : "vals must contain 3 values";
		this.x += vals[0];
		this.y += vals[1];
		this.z += vals[2];
		return this;
	}

	/**
	 * Adds the given value to all three components of the vector.
	 *
	 * @param values The value
	 * @return This vector for chaining
	 */
	public Vector3d add(double values) {
		return this.set(this.x + values, this.y + values, this.z + values);
	}

	public Vector3d sub(final Vector3d a_vec) {
		return this.sub(a_vec.x, a_vec.y, a_vec.z);
	}

	public Vector3d sub(final Vector3 a_vec) {
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
	public Vector3d sub(double x, double y, double z) {
		return this.set(this.x - x, this.y - y, this.z - z);
	}

	/**
	 * Subtracts the given value from all components of this vector
	 *
	 * @param value The value
	 * @return This vector for chaining
	 */
	public Vector3d sub(double value) {
		return this.set(this.x - value, this.y - value, this.z - value);
	}

	public Vector3d scl(double scalar) {
		return this.set(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	public Vector3d scl(final Vector3d other) {
		return this.set(x * other.x, y * other.y, z * other.z);
	}

	/**
	 * Scales this vector by the given values
	 * 
	 * @param vx X value
	 * @param vy Y value
	 * @param vz Z value
	 * @return This vector for chaining
	 */
	public Vector3d scl(double vx, double vy, double vz) {
		return this.set(this.x * vx, this.y * vy, this.z * vz);
	}

	public Vector3d mulAdd(Vector3d vec, double scalar) {
		this.x += vec.x * scalar;
		this.y += vec.y * scalar;
		this.z += vec.z * scalar;
		return this;
	}

	public Vector3d mulAdd(Vector3d vec, Vector3d mulVec) {
		this.x += vec.x * mulVec.x;
		this.y += vec.y * mulVec.y;
		this.z += vec.z * mulVec.z;
		return this;
	}

	public Vector3d mul(Vector3d vec) {
		this.x *= vec.x;
		this.y *= vec.y;
		this.z *= vec.z;
		return this;
	}

	public Vector3d div(Vector3d vec) {
		this.x /= vec.x;
		this.y /= vec.y;
		this.z /= vec.z;
		return this;
	}

	/** @return The euclidian length */
	public static double len(final double x, final double y, final double z) {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double len() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	/** @return The squared euclidian length */
	public static double len2(final double x, final double y, final double z) {
		return x * x + y * y + z * z;
	}

	public double len2() {
		return x * x + y * y + z * z;
	}

	/**
	 * @param vector The other vector
	 * @return Wether this and the other vector are equal
	 */
	public boolean idt(final Vector3d vector) {
		return x == vector.x && y == vector.y && z == vector.z;
	}

	/** @return The euclidian distance between the two specified vectors */
	public static double dst(final double x1, final double y1, final double z1, final double x2, final double y2,
			final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return Math.sqrt(a * a + b * b + c * c);
	}

	public double dst(final Vector3d vector) {
		final double a = vector.x - x;
		final double b = vector.y - y;
		final double c = vector.z - z;
		return Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the distance between this point and the given point */
	public double dst(double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return Math.sqrt(a * a + b * b + c * c);
	}

	/** @return the squared distance between the given points */
	public static double dst2(final double x1, final double y1, final double z1, final double x2, final double y2,
			final double z2) {
		final double a = x2 - x1;
		final double b = y2 - y1;
		final double c = z2 - z1;
		return a * a + b * b + c * c;
	}

	public double dst2(Vector3d point) {
		final double a = point.x - x;
		final double b = point.y - y;
		final double c = point.z - z;
		return a * a + b * b + c * c;
	}

	/**
	 * Returns the squared distance between this point and the given point
	 * 
	 * @param x The x-component of the other point
	 * @param y The y-component of the other point
	 * @param z The z-component of the other point
	 * @return The squared distance
	 */
	public double dst2(double x, double y, double z) {
		final double a = x - this.x;
		final double b = y - this.y;
		final double c = z - this.z;
		return a * a + b * b + c * c;
	}

	public Vector3d nor() {
		final double len2 = this.len2();
		if (len2 == 0f || len2 == 1f)
			return this;
		return this.scl(1f / Math.sqrt(len2));
	}

	/** @return The dot product between the two vectors */
	public static double dot(double x1, double y1, double z1, double x2, double y2, double z2) {
		return x1 * x2 + y1 * y2 + z1 * z2;
	}

	public double dot(final Vector3d vector) {
		return x * vector.x + y * vector.y + z * vector.z;
	}

	/**
	 * Returns the dot product between this and the given vector.
	 * 
	 * @param x The x-component of the other vector
	 * @param y The y-component of the other vector
	 * @param z The z-component of the other vector
	 * @return The dot product
	 */
	public double dot(double x, double y, double z) {
		return this.x * x + this.y * y + this.z * z;
	}

	/**
	 * Sets this vector to the cross product between it and the other vector.
	 * 
	 * @param vector The other vector
	 * @return This vector for chaining
	 */
	public Vector3d crs(final Vector3d vector) {
		return this.set(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x);
	}

	/**
	 * Calculates the outer product of two given vectors <code>v</code> and
	 * <code>w</code> and returns the result as a new <code>GVector3d</code>.
	 *
	 * @param v left operand
	 * @param w right operand
	 * @return outer product of <code>v</code> and <code>w</code>
	 */
	static public Vector3d crs(final Vector3d v, final Vector3d w) {
		final Vector3d res = new Vector3d(v);

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
	public Vector3d crs(double x, double y, double z) {
		return this.set(this.y * z - this.z * y, this.z * x - this.x * z, this.x * y - this.y * x);
	}

	/**
	 * Left-multiplies the vector by the given 4x3 column major matrix. The matrix
	 * should be composed by a 3x3 matrix representing rotation and scale plus a 1x3
	 * matrix representing the translation.
	 * 
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	public Vector3d mul4x3(double[] matrix) {
		return set(x * matrix[0] + y * matrix[3] + z * matrix[6] + matrix[9],
				x * matrix[1] + y * matrix[4] + z * matrix[7] + matrix[10],
				x * matrix[2] + y * matrix[5] + z * matrix[8] + matrix[11]);
	}

	/**
	 * Left-multiplies the vector by the given matrix, assuming the fourth (w)
	 * component of the vector is 1.
	 * 
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	public Vector3d mul(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(
				x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02] + l_mat[Matrix4d.M03],
				x * l_mat[Matrix4d.M10] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12] + l_mat[Matrix4d.M13],
				x * l_mat[Matrix4d.M20] + y * l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M23]);
	}

	public Vector3d mulLeft(final Matrix3 matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M01] + z * l_mat[Matrix3.M02],
				x * l_mat[Matrix3.M10] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M12],
				x * l_mat[Matrix3.M20] + y * l_mat[Matrix3.M21] + z * l_mat[Matrix3.M22]);
	}

	public Vector3d mulRight(final Matrix3 matrix) {
		final float l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix3.M00] + y * l_mat[Matrix3.M10] + z * l_mat[Matrix3.M20],
				x * l_mat[Matrix3.M01] + y * l_mat[Matrix3.M11] + z * l_mat[Matrix3.M21],
				x * l_mat[Matrix3.M02] + y * l_mat[Matrix3.M12] + z * l_mat[Matrix3.M22]);
	}

	/**
	 * Multiplies the vector by the transpose of the given matrix, assuming the
	 * fourth (w) component of the vector is 1.
	 * 
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	public Vector3d traMul(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(
				x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20] + l_mat[Matrix4d.M30],
				x * l_mat[Matrix4d.M01] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21] + l_mat[Matrix4d.M31],
				x * l_mat[Matrix4d.M02] + y * l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M32]);
	}

	/**
	 * Multiplies the vector by the given {@link Quaternion}.
	 * 
	 * @return This vector for chaining
	 */
	public Vector3d mul(final Quaterniond quat) {
		return quat.transform(this);
	}

	/**
	 * Multiplies this vector by the given matrix dividing by w, assuming the fourth
	 * (w) component of the vector is 1. This is mostly used to project/unproject
	 * vectors via a perspective projection matrix.
	 *
	 * @param matrix The matrix.
	 * @return This vector for chaining
	 */
	public Vector3d prj(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		final double l_w = 1f
				/ (x * l_mat[Matrix4d.M30] + y * l_mat[Matrix4d.M31] + z * l_mat[Matrix4d.M32] + l_mat[Matrix4d.M33]);
		return this.set(
				(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02] + l_mat[Matrix4d.M03])
						* l_w,
				(x * l_mat[Matrix4d.M10] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12] + l_mat[Matrix4d.M13])
						* l_w,
				(x * l_mat[Matrix4d.M20] + y * l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22] + l_mat[Matrix4d.M23])
						* l_w);
	}

	/**
	 * Multiplies this vector by the first three columns of the matrix, essentially
	 * only applying rotation and scaling.
	 *
	 * @param matrix The matrix
	 * @return This vector for chaining
	 */
	public Vector3d rot(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M01] + z * l_mat[Matrix4d.M02],
				x * l_mat[Matrix4d.M10] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M12],
				x * l_mat[Matrix4d.M20] + y * l_mat[Matrix4d.M21] + z * l_mat[Matrix4d.M22]);
	}

	/**
	 * Multiplies this vector by the transpose of the first three columns of the
	 * matrix. Note: only works for translation and rotation, does not work for
	 * scaling. For those, use {@link #rot(Matrix4d)} with {@link Matrix4d#inv()}.
	 * 
	 * @param matrix The transformation matrix
	 * @return The vector for chaining
	 */
	public Vector3d unrotate(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20],
				x * l_mat[Matrix4d.M01] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21],
				x * l_mat[Matrix4d.M02] + y * l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22]);
	}

	/**
	 * Translates this vector in the direction opposite to the translation of the
	 * matrix and the multiplies this vector by the transpose of the first three
	 * columns of the matrix. Note: only works for translation and rotation, does
	 * not work for scaling. For those, use {@link #mul(Matrix4d)} with
	 * {@link Matrix4d#inv()}.
	 * 
	 * @param matrix The transformation matrix
	 * @return The vector for chaining
	 */
	public Vector3d untransform(final Matrix4d matrix) {
		final double l_mat[] = matrix.val;
		x -= l_mat[Matrix4d.M03];
		y -= l_mat[Matrix4d.M03];
		z -= l_mat[Matrix4d.M03];
		return this.set(x * l_mat[Matrix4d.M00] + y * l_mat[Matrix4d.M10] + z * l_mat[Matrix4d.M20],
				x * l_mat[Matrix4d.M01] + y * l_mat[Matrix4d.M11] + z * l_mat[Matrix4d.M21],
				x * l_mat[Matrix4d.M02] + y * l_mat[Matrix4d.M12] + z * l_mat[Matrix4d.M22]);
	}

	/**
	 * Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param degrees the angle in degrees
	 * @param axisX   the x-component of the axis
	 * @param axisY   the y-component of the axis
	 * @param axisZ   the z-component of the axis
	 * @return This vector for chaining
	 */
	public Vector3d rotate(double degrees, double axisX, double axisY, double axisZ) {
		return this.mul(tmpMat.setToRotation(axisX, axisY, axisZ, degrees));
	}

	/**
	 * Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param radians the angle in radians
	 * @param axisX   the x-component of the axis
	 * @param axisY   the y-component of the axis
	 * @param axisZ   the z-component of the axis
	 * @return This vector for chaining
	 */
	public Vector3d rotateRad(double radians, double axisX, double axisY, double axisZ) {
		return this.mul(tmpMat.setToRotationRad(axisX, axisY, axisZ, radians));
	}

	/**
	 * Rotates this vector by the given angle in degrees around the given axis.
	 *
	 * @param axis    the axis
	 * @param degrees the angle in degrees
	 * @return This vector for chaining
	 */
	public Vector3d rotate(final Vector3d axis, double degrees) {
		tmpMat.setToRotation(axis, degrees);
		return this.mul(tmpMat);
	}

	/**
	 * Rotates this vector by the given angle in radians around the given axis.
	 *
	 * @param axis    the axis
	 * @param radians the angle in radians
	 * @return This vector for chaining
	 */
	public Vector3d rotateRad(final Vector3d axis, double radians) {
		tmpMat.setToRotationRad(axis, radians);
		return this.mul(tmpMat);
	}

	/**
	 * Sets the matrix aux to a translation matrix using this vector
	 * 
	 * @param aux
	 * @return The matrix aux
	 */
	public Matrix4 getMatrix(Matrix4 aux) {
		return aux.idt().translate((float) x, (float) y, (float) z);
	}

	/**
	 * Sets the given matrix to a translation matrix using this vector
	 * 
	 * @param aux The out matrix
	 * @return The matrix with the transform
	 */
	public Matrix4d getMatrix(Matrix4d aux) {
		return aux.idt().translate(this);
	}

	public boolean isUnit() {
		return isUnit(0.000000001);
	}

	public boolean isUnit(final double margin) {
		return Math.abs(len2() - 1f) < margin;
	}

	public boolean isZero() {
		return x == 0 && y == 0 && z == 0;
	}

	public boolean isZero(final double margin) {
		return len2() < margin;
	}

	public boolean hasSameDirection(Vector3d vector) {
		return dot(vector) > 0;
	}

	public boolean hasOppositeDirection(Vector3d vector) {
		return dot(vector) < 0;
	}

	public Vector3d lerp(final Vector3d target, double alpha) {
		scl(1.0f - alpha);
		add(target.x * alpha, target.y * alpha, target.z * alpha);
		return this;
	}

	/**
	 * Spherically interpolates between this vector and the target vector by alpha
	 * which is in the range [0,1]. The result is stored in this vector.
	 *
	 * @param target The target vector
	 * @param alpha  The interpolation coefficient
	 * @return This vector for chaining.
	 */
	public Vector3d slerp(final Vector3d target, double alpha) {
		final double dot = dot(target);
		// If the inputs are too close for comfort, simply linearly interpolate.
		if (dot > 0.9995 || dot < -0.9995)
			return lerp(target, alpha);

		// theta0 = angle between input vectors
		final double theta0 = Math.acos(dot);
		// theta = angle between this vector and result
		final double theta = theta0 * alpha;

		final double st = Math.sin(theta);
		final double tx = target.x - x * dot;
		final double ty = target.y - y * dot;
		final double tz = target.z - z * dot;
		final double l2 = tx * tx + ty * ty + tz * tz;
		final double dl = st * ((l2 < 0.0001f) ? 1f : 1f / Math.sqrt(l2));

		return scl(Math.cos(theta)).add(tx * dl, ty * dl, tz * dl).nor();
	}

	public String toString() {
		return x + "," + y + "," + z;
	}

	public Vector3d limit(double limit) {
		if (len2() > limit * limit)
			nor().scl(limit);
		return this;
	}

	public Vector3d limit2(double limit2) {
		double len2 = len2();
		if (len2 > limit2) {
			scl(Math.sqrt(limit2 / len2));
		}
		return this;
	}

	public Vector3d setLength(double len) {
		return setLength2(len * len);
	}

	public Vector3d setLength2(double len2) {
		double oldLen2 = len2();
		return (oldLen2 == 0 || oldLen2 == len2) ? this : scl(Math.sqrt(len2 / oldLen2));
	}

	public Vector3d clamp(double min, double max) {
		final double l2 = len2();
		if (l2 == 0f)
			return this;
		if (l2 > max * max)
			return nor().scl(max);
		if (l2 < min * min)
			return nor().scl(min);
		return this;
	}

	public double[] values() {
		return new double[] { x, y, z };
	}

	public float[] valuesf() {
		return new float[] { (float) x, (float) y, (float) z };
	}

	public float[] valuesf(float[] vec) {
		vec[0] = (float) x;
		vec[1] = (float) y;
		vec[2] = (float) z;
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
	public Vector3d scaleAdd(final double s, final Vector3d v) {
		return this.add(v.scl(s));
	}

	/**
	 * Returns a vector3 representation of this vector by casting the doubles to
	 * floats. This creates a new object
	 * 
	 * @return The vector3 representation of this vector3d
	 */
	public Vector3 toVector3() {
		return new Vector3((float) x, (float) y, (float) z);
	}

	/**
	 * Returns set v to this vector by casting doubles to floats.
	 * 
	 * @return The float vector v.
	 */
	public Vector3 setVector3(Vector3 v) {
		return v.set((float) x, (float) y, (float) z);
	}

	/**
	 * Rotates this vector by a quaternion, using "vector rotation" according to
	 * (60) in LL-072. Both the original and the returned vectors are expressed in
	 * the same reference frame as the quaternion.
	 *
	 * @param q Quaternion defining the vector rotation.
	 * @return the rotated vector.
	 */
	public Vector3d rotateVectorByQuaternion(final Quaterniond q) {
		Quaterniond oldVecQ = new Quaterniond(this.x, this.y, this.z, 0.0);
		Quaterniond newVecQ = q.cpy().mul(oldVecQ).mulInverse(q);
		this.x = newVecQ.x;
		this.y = newVecQ.y;
		this.z = newVecQ.z;

		return this;
	}

	/** Gets the angle in degrees between the two vectors **/
	public double angle(Vector3d v) {
		return MathUtilsd.radiansToDegrees * FastMath.acos(this.dot(v) / (this.len() * v.len()));
	}

	/** Gets the angle in degrees between the two vectors **/
	public double anglePrecise(Vector3d v) {
		return MathUtilsd.radiansToDegrees * Math.acos(this.dot(v) / (this.len() * v.len()));
	}

	public boolean hasNaN() {
		return Double.isNaN(this.x) || Double.isNaN(this.y) || Double.isNaN(this.z);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vector3d other = (Vector3d) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

}
