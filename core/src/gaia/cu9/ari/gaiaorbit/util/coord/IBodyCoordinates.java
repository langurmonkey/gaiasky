package gaia.cu9.ari.gaiaorbit.util.coord;

import gaia.cu9.ari.gaiaorbit.scenegraph.Orbit;
import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

import java.time.Instant;

/**
 * Defines the interface to get the coordinates of a body
 *
 * @author Toni Sagrista
 */
public interface IBodyCoordinates {

    /**
     * Initializes the coordinates object
     *
     * @param params
     */
    void doneLoading(Object... params);

    /**
     * Returns the ecliptic coordinates of the body in the out vector for the
     * given date.
     *
     * @param instant The instant.
     * @param out     The out vector with the ecliptic coordinates in internal
     *                units.
     * @return The out vector for chaining.
     */
    Vector3d getEclipticSphericalCoordinates(Instant instant, Vector3d out);

    /**
     * Gets ecliptic cartesian coordinates for the given date.
     *
     * @param instant The instant.
     * @param out     The out vector where the ecliptic cartesian coordinates will
     *                be.
     * @return The out vector for chaining, or null if the date is out of range,
     * in case of non elliptical orbits such as Gaia.
     */
    Vector3d getEclipticCartesianCoordinates(Instant instant, Vector3d out);

    /**
     * Gets equatorial cartesian coordinates for the given date.
     *
     * @param instant The instant.
     * @param out     The out vector where the equatorial cartesian coordinates will
     *                be.
     * @return The out vector for chaining, or null if the date is out of range,
     * in case of non elliptical orbits such as Gaia.
     */
    Vector3d getEquatorialCartesianCoordinates(Instant instant, Vector3d out);

    /**
     * Gets the orbit object of these coordinates, if any.
     *
     * @return
     */
    Orbit getOrbitObject();

}
