/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.Constants;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;
import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;

import java.util.HashMap;
import java.util.Map;

public class Coordinates {

    /**
     * Obliquity for low precision calculations in degrees and radians. J2000
     * with T=0
     **/
    public static final double OBLIQUITY_DEG_J2000 = 23.4392808;
    public static final double OBLIQUITY_RAD_J2000 = Math.toRadians(OBLIQUITY_DEG_J2000);
    /**
     * Obliquity of ecliptic in J2000 in arcsec
     **/
    public static final double OBLIQUITY_ARCSEC_J2000 = 84381.41100;

    /**
     * Some galactic system constants J2000 - see
     * <a href="https://github.com/astropy/astropy/blob/master/cextern/erfa/icrs2g.c#L71">this link</a>.
     * ICRS to galactic rotation matrix, obtained by computing R_3(-R)
     * R_1(pi/2-Q) R_3(pi/2+P)
     **/
    private static final double R = 32.93192;
    private static final double Q = 27.12825;
    private static final double P = 192.85948;

    private static final Matrix4d equatorialToEcliptic;
    private static final Matrix4d eclipticToEquatorial;
    private static final Matrix4d equatorialToGalactic;
    private static final Matrix4d galacticToEquatorial;
    private static final Matrix4d eclipticToGalactic;
    private static final Matrix4d galacticToEcliptic;
    private static final Matrix4d mat4didt;
    private static final Matrix4 equatorialToEclipticF;
    private static final Matrix4 eclipticToEquatorialF;
    private static final Matrix4 equatorialToGalacticF;
    private static final Matrix4 galacticToEquatorialF;
    private static final Matrix4 eclipticToGalacticF;
    private static final Matrix4 galacticToEclipticF;
    private static final Matrix4 mat4fidt;

    private static final Map<String, Matrix4d> mapd;
    private static final Map<String, Matrix4> mapf;

    static {
        // Initialize matrices

        // EQ -> ECL
        equatorialToEcliptic = getRotationMatrix(0, -OBLIQUITY_DEG_J2000, 0);
        equatorialToEclipticF = equatorialToEcliptic.putIn(new Matrix4());

        // ECL -> EQ
        eclipticToEquatorial = getRotationMatrix(0, OBLIQUITY_DEG_J2000, 0);
        eclipticToEquatorialF = eclipticToEquatorial.putIn(new Matrix4());

        // GAL -> EQ
        galacticToEquatorial = getRotationMatrix(-R, 90 - Q, 90 + P);
        galacticToEquatorialF = galacticToEquatorial.putIn(new Matrix4());

        // EQ -> GAL
        equatorialToGalactic = new Matrix4d(galacticToEquatorial).inv();
        equatorialToGalacticF = equatorialToGalactic.putIn(new Matrix4());

        // ECL -> GAL
        eclipticToGalactic = new Matrix4d(galacticToEquatorial).mul(equatorialToEcliptic);
        eclipticToGalacticF = eclipticToGalactic.putIn(new Matrix4());

        // GAL -> ECL
        galacticToEcliptic = new Matrix4d(eclipticToEquatorial).mul(equatorialToGalactic);
        galacticToEclipticF = galacticToEcliptic.putIn(new Matrix4());

        // Identities
        mat4didt = new Matrix4d();
        mat4fidt = new Matrix4();

        // Init maps
        mapd = new HashMap<>();
        mapf = new HashMap<>();

        mapd.put("equatorialtoecliptic", equatorialToEcliptic);
        mapd.put("eqtoecl", equatorialToEcliptic);
        mapf.put("equatorialtoecliptic", equatorialToEclipticF);
        mapf.put("eqtoecl", equatorialToEclipticF);

        mapd.put("ecliptictoequatorial", eclipticToEquatorial);
        mapd.put("ecltoeq", eclipticToEquatorial);
        mapf.put("ecliptictoequatorial", eclipticToEquatorialF);
        mapf.put("ecltoeq", eclipticToEquatorialF);

        mapd.put("galactictoequatorial", galacticToEquatorial);
        mapd.put("galtoeq", galacticToEquatorial);
        mapf.put("galactictoequatorial", galacticToEquatorialF);
        mapf.put("galtoeq", galacticToEquatorialF);

        mapd.put("equatorialtogalactic", equatorialToGalactic);
        mapd.put("eqtogal", equatorialToGalactic);
        mapf.put("equatorialtogalactic", equatorialToGalacticF);
        mapf.put("eqtogal", equatorialToGalacticF);

        mapd.put("ecliptictogalactic", eclipticToGalactic);
        mapd.put("ecltogal", eclipticToGalactic);
        mapf.put("ecliptictogalactic", eclipticToGalacticF);
        mapf.put("ecltogal", eclipticToGalacticF);

        mapd.put("galactictoecliptic", galacticToEcliptic);
        mapd.put("galtoecl", galacticToEcliptic);
        mapf.put("galactictoecliptic", galacticToEclipticF);
        mapf.put("galtoecl", galacticToEclipticF);

    }

    /**
     * Gets the rotation matrix to apply for the given Euler angles &alpha;,
     * &beta; and &gamma;. It applies Ry(&gamma;)*Rz(&beta;)*Ry(&alpha;), so
     * that it rotates the fixed xyz system to make it coincide with the XYZ,
     * where &alpha; is the angle between the axis z and the line of nodes N,
     * &beta; is the angle between the y axis and the Y axis, and &gamma; is the
     * angle between the Z axis and the line of nodes N.<br/>
     * The assumed reference system is as follows:
     * <ul>
     * <li>ZX is the fundamental plane.</li>
     * <li>Z points to the origin of the reference plane (the line of nodes N).
     * </li>
     * <li>Y points upwards.</li>
     * </ul>
     *
     * @param alpha The &alpha; angle in degrees, between z and N.
     * @param beta  The &beta; angle in degrees, between y and Y.
     * @param gamma The &gamma; angle in degrees, Z and N.
     * @return The rotation matrix.
     */
    public static Matrix4d getRotationMatrix(double alpha, double beta, double gamma) {
        return new Matrix4d().rotate(0, 1, 0, gamma).rotate(0, 0, 1, beta).rotate(0, 1, 0, alpha);
    }

    /**
     * Gets the rotation matrix to transform equatorial to the ecliptic
     * coordinates. Since the zero point in both systems is the same (the vernal
     * equinox, &gamma;, defined as the intersection between the equator and the
     * ecliptic), &alpha; and &gamma; are zero. &beta;, the angle between the up
     * directions of both systems, is precisely the obliquity of the ecliptic,
     * &epsilon;. So we have the Euler angles &alpha;=0&deg;, &beta;=&epsilon;;,
     * &gamma;=0&deg;.
     *
     * @return The matrix to transform from equatorial coordinates to ecliptic
     * coordinates.
     */
    public static Matrix4d eclToEq() {
        //return getRotationMatrix(0, obliquity, 0);
        return eclipticToEquatorial;
    }

    public static Matrix4d eclipticToEquatorial() {
        return eclToEq();
    }

    public static Matrix4 eclToEqF() {
        //return getRotationMatrix(0, obliquity, 0);
        return eclipticToEquatorialF;
    }

    public static Matrix4 eclipticToEquatorialF() {
        return eclToEqF();
    }

    /**
     * Gets the rotation matrix to transform from the ecliptic system to the
     * equatorial system. See {@link Coordinates#equatorialToEcliptic()} for
     * more information, for this is the inverse transformation.
     *
     * @return The transformation matrix.
     */
    public static Matrix4d eclToEq(double julianDate) {
        return getRotationMatrix(0, AstroUtils.obliquity(julianDate), 0);
    }

    public static Matrix4d eclipticToEquatorial(double jd) {
        return eclToEq(jd);
    }

    /**
     * Gets the rotation matrix to transform from the ecliptic system to the
     * equatorial system. See {@link Coordinates#eclToEq()} for more
     * information, for this is the inverse transformation.
     *
     * @return The transformation matrix.
     */
    public static Matrix4d eqToEcl() {
        //return getRotationMatrix(0, -obliquity, 0);
        return equatorialToEcliptic;
    }

    public static Matrix4d equatorialToEcliptic() {
        return eqToEcl();
    }

    public static Matrix4 eqToEclF() {
        //return getRotationMatrix(0, -obliquity, 0);
        return equatorialToEclipticF;
    }

    public static Matrix4 equatorialToEclipticF() {
        return eqToEclF();
    }

    /**
     * Gets the rotation matrix to transform equatorial to the ecliptic
     * coordinates. Since the zero point in both systems is the same (the vernal
     * equinox, &gamma;, defined as the intersection between the equator and the
     * ecliptic), &alpha; and &gamma; are zero. &beta;, the angle between the up
     * directions of both systems, is precisely the obliquity of the ecliptic,
     * &epsilon;. So we have the Euler angles &alpha;=0&deg;, &beta;=&epsilon;;,
     * &gamma;=0&deg;.
     *
     * @return The matrix to transform from equatorial coordinates to ecliptic
     * coordinates.
     */
    public static Matrix4d eqToEcl(double julianDate) {
        return getRotationMatrix(0, -AstroUtils.obliquity(julianDate), 0);
    }

    public static Matrix4d equatorialToEcliptic(double jd) {
        return eqToEcl(jd);
    }

    /**
     * Gets the rotation matrix to transform from the galactic system to the
     * equatorial system. See {@link Coordinates#galacticToEquatorial()} for more
     * information, since this is the inverse transformation. Use this matrix if
     * you need to convert equatorial cartesian coordinates to galactic
     * cartesian coordinates.
     *
     * @return The transformation matrix.
     */
    public static Matrix4d galToEq() {
        return galacticToEquatorial;
    }

    public static Matrix4d galacticToEquatorial() {
        return galToEq();
    }

    public static Matrix4 galToEqF() {
        return galacticToEquatorialF;
    }

    public static Matrix4 galacticToEquatorialF() {
        return galToEqF();
    }

    /**
     * Gets the rotation matrix to transform equatorial to galactic coordinates.
     * The inclination of the galactic equator to the celestial equator is
     * 62.9&deg;. The intersection, or node line, of the two equators is at
     * RA=282.25&deg; DEC=0&deg; and l=33&deg; b=0&deg;. So we have the Euler
     * angles &alpha;=-33&deg;, &beta;=62.9&deg;, &gamma;=282.25&deg;.
     *
     * @return The transformation matrix.
     */
    public static Matrix4d eqToGal() {
        return equatorialToGalactic;
    }

    public static Matrix4d equatorialToGalactic() {
        return eqToGal();
    }

    public static Matrix4 eqToGalF() {
        return equatorialToGalacticF;
    }

    public static Matrix4 equatorialToGalacticF() {
        return eqToGalF();
    }

    /**
     * Transforms from spherical equatorial coordinates to spherical galactic coordinates.
     *
     * @param alpha The right ascension in radians.
     * @param delta The declination in radians.
     * @param out   The out vector.
     * @return The out vector with the galactic longitude and latitude, in radians.
     */
    public static Vector2d equatorialToGalactic(double alpha, double delta, Vector2d out) {
        // To equatorial cartesian
        Vector3d aux = new Vector3d(alpha, delta, 1);
        sphericalToCartesian(aux, aux);

        // Rotate to galactic cartesian
        aux.mul(equatorialToGalactic);

        // Back to spherical
        cartesianToSpherical(aux, aux);

        out.x = aux.x;
        out.y = aux.y;

        return out;
    }

    /**
     * Transforms from spherical ecliptic coordinates to spherical equatorial coordinates.
     *
     * @param vec Vector with ecliptic longitude (&lambda;) and ecliptic
     *            latitude (&beta;) in radians.
     * @param out The output vector.
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     * for chaining.
     */
    public static Vector2d eclipticToEquatorial(Vector2d vec, Vector2d out) {
        return eclipticToEquatorial(vec.x, vec.y, out);
    }

    /**
     * Transforms from spherical ecliptic coordinates to spherical equatorial coordinates.
     *
     * @param lambda Ecliptic longitude (&lambda;) in radians.
     * @param beta   Ecliptic latitude (&beta;) in radians.
     * @param out    The output vector.
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     * for chaining.
     */
    public static Vector2d eclipticToEquatorial(double lambda, double beta, Vector2d out) {

        double alpha = Math.atan2((Math.sin(lambda) * Math.cos(OBLIQUITY_RAD_J2000) - Math.tan(beta) * Math.sin(OBLIQUITY_RAD_J2000)), Math.cos(lambda));
        if (alpha < 0) {
            alpha += Math.PI * 2;
        }
        double delta = Math.asin(Math.sin(beta) * Math.cos(OBLIQUITY_RAD_J2000) + Math.cos(beta) * Math.sin(OBLIQUITY_RAD_J2000) * Math.sin(lambda));

        return out.set(alpha, delta);
    }

    public static Matrix4d eclipticToGalactic() {
        return eclipticToGalactic;
    }

    public static Matrix4 eclipticToGalacticF() {
        return eclipticToGalacticF;
    }

    public static Matrix4d galacticToEcliptic() {
        return galacticToEcliptic;
    }

    public static Matrix4 galacticToEclipticF() {
        return galacticToEclipticF;
    }

    /**
     * Converts from spherical to Cartesian coordinates, given a longitude
     * (&alpha;), a latitude (&delta;) and the radius. The result is in the XYZ
     * space, where ZX is the fundamental plane, with Z pointing to the the
     * origin of coordinates (equinox) and Y pointing to the north pole.
     *
     * @param vec Vector containing the spherical coordinates.
     *            <ol>
     *            <li>The longitude or right ascension (&alpha;), from the Z
     *            direction to the X direction, in radians.</li>
     *            <li>The latitude or declination (&delta;), in radians.</li>
     *            <li>The radius or distance to the point.</li>
     *            </ol>
     * @param out The output vector.
     * @return Output vector in Cartesian coordinates where x and z are on the
     * horizontal plane and y is in the up direction.
     */
    public static Vector3d sphericalToCartesian(Vector3d vec, Vector3d out) {
        return sphericalToCartesian(vec.x, vec.y, vec.z, out);
    }

    public static Vector3b sphericalToCartesian(Vector3b vec, Vector3b out) {
        return sphericalToCartesian(vec.x.doubleValue(), vec.y.doubleValue(), vec.z, out);
    }

    /**
     * Converts from spherical to Cartesian coordinates, given a longitude
     * (&alpha;), a latitude (&delta;) and the radius.
     *
     * @param longitude The longitude or right ascension angle, from the z direction
     *                  to the x direction, in radians.
     * @param latitude  The latitude or declination, in radians.
     * @param radius    The radius or distance to the point.
     * @param out       The output vector.
     * @return Output vector with the Cartesian coordinates[x, y, z] where x and
     * z are on the horizontal plane and y is in the up direction, for
     * chaining.
     */
    public static Vector3d sphericalToCartesian(double longitude, double latitude, double radius, Vector3d out) {
        out.x = radius * Math.cos(latitude) * Math.sin(longitude);
        out.y = radius * Math.sin(latitude);
        out.z = radius * Math.cos(latitude) * Math.cos(longitude);
        return out;
    }

    public static Vector3b sphericalToCartesian(double longitude, double latitude, Apfloat radius, Vector3b out) {
        out.x = radius.multiply(new Apfloat(Math.cos(latitude) * Math.sin(longitude), Constants.PREC));
        out.y = radius.multiply(new Apfloat(Math.sin(latitude), Constants.PREC));
        out.z = radius.multiply(new Apfloat(Math.cos(latitude) * Math.cos(longitude), Constants.PREC));
        return out;
    }

    /**
     * Converts from Cartesian coordinates to spherical coordinates.
     *
     * @param vec Vector with the Cartesian coordinates[x, y, z] where x and z
     *            are on the horizontal plane and y is in the up direction.
     * @param out Output vector.
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3d cartesianToSpherical(Vector3d vec, Vector3d out) {
        /*
         *
         * x, y, z = values[:] xsq = x ** 2 ysq = y ** 2 zsq = z ** 2 distance =
         * math.sqrt(xsq + ysq + zsq)
         *
         * alpha = math.atan2(y, x) # Correct the value of alpha depending upon
         * the quadrant. if alpha < 0: alpha += 2 * math.pi
         *
         * if (xsq + ysq) == 0: # In the case of the poles, delta is -90 or +90
         * delta = math.copysign(math.pi / 2, z) else: delta = math.atan(z /
         * math.sqrt(xsq + ysq))
         */
        double xsq = vec.x * vec.x;
        double ysq = vec.y * vec.y;
        double zsq = vec.z * vec.z;
        double distance = FastMath.sqrt(xsq + ysq + zsq);

        double alpha = Math.atan2(vec.x, vec.z);
        if (alpha < 0) {
            alpha += 2 * Math.PI;
        }

        double delta;
        if (zsq + xsq == 0) {
            delta = (vec.y > 0 ? Math.PI / 2 : -Math.PI / 2);
        } else {
            delta = FastMath.atan(vec.y / FastMath.sqrt(zsq + xsq));
        }

        out.x = alpha;
        out.y = delta;
        out.z = distance;
        return out;
    }

    /**
     * Converts from Cartesian coordinates to spherical coordinates.
     *
     * @param vec Vector with the Cartesian coordinates[x, y, z] where x and z
     *            are on the horizontal plane and y is in the up direction.
     * @param out Output vector.
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3d cartesianToSpherical(Vector3b vec, Vector3d out) {
        /*
         *
         * x, y, z = values[:] xsq = x ** 2 ysq = y ** 2 zsq = z ** 2 distance =
         * math.sqrt(xsq + ysq + zsq)
         *
         * alpha = math.atan2(y, x) # Correct the value of alpha depending upon
         * the quadrant. if alpha < 0: alpha += 2 * math.pi
         *
         * if (xsq + ysq) == 0: # In the case of the poles, delta is -90 or +90
         * delta = math.copysign(math.pi / 2, z) else: delta = math.atan(z /
         * math.sqrt(xsq + ysq))
         */

        double x = vec.x.doubleValue();
        double y = vec.y.doubleValue();
        double z = vec.z.doubleValue();

        double xsq = x * x;
        double ysq = y * y;
        double zsq = z * z;
        double distance = FastMath.sqrt(xsq + ysq + zsq);

        double alpha = Math.atan2(x, z);
        if (alpha < 0) {
            alpha += 2 * Math.PI;
        }

        double delta;
        if (zsq + xsq == 0) {
            delta = (y > 0 ? Math.PI / 2 : -Math.PI / 2);
        } else {
            delta = FastMath.atan(y / FastMath.sqrt(zsq + xsq));
        }

        out.x = alpha;
        out.y = delta;
        out.z = distance;
        return out;
    }

    /**
     * Converts from Cartesian coordinates to spherical coordinates.
     *
     * @param vec Vector with the Cartesian coordinates[x, y, z] where x and z
     *            are on the horizontal plane and y is in the up direction.
     * @param out Output vector.
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3b cartesianToSpherical(Vector3b vec, Vector3b out) {
        /*
         *
         * x, y, z = values[:] xsq = x ** 2 ysq = y ** 2 zsq = z ** 2 distance =
         * math.sqrt(xsq + ysq + zsq)
         *
         * alpha = math.atan2(y, x) # Correct the value of alpha depending upon
         * the quadrant. if alpha < 0: alpha += 2 * math.pi
         *
         * if (xsq + ysq) == 0: # In the case of the poles, delta is -90 or +90
         * delta = math.copysign(math.pi / 2, z) else: delta = math.atan(z /
         * math.sqrt(xsq + ysq))
         */

        Apfloat xsq = vec.x.multiply(vec.x);
        Apfloat ysq = vec.y.multiply(vec.y);
        Apfloat zsq = vec.z.multiply(vec.z);
        Apfloat distance = ApfloatMath.sqrt(xsq.add(ysq).add(zsq));

        Apfloat alpha = ApfloatMath.atan2(vec.x, vec.z);
        if (alpha.doubleValue() < 0) {
            alpha = alpha.add(ApfloatMath.pi(Constants.PREC).multiply(new Apfloat(2, Constants.PREC)));
        }

        Apfloat delta;
        if (zsq.add(xsq).doubleValue() == 0) {
            Apfloat piOverTwo = ApfloatMath.pi(Constants.PREC).divide(new Apfloat(2, Constants.PREC));
            delta = (vec.y.doubleValue() > 0 ? piOverTwo : piOverTwo.multiply(new Apfloat(-1, Constants.PREC)));
        } else {
            delta = ApfloatMath.atan(vec.y.divide(ApfloatMath.sqrt(zsq.add(xsq))));
        }

        return out.set(alpha, delta, distance);
    }

    public static Matrix4d getTransformD(String name) {
        if (name == null || name.isEmpty() || !mapf.containsKey(name))
            return mat4didt;
        return mapd.get(name.toLowerCase());
    }

    public static Matrix4 getTransformF(String name) {
        if (name == null || name.isEmpty() || !mapf.containsKey(name))
            return mat4fidt;
        return mapf.get(name.toLowerCase());
    }

    public static Matrix4d idt() {
        return mat4didt;
    }

    public static Matrix4 idtF() {
        return mat4fidt;
    }

}
