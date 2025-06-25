/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.script.v2.impl.RefsysModule;

/**
 * API definition for the reference system module, {@link RefsysModule}.
 * <p>
 * The reference system module contains calls and methods to deal with reference system changes and other
 * useful utilities related to orientation.
 */
public interface RefsysAPI {

    /**
     * Returns the column-major matrix representing the given reference system transformation.
     *
     * @param name <p>The name of the reference system transformation:</p>
     *             <ul>
     *             <li>'equatorialtoecliptic', 'eqtoecl'</li>
     *             <li>'ecliptictoequatorial', 'ecltoeq'</li>
     *             <li>'galactictoequatorial', 'galtoeq'</li>
     *             <li>'equatorialtogalactic', 'eqtogal'</li>
     *             <li>'ecliptictogalactic', 'ecltogal</li>
     *             <li>'galactictoecliptic', 'galtoecl</li>
     *             </ul>
     *
     * @return The transformation matrix in column-major order.
     */
    double[] get_transform_matrix(String name);

    /**
     * Convert galactic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The galactic longitude in degrees.
     * @param b The galactic latitude in degrees.
     * @param r The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     *         internal reference system, in internal units.
     */
    double[] galactic_to_cartesian(double l,
                                   double b,
                                   double r);

    /**
     * Convert ecliptic coordinates to the internal cartesian coordinate
     * system.
     *
     * @param l The ecliptic longitude in degrees.
     * @param b The ecliptic latitude in degrees.
     * @param r The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     *         internal reference system, in internal units.
     */
    double[] ecliptic_to_cartesian(double l,
                                   double b,
                                   double r);

    /**
     * Convert equatorial coordinates to the internal cartesian coordinate
     * system.
     *
     * @param ra  The right ascension in degrees.
     * @param dec The declination in degrees.
     * @param r   The distance in Km.
     *
     * @return An array of doubles containing <code>[x, y, z]</code> in the
     *         internal reference system, in internal units.
     */
    double[] equatorial_to_cartesian(double ra,
                                     double dec,
                                     double r);

    /**
     * Convert internal cartesian coordinates to equatorial
     * <code>[ra, dec, distance]</code> coordinates.
     *
     * @param x The x component, in any distance units.
     * @param y The y component, in any distance units.
     * @param z The z component, in any distance units.
     *
     * @return An array of doubles containing <code>[ra, dec, distance]</code>
     *         with <code>ra</code> and <code>dec</code> in degrees and
     *         <code>distance</code> in the same distance units as the input
     *         position.
     */
    double[] cartesian_to_equatorial(double x,
                                     double y,
                                     double z);

    /**
     * Convert regular cartesian coordinates, where XY is the equatorial plane, with X pointing to
     * the vernal equinox (ra=0) and Y points to ra=90, and Z pointing to the celestial North Pole (dec=90)
     * to internal cartesian coordinates with internal units.
     *
     * @param eq       Equatorial cartesian coordinates (X->[ra=0,dec=0], Y->[ra=90,dec=0], Z->[ra=0,dec=90])
     * @param kmFactor Factor used to bring the input coordinate units to Kilometers, so that <code>eq * factor =
     *                 Km</code>
     *
     * @return Internal coordinates ready to be fed in other scripting functions
     */
    double[] equatorial_cartesian_to_internal(double[] eq,
                                              double kmFactor);

    /**
     * Convert equatorial cartesian coordinates (in the internal reference system)
     * to galactic cartesian coordinates.
     *
     * @param eq Vector with [x, y, z] equatorial cartesian coordinates
     *
     * @return Vector with [x, y, z] galactic cartesian coordinates
     */
    double[] equatorial_to_galactic(double[] eq);

    /**
     * Convert equatorial cartesian coordinates (in the internal reference system)
     * to ecliptic cartesian coordinates.
     *
     * @param eqInternal Vector with [x, y, z] equatorial cartesian coordinates
     *
     * @return Vector with [x, y, z] ecliptic cartesian coordinates
     */
    double[] equatorial_to_ecliptic(double[] eqInternal);

    /**
     * Convert galactic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param galInternal Vector with [x, y, z] galactic cartesian coordinates
     *
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] galactic_to_equatorial(double[] galInternal);

    /**
     * Convert ecliptic cartesian coordinates (in the internal reference system)
     * to equatorial cartesian coordinates.
     *
     * @param eclInternal Vector with [x, y, z] ecliptic cartesian coordinates
     *
     * @return Vector with [x, y, z] equatorial cartesian coordinates
     */
    double[] ecliptic_to_equatorial(double[] eclInternal);
}
