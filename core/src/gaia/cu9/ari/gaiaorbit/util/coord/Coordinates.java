package gaia.cu9.ari.gaiaorbit.util.coord;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.math.Matrix4;

import gaia.cu9.ari.gaiaorbit.util.math.Matrix4d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector2d;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

/**
 * Provides utility coordinate conversions between some astronomical coordinate
 * systems and to Cartesian coordinates. All angles are in radians.
 * 
 * @author Toni Sagrista
 *
 */
public class Coordinates {

    /**
     * Obliquity for low precision calculations in degrees and radians. J2000
     * with T=0
     **/
    public static final double OBLIQUITY_DEG_J2000 = 23.4392808;
    public static final double OBLIQUITY_RAD_J2000 = Math.toRadians(OBLIQUITY_DEG_J2000);
    /** Obliquity of ecliptic in J2000 in arcsec **/
    public static final double OBLIQUITY_ARCSEC_J2000 = 84381.41100;

    /**
     * Some galactic system constants J2000 - see
     * https://github.com/astropy/astropy/blob/master/cextern/erfa/icrs2g.c#L71
     * ICRS to galactic rotation matrix, obtained by computing R_3(-R)
     * R_1(pi/2-Q) R_3(pi/2+P)
     **/
    private static final double R = 32.93192;
    private static final double Q = 27.12825;
    private static final double P = 192.85948;

    private static Matrix4d equatorialToEcliptic, eclipticToEquatorial, equatorialToGalactic, galacticToEquatorial, eclipticToGalactic, galacticToEcliptic, mat4didt;
    private static Matrix4 equatorialToEclipticF, eclipticToEquatorialF, equatorialToGalacticF, galacticToEquatorialF, eclipticToGalacticF, galacticToEclipticF, mat4fidt;

    private static Map<String, Matrix4d> mapd;
    private static Map<String, Matrix4> mapf;

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
        mapd = new HashMap<String, Matrix4d>();
        mapf = new HashMap<String, Matrix4>();

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
     * @param alpha
     *            The &alpha; angle in degrees, between z and N.
     * @param beta
     *            The &beta; angle in degrees, between y and Y.
     * @param gamma
     *            The &gamma; angle in degrees, Z and N.
     * @return The rotation matrix.
     */
    public static Matrix4d getRotationMatrix(double alpha, double beta, double gamma) {
        Matrix4d m = new Matrix4d().rotate(0, 1, 0, gamma).rotate(0, 0, 1, beta).rotate(0, 1, 0, alpha);
        return m;
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
     *         coordinates.
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
     *         coordinates.
     */
    public static Matrix4d eqToEcl(double julianDate) {
        return getRotationMatrix(0, -AstroUtils.obliquity(julianDate), 0);
    }

    public static Matrix4d equatorialToEcliptic(double jd) {
        return eqToEcl(jd);
    }

    /**
     * Gets the rotation matrix to transform from the galactic system to the
     * equatorial system. See {@link Coordinates#galToEq()} for more
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
     * Transforms from ecliptic to equatorial coordinates
     * 
     * @param vec
     *            Vector with ecliptic longitude (&lambda;) and ecliptic
     *            latitude (&beta;) in radians.
     * @param out
     *            The output vector.
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     *         for chaining.
     */
    public static Vector2d eclipticToEquatorial(Vector2d vec, Vector2d out) {
        return eclipticToEquatorial(vec.x, vec.y, out);
    }

    /**
     * Transforms from ecliptic to equatorial coordinates
     * 
     * @param lambda
     *            Ecliptic longitude (&lambda;) in radians.
     * @param beta
     *            Ecliptic latitude (&beta;) in radians.
     * @param out
     *            The output vector.
     * @return The output vector with ra (&alpha;) and dec (&delta;) in radians,
     *         for chaining.
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
     * @param vec
     *            Vector containing the spherical coordinates.
     *            <ol>
     *            <li>The longitude or right ascension (&alpha;), from the Z
     *            direction to the X direction, in radians.</li>
     *            <li>The latitude or declination (&delta;), in radians.</li>
     *            <li>The radius or distance to the point.</li>
     *            </ol>
     * @param out
     *            The output vector.
     * @return Output vector in Cartesian coordinates where x and z are on the
     *         horizontal plane and y is in the up direction.
     */
    public static Vector3d sphericalToCartesian(Vector3d vec, Vector3d out) {
        return sphericalToCartesian(vec.x, vec.y, vec.z, out);
    }

    /**
     * Converts from spherical to Cartesian coordinates, given a longitude
     * (&alpha;), a latitude (&delta;) and the radius.
     * 
     * @param longitude
     *            The longitude or right ascension angle, from the z direction
     *            to the x direction, in radians.
     * @param latitude
     *            The latitude or declination, in radians.
     * @param radius
     *            The radius or distance to the point.
     * @param out
     *            The output vector.
     * @return Output vector with the Cartesian coordinates[x, y, z] where x and
     *         z are on the horizontal plane and y is in the up direction, for
     *         chaining.
     */
    public static Vector3d sphericalToCartesian(double longitude, double latitude, double radius, Vector3d out) {
        out.x = radius * Math.cos(latitude) * Math.sin(longitude);
        out.y = radius * Math.sin(latitude);
        out.z = radius * Math.cos(latitude) * Math.cos(longitude);
        return out;
    }

    /**
     * Converts from Cartesian coordinates to spherical coordinates.
     * 
     * @param vec
     *            Vector with the Cartesian coordinates[x, y, z] where x and z
     *            are on the horizontal plane and y is in the up direction.
     * @param out
     *            Output vector.
     * @return Output vector containing the spherical coordinates.
     *         <ol>
     *         <li>The longitude or right ascension (&alpha;), from the z
     *         direction to the x direction.</li>
     *         <li>The latitude or declination (&delta;).</li>
     *         <li>The radius or distance to the point.</li>
     *         </ol>
     */
    public static Vector3d cartesianToSpherical(Vector3d vec, Vector3d out) {
        /**
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
        double distance = (float) Math.sqrt(xsq + ysq + zsq);

        double alpha = Math.atan2(vec.x, vec.z);
        if (alpha < 0) {
            alpha += 2 * Math.PI;
        }

        double delta = 0;
        if (zsq + xsq == 0) {
            delta = (vec.y > 0 ? Math.PI / 2 : -Math.PI / 2);
        } else {
            delta = Math.atan(vec.y / Math.sqrt(zsq + xsq));
        }

        out.x = alpha;
        out.y = delta;
        out.z = distance;
        return out;
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

}
