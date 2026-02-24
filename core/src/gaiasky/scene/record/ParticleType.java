/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import gaiasky.scene.component.ParticleSet;
import gaiasky.util.filter.attrib.*;

/**
 * Enumeration to identify the type of record.
 */
public enum ParticleType {
    /** Simple positional particles. Corresponds to {@link Particle}. **/
    PARTICLE(AttributeDistance.class,
             AttributeRA.class,
             AttributeDEC.class,
             AttributeEclLatitude.class,
             AttributeEclLongitude.class,
             AttributeGalLatitude.class,
             AttributeGalLongitude.class,
             AttributeX.class,
             AttributeY.class,
             AttributeZ.class,
             AttributeColorRed.class,
             AttributeColorGreen.class,
             AttributeColorBlue.class),
    /** Extended particles, with proper motions, colors and sizes. Corresponds to {@link ParticleExt}. **/
    PARTICLE_EXT(AttributeDistance.class,
                 AttributeRA.class,
                 AttributeDEC.class,
                 AttributeEclLatitude.class,
                 AttributeEclLongitude.class,
                 AttributeGalLatitude.class,
                 AttributeGalLongitude.class,
                 AttributeX.class,
                 AttributeY.class,
                 AttributeZ.class,
                 AttributeColorRed.class,
                 AttributeColorGreen.class,
                 AttributeColorBlue.class),
    /** Stars. Corresponds to {@link ParticleStar}. **/
    STAR(AttributeDistance.class,
         AttributeRA.class,
         AttributeDEC.class,
         AttributeEclLatitude.class,
         AttributeEclLongitude.class,
         AttributeGalLatitude.class,
         AttributeGalLongitude.class,
         AttributeAppmag.class,
         AttributeAbsmag.class,
         AttributeMualpha.class,
         AttributeMudelta.class,
         AttributeRadvel.class,
         AttributeX.class,
         AttributeY.class,
         AttributeZ.class,
         AttributeColorRed.class,
         AttributeColorGreen.class,
         AttributeColorBlue.class),
    /** Variable stars. Corresponds to {@link ParticleVariable}. **/
    VARIABLE(AttributeDistance.class,
             AttributeRA.class,
             AttributeDEC.class,
             AttributeEclLatitude.class,
             AttributeEclLongitude.class,
             AttributeGalLatitude.class,
             AttributeGalLongitude.class,
             AttributeAppmag.class,
             AttributeAbsmag.class,
             AttributeMualpha.class,
             AttributeMudelta.class,
             AttributeRadvel.class,
             AttributeX.class,
             AttributeY.class,
             AttributeZ.class,
             AttributeColorRed.class,
             AttributeColorGreen.class,
             AttributeColorBlue.class),
    /** Particle whose location is described as Keplerian orbital elements. **/
    KEPLER(AttributePeriod.class,
           AttributeSma.class,
           AttributeInclination.class,
           AttributeEccentricity.class,
           AttributeMeanAnomaly.class,
           AttributeAscNode.class,
           AttributeArgPeri.class,
           AttributeDistance.class,
           AttributeRA.class,
           AttributeDEC.class,
           AttributeEclLatitude.class,
           AttributeEclLongitude.class,
           AttributeGalLatitude.class,
           AttributeGalLongitude.class,
           AttributeX.class,
           AttributeY.class,
           AttributeZ.class),
    /** Particle implemented as a double array. Can hold anything. Corresponds to {@link ParticleVector}. **/
    VECTOR(AttributeDistance.class,
           AttributeX.class,
           AttributeY.class,
           AttributeZ.class);


    /** Attributes supported by the particle type. **/
    public final Class<? extends IAttribute>[] attributes;

    @SafeVarargs
    ParticleType(Class<? extends IAttribute>... attributes) {
        this.attributes = attributes;
    }

    public boolean isParticleOrStar() {
        return this == PARTICLE || this == PARTICLE_EXT || this == STAR || this == VARIABLE;
    }

    public boolean isKeplerElements() {
        return this == KEPLER;
    }

    public boolean isVector() {
        return this == VECTOR;
    }

    /**
     * Get the type from a particle set.
     *
     * @param set The set.
     *
     * @return The type.
     */
    public static ParticleType type(ParticleSet set) {
        var d = set.data();
        if (d != null && !d.isEmpty()) {
            return d.getFirst().getType();
        }
        return PARTICLE;
    }
}
