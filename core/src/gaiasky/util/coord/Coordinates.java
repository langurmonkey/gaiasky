/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import com.badlogic.gdx.math.Matrix4;
import gaiasky.util.Constants;
import gaiasky.util.Nature;
import gaiasky.util.math.*;
import net.jafama.FastMath;

import java.util.HashMap;
import java.util.Map;

public class Coordinates {

    /**
     * Obliquity for low precision calculations in degrees and radians. J2000
     * with T=0
     **/
    public static final double OBLIQUITY_DEG_J2000 = 23.4392808;
    public static final double OBLIQUITY_RAD_J2000 = FastMath.toRadians(OBLIQUITY_DEG_J2000);
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

    private static final Matrix4D equatorialToEcliptic;
    private static final Matrix4D eclipticToEquatorial;
    private static final Matrix4D equatorialToGalactic;
    private static final Matrix4D galacticToEquatorial;
    private static final Matrix4D eclipticToGalactic;
    private static final Matrix4D galacticToEcliptic;
    private static final Matrix4D mat4didt;
    private static final Matrix4 equatorialToEclipticF;
    private static final Matrix4 eclipticToEquatorialF;
    private static final Matrix4 equatorialToGalacticF;
    private static final Matrix4 galacticToEquatorialF;
    private static final Matrix4 eclipticToGalacticF;
    private static final Matrix4 galacticToEclipticF;
    private static final Matrix4 mat4fidt;

    private static final Map<String, Matrix4D> mapd;
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
        equatorialToGalactic = new Matrix4D(galacticToEquatorial).inv();
        equatorialToGalacticF = equatorialToGalactic.putIn(new Matrix4());

        // ECL -> GAL
        eclipticToGalactic = new Matrix4D(galacticToEquatorial).mul(equatorialToEcliptic);
        eclipticToGalacticF = eclipticToGalactic.putIn(new Matrix4());

        // GAL -> ECL
        galacticToEcliptic = new Matrix4D(eclipticToEquatorial).mul(equatorialToGalactic);
        galacticToEclipticF = galacticToEcliptic.putIn(new Matrix4());

        // Identities
        mat4didt = new Matrix4D();
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

    public static Map<String, Matrix4D> getMap() {
        return mapd;
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
     *
     * @return The rotation matrix.
     */
    public static Matrix4D getRotationMatrix(double alpha, double beta, double gamma) {
        return new Matrix4D().rotate(0, 1, 0, gamma)
                .rotate(0, 0, 1, beta)
                .rotate(0, 1, 0, alpha);
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
    public static Matrix4D eclToEq() {
        //return getRotationMatrix(0, obliquity, 0);
        return eclipticToEquatorial;
    }

    public static Matrix4D eclipticToEquatorial() {
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
    public static Matrix4D eclToEq(double julianDate) {
        return getRotationMatrix(0, AstroUtils.obliquity(julianDate), 0);
    }

    public static Matrix4D eclipticToEquatorial(double jd) {
        return eclToEq(jd);
    }

    /**
     * Gets the rotation matrix to transform from the ecliptic system to the
     * equatorial system. See {@link Coordinates#eclToEq()} for more
     * information, for this is the inverse transformation.
     *
     * @return The transformation matrix.
     */
    public static Matrix4D eqToEcl() {
        //return getRotationMatrix(0, -obliquity, 0);
        return equatorialToEcliptic;
    }

    public static Matrix4D equatorialToEcliptic() {
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
    public static Matrix4D eqToEcl(double julianDate) {
        return getRotationMatrix(0, -AstroUtils.obliquity(julianDate), 0);
    }

    public static Matrix4D equatorialToEcliptic(double jd) {
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
    public static Matrix4D galToEq() {
        return galacticToEquatorial;
    }

    public static Matrix4D galacticToEquatorial() {
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
    public static Matrix4D eqToGal() {
        return equatorialToGalactic;
    }

    public static Matrix4D equatorialToGalactic() {
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
     *
     * @return The out vector with the galactic longitude and latitude, in radians.
     */
    public static Vector2D equatorialToGalactic(double alpha, double delta, Vector2D out) {
        // To equatorial cartesian
        Vector3D aux = new Vector3D(alpha, delta, Constants.PC_TO_U);
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
     * Transforms from spherical galactic coordinates to spherical equatorial coordinates.
     *
     * @param l   The galactic longitude in radians.
     * @param b   The galactic latitude in radians.
     * @param out The out vector.
     *
     * @return The out vector with the right ascension and declination, in radians.
     */
    public static Vector2D galacticToEquatorial(double l, double b, Vector2D out) {
        // To equatorial cartesian
        Vector3D aux = new Vector3D(l, b, Constants.PC_TO_U);
        sphericalToCartesian(aux, aux);

        // Rotate to galactic cartesian
        aux.mul(galacticToEquatorial);

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
     *
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     * for chaining.
     */
    public static Vector2D eclipticToEquatorial(Vector2D vec, Vector2D out) {
        return eclipticToEquatorial(vec.x, vec.y, out);
    }

    /**
     * Transforms from spherical ecliptic coordinates to spherical equatorial coordinates.
     *
     * @param lambda Ecliptic longitude (&lambda;) in radians.
     * @param beta   Ecliptic latitude (&beta;) in radians.
     * @param out    The output vector.
     *
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     * for chaining.
     */
    public static Vector2D eclipticToEquatorial(double lambda, double beta, Vector2D out) {

        double alpha = FastMath.atan2(
                (Math.sin(lambda) * FastMath.cos(OBLIQUITY_RAD_J2000) - FastMath.tan(beta) * FastMath.sin(OBLIQUITY_RAD_J2000)),
                FastMath.cos(lambda));
        if (alpha < 0) {
            alpha += FastMath.PI * 2;
        }
        double delta = FastMath.asin(Math.sin(beta) * FastMath.cos(OBLIQUITY_RAD_J2000) + FastMath.cos(beta) * FastMath.sin(
                OBLIQUITY_RAD_J2000) * FastMath.sin(lambda));

        return out.set(alpha, delta);
    }

    public static Matrix4D eclipticToGalactic() {
        return eclipticToGalactic;
    }

    public static Matrix4 eclipticToGalacticF() {
        return eclipticToGalacticF;
    }

    public static Matrix4D galacticToEcliptic() {
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
     *
     * @return Output vector in Cartesian coordinates where x and z are on the
     * horizontal plane and y is in the up direction.
     */
    public static Vector3D sphericalToCartesian(Vector3D vec, Vector3D out) {
        return sphericalToCartesian(vec.x, vec.y, vec.z, out);
    }

    public static Vector3Q sphericalToCartesian(Vector3Q vec, Vector3Q out) {
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
     *
     * @return Output vector with the Cartesian coordinates[x, y, z] where x and
     * z are on the horizontal plane and y is in the up direction, for
     * chaining.
     */
    public static Vector3D sphericalToCartesian(double longitude, double latitude, double radius, Vector3D out) {
        out.x = radius * FastMath.cos(latitude) * FastMath.sin(longitude);
        out.y = radius * FastMath.sin(latitude);
        out.z = radius * FastMath.cos(latitude) * FastMath.cos(longitude);
        return out;
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
     *
     * @return Output vector with the Cartesian coordinates[x, y, z] where x and
     * z are on the horizontal plane and y is in the up direction, for
     * chaining.
     */
    public static Vector3Q sphericalToCartesian(double longitude, double latitude, Quadruple radius, Vector3Q out) {
        out.x.assign(radius).multiply(new Quadruple(Math.cos(latitude) * FastMath.sin(longitude)));
        out.y.assign(radius).multiply(new Quadruple(Math.sin(latitude)));
        out.z.assign(radius).multiply(new Quadruple(Math.cos(latitude) * FastMath.cos(longitude)));
        return out;
    }

    /**
     * Converts from Cartesian coordinates to spherical coordinates.
     *
     * @param vec Vector with the Cartesian coordinates[x, y, z] where x and z
     *            are on the horizontal plane and y is in the up direction.
     * @param out Output vector.
     *
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3D cartesianToSpherical(Vector3D vec, Vector3D out) {
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

        double alpha = FastMath.atan2(vec.x, vec.z);
        if (alpha < 0) {
            alpha += 2 * FastMath.PI;
        }

        double delta;
        if (zsq + xsq == 0) {
            delta = (vec.y > 0 ? FastMath.PI / 2 : -Math.PI / 2);
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
     *
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3D cartesianToSpherical(Vector3Q vec, Vector3D out) {
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
        if (vec == null || out == null) {
            return null;
        }

        double x = vec.x.doubleValue();
        double y = vec.y.doubleValue();
        double z = vec.z.doubleValue();

        double xsq = x * x;
        double ysq = y * y;
        double zsq = z * z;
        double distance = FastMath.sqrt(xsq + ysq + zsq);

        double alpha = FastMath.atan2(x, z);
        if (alpha < 0) {
            alpha += 2 * FastMath.PI;
        }

        double delta;
        if (zsq + xsq == 0) {
            delta = (y > 0 ? FastMath.PI / 2 : -Math.PI / 2);
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
     *
     * @return Output vector containing the spherical coordinates.
     * <ol>
     * <li>The longitude or right ascension (&alpha;), from the z
     * direction to the x direction.</li>
     * <li>The latitude or declination (&delta;).</li>
     * <li>The radius or distance to the point.</li>
     * </ol>
     */
    public static Vector3Q cartesianToSpherical(Vector3Q vec, Vector3Q out) {
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

        Quadruple xsq = vec.x.multiply(vec.x);
        Quadruple ysq = vec.y.multiply(vec.y);
        Quadruple zsq = vec.z.multiply(vec.z);
        Quadruple distance = xsq.add(ysq).add(zsq).sqrt();

        Quadruple alpha = QuadrupleMath.atan2(vec.x, vec.z);
        if (alpha.doubleValue() < 0) {
            alpha.add(QuadrupleMath.pi2());
        }

        Quadruple delta;
        if (zsq.add(xsq).doubleValue() == 0) {
            Quadruple piOverTwo = QuadrupleMath.piOver2();
            delta = (vec.y.doubleValue() > 0 ? piOverTwo : piOverTwo.multiply(Quadruple.valueOf(-1.0)));
        } else {
            delta = QuadrupleMath.atan(vec.y.divide(zsq.add(xsq).sqrt()));
        }

        return out.set(alpha, delta, distance);
    }

    /**
     * Converts proper motions + radial velocity into a cartesian vector.
     * See <a href="http://www.astronexus.com/a-a/motions-long-term">this article</a>.
     *
     * @param muAlphaStar Mu alpha star, in mas/yr.
     * @param muDelta     Mu delta, in mas/yr.
     * @param radVel      Radial velocity in km/s.
     * @param ra          Right ascension in radians.
     * @param dec         Declination in radians.
     * @param distPc      Distance in parsecs to the star.
     *
     * @return The proper motion vector in internal_units/year.
     */
    public static Vector3D properMotionsToCartesian(double muAlphaStar, double muDelta, double radVel, double ra, double dec,
                                                    double distPc,
                                                    Vector3D out) {
        double ma = muAlphaStar * Nature.MILLIARCSEC_TO_ARCSEC;
        double md = muDelta * Nature.MILLIARCSEC_TO_ARCSEC;

        // Multiply arcsec/yr with distance in parsecs gives a linear velocity. The factor 4.74 converts result to km/s
        double vta = ma * distPc * Nature.ARCSEC_PER_YEAR_TO_KMS;
        double vtd = md * distPc * Nature.ARCSEC_PER_YEAR_TO_KMS;

        double cosAlpha = FastMath.cos(ra);
        double sinAlpha = FastMath.sin(ra);
        double cosDelta = FastMath.cos(dec);
        double sinDelta = FastMath.sin(dec);

        // +x to delta=0, alpha=0
        // +y to delta=0, alpha=90
        // +z to delta=90
        // components in km/s

        /*
         * vx = (vR cos \delta cos \alpha) - (vTA sin \alpha) - (vTD sin \delta cos \alpha)
         * vy = (vR cos \delta sin \alpha) + (vTA cos \alpha) - (vTD sin \delta sin \alpha)
         * vz = vR sin \delta + vTD cos \delta
         */
        double vx = (radVel * cosDelta * cosAlpha) - (vta * sinAlpha) - (vtd * sinDelta * cosAlpha);
        double vy = (radVel * cosDelta * sinAlpha) + (vta * cosAlpha) - (vtd * sinDelta * sinAlpha);
        double vz = (radVel * sinDelta) + (vtd * cosDelta);

        return (out.set(vy, vz, vx)).scl(Constants.KM_TO_U / Nature.S_TO_Y);

    }

    /**
     * Converts a cartesian velocity vector [vx,vy,vz] into proper motions + radial velocity.
     * See <a href="http://www.astronexus.com/a-a/motions-long-term">this article</a>.
     *
     * @param vx     The X component of the cartesian velocity vector in internal_units/year.
     * @param vy     The Y component of the cartesian velocity vector internal_units/year.
     * @param vz     The Z component of the cartesian velocity vector internal_units/year.
     * @param ra     Right ascension in radians.
     * @param dec    Declination in radians.
     * @param distPc Distance in parsecs to the star.
     *
     * @return The proper motions (muAlpha, muDelta) in mas/yr, and the radial velocity in km/s.
     */
    public static Vector3D cartesianToProperMotions(double vx, double vy, double vz,
                                                    double ra, double dec, double distPc,
                                                    Vector3D out) {
        // Precompute constants for conversion
        double kmPerYearToInternal = Constants.U_TO_KM / Nature.Y_TO_S;
        double arcsecPerYearToKm = Nature.ARCSEC_PER_YEAR_TO_KMS * distPc;

        // Convert from internal units/year to km/s
        double vxKms = vz * kmPerYearToInternal;
        double vyKms = vx * kmPerYearToInternal;
        double vzKms = vy * kmPerYearToInternal;

        // Unit vectors
        double cosA = FastMath.cos(ra);
        double sinA = FastMath.sin(ra);
        double cosD = FastMath.cos(dec);
        double sinD = FastMath.sin(dec);

        double rx = cosD * cosA;
        double ry = cosD * sinA;
        double rz = sinD;

        double ax = -sinA;
        double ay = cosA;
        double az = 0.0;

        double dx = -cosA * sinD;
        double dy = -sinA * sinD;
        double dz = cosD;

        double vr = vxKms * rx + vyKms * ry + vzKms * rz;
        double vta = vxKms * ax + vyKms * ay + vzKms * az;
        double vtd = vxKms * dx + vyKms * dy + vzKms * dz;

        double muAlphaStar = (vta / arcsecPerYearToKm) * Nature.ARCSEC_TO_MILLIARCSEC;
        double muDelta = (vtd / arcsecPerYearToKm) * Nature.ARCSEC_TO_MILLIARCSEC;

        return out.set(muAlphaStar, muDelta, vr);
    }

    public static Matrix4D getTransformD(String name) {
        if (name == null || name.isEmpty() || !mapf.containsKey(name))
            return mat4didt;
        return mapd.get(name.toLowerCase());
    }

    public static Matrix4 getTransformF(String name) {
        if (name == null || name.isEmpty() || !mapf.containsKey(name))
            return mat4fidt;
        return mapf.get(name.toLowerCase());
    }

    public static Matrix4D idt() {
        return mat4didt;
    }

    public static Matrix4 idtF() {
        return mat4fidt;
    }

}
