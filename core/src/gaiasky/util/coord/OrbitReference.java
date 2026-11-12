/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import java.util.Locale;

/**
 * Reference plane and reference direction in which the orbital elements of an
 * {@link gaiasky.scene.record.OrbitComponent} are expressed.
 * <p>
 * The classical elements are meaningless without them: the inclination is the angle between the
 * orbital plane and an unnamed plane, and the longitude of the ascending node is measured from an
 * unnamed direction within it. Historically this was left implicit and resolved from whatever the
 * orbit entity happened to declare - a {@code transformFunction}, else the parent's orientation -
 * which is what {@link #INHERITED} still does, and is the default.
 * <p>
 * In every explicit mode the reference direction is the ascending node of the reference plane on the
 * equatorial plane of J2000, which is the convention JPL uses for its satellite element tables. That
 * makes their inclination and longitude of the ascending node usable verbatim, provided the pole of
 * the plane is the one they used.
 */
public enum OrbitReference {
    /**
     * Keep the historical behavior: use the {@code transformFunction} of the trajectory if there is
     * one, else the orientation of the parent body if it has one, else the equatorial plane. This is
     * the default, and the only mode that is bit-for-bit identical to the behavior before the
     * property existed.
     */
    INHERITED,

    /**
     * The equatorial plane of the parent body, with the reference direction at the ascending node of
     * that equator on the equatorial plane of J2000. The pole is taken from the parent's rotation
     * model, so the orbits are flattened onto the same pole the parent is drawn with.
     * <p>
     * This is the classical convention for natural satellites. JPL labels it {@code EQ}.
     */
    PARENT_EQUATOR,

    /**
     * The orbital plane of the parent body around its own parent, with the reference direction at the
     * ascending node of that plane on the equatorial plane of J2000. The pole is the normal of the
     * parent's orbit, sampled from the parent's own coordinates.
     * <p>
     * This is the plane in which the orbit of a planet lies about its star, which for a planet of the
     * solar system is very nearly the ecliptic - the two differ by the planet's own inclination (from
     * 0.8 degrees for Uranus to 7 degrees for Mercury).
     */
    PARENT_ORBITAL_PLANE,

    /**
     * The ecliptic plane of J2000, with the reference direction at the vernal equinox.
     * <p>
     * This is what {@code transformFunction: eclipticToEquatorial} produces. JPL labels it {@code EC}
     * and uses it for the irregular satellites, whose orbital planes are set by the capture geometry
     * rather than by the equator of the planet.
     */
    ECLIPTIC,

    /**
     * The Laplace plane: the plane about which the orbital plane of the satellite precesses, whose
     * normal is the satellite's orbital precession pole. For a close satellite it coincides with the
     * equator of the planet and for a distant one with the orbital plane of the planet; in between it
     * is a gradual interpolation, and for Iapetus it is 15 degrees away from both.
     * <p>
     * It is not derivable from the parent, so it must be given explicitly in
     * {@link gaiasky.scene.record.OrbitComponent#referencePole} as the right ascension and declination
     * of the pole. Without a pole it falls back to {@link #PARENT_EQUATOR} and logs a warning.
     * <p>
     * JPL labels it {@code LP}.
     */
    LAPLACE;

    /**
     * Parses a reference from its name, case-insensitively, tolerating the singular and plural forms
     * of {@link #INHERITED}. Returns {@code null} if the name is not recognized, so that the caller
     * can report it with the attribute that carried it.
     *
     * @param name The name of the reference.
     *
     * @return The reference, or null if unknown.
     */
    public static OrbitReference fromString(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "INHERIT", "INHERITED", "IH" -> INHERITED;
            case "PARENT_EQUATOR", "PARENTEQUATOR", "PARENT-EQUATOR", "EQ" -> PARENT_EQUATOR;
            case "PARENT_ORBITAL_PLANE", "PARENTORBITALPLANE", "PARENT-ORBITAL-PLANE", "POP" -> PARENT_ORBITAL_PLANE;
            case "ECLIPTIC", "EC" -> ECLIPTIC;
            case "LAPLACE", "LP" -> LAPLACE;
            default -> null;
        };
    }
}
