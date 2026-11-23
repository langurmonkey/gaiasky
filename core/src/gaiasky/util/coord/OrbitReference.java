/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.coord;

import java.util.Locale;

/**
 * Reference plane and direction of the orbital elements of an {@link gaiasky.scene.record.OrbitComponent}.
 * Explicit modes use the ascending node on the J2000 equator, the convention of the JPL tables.
 */
public enum OrbitReference {
    /**
     * Keep the historical behavior: the {@code transformFunction}, else the parent's orientation,
     * else the equator. The default.
     */
    INHERITED,

    /**
     * The equatorial plane of the parent body, pole taken from its rotation model. The classical
     * convention for natural satellites; JPL labels it {@code EQ}.
     */
    PARENT_EQUATOR,

    /**
     * The orbital plane of the parent body about its own parent, its pole the normal of the parent's
     * position sampled twice.
     */
    PARENT_ORBITAL_PLANE,

    /**
     * The ecliptic plane of J2000, reference direction at the vernal equinox. It is what
     * {@code transformFunction: eclipticToEquatorial} produces; JPL labels it {@code EC}.
     */
    ECLIPTIC,

    /**
     * The Laplace plane, about which the orbital plane of the satellite precesses. Not derivable
     * from the parent: it needs an explicit pole, else it falls back to {@link #PARENT_EQUATOR}.
     */
    LAPLACE;

    /**
     * Parses a reference from its name, case-insensitively, or returns null if it is not recognized.
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
