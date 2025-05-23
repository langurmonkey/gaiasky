/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.api;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.math.Vector3D;
import gaiasky.util.ucd.UCD;
import uk.ac.bristol.star.cdf.Variable;

public interface IParticleRecord {

    double x();

    double y();

    double z();

    Vector3D pos(Vector3D aux);

    boolean hasProperMotion();

    float vx();

    float vy();

    float vz();

    String[] names();

    String namesConcat();

    boolean hasName(String candidate);

    boolean hasName(String candidate, boolean matchCase);

    float appMag();

    float absMag();

    boolean hasColor();

    float color();

    double[] rgb();

    boolean hasSize();

    float size();

    double radius();

    long id();

    int hip();

    float muAlpha();

    float muDelta();


    float radVel();

    /**
     * Distance in internal units. Beware, does the computation on the fly.
     *
     * @return The distance, in internal units.
     */
    double distance();

    /**
     * Parallax in mas.
     *
     * @return The parallax in mas.
     */
    double parallax();

    /**
     * Right ascension in degrees. Beware, does the conversion on the fly.
     *
     * @return The right ascension, in degrees.
     **/
    double ra();

    /**
     * Declination in degrees. Beware, does the conversion on the fly.
     *
     * @return The declination, in degrees.
     **/
    double dec();

    /**
     * Ecliptic longitude in degrees.
     *
     * @return The ecliptic longitude, in degrees.
     */
    double lambda();

    /**
     * Ecliptic latitude in degrees.
     *
     * @return The ecliptic latitude, in degrees.
     */
    double beta();

    /**
     * Galactic longitude in degrees.
     *
     * @return The galactic longitude, in degrees.
     */
    double l();

    /**
     * Galactic latitude in degrees.
     *
     * @return The galactic latitude, in degrees.
     */
    double b();

    /**
     * Returns the effective temperature, in K.
     *
     * @return The effective temperature in K.
     */
    float tEff();

    void setExtraAttributes(ObjectMap<UCD, Object> extra);

    boolean hasExtra();

    boolean hasExtra(String name);

    boolean hasExtra(UCD ucd);

    /**
     * Gets the extra attributes map.
     *
     * @return The map.
     */
    ObjectMap<UCD, Object> getExtra();

    /**
     * Gets the extra data filed with the given name.
     *
     * @param name The name of the data filed to get.
     *
     * @return The data field, or null if it does not exist.
     */
    Object getExtra(String name);

    /**
     * Gets the extra data filed with the given UCD.
     *
     * @param ucd The UCD of the data filed to get.
     *
     * @return The data field, or null if it does not exist.
     */
    Object getExtra(UCD ucd);

    /**
     * Gets the extra data filed with the given name, as a double number.
     *
     * @param name The name of the data filed to get.
     *
     * @return The data field as a double, or NaN if it does not exist or is not a number.
     */
    double getExtraNumber(String name);

    /**
     * Gets the extra data filed with the given UCD, as a double number.
     *
     * @param ucd The UCD of the data filed to get.
     *
     * @return The data field as a double, or NaN if it does not exist or is not a number.
     */
    double getExtraNumber(UCD ucd);

    ObjectMap.Keys<UCD> extraKeys();

    /**
     * Returns the particle record type.
     *
     * @return The type.
     */
    ParticleType getType();

    /**
     * Returns whether this particle record has a {@link Variable} attached, making it a variable star.
     *
     * @return True if this record is a variable star.
     */
    boolean isVariable();

    /**
     * @return The number of variable star samples
     */
    int nVari();

    /**
     * @return The variability period in days.
     */
    double period();

    /**
     * @return The vector with the variable star magnitudes.
     */
    float[] variMags();

    /**
     * @return The vector with the variable star times corresponding to the magnitudes.
     */
    double[] variTimes();
}
