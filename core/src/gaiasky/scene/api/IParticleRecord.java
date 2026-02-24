/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.api;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.record.ParticleType;
import gaiasky.util.Constants;
import gaiasky.util.TLV3D;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.ucd.UCD;
import uk.ac.bristol.star.cdf.Variable;

public interface IParticleRecord {

    // Aux vectors.
    TLV3D aux3d1 = new TLV3D();
    TLV3D aux3d2 = new TLV3D();

    double x();

    double y();

    double z();

    default Vector3D pos(Vector3D aux) {
        return aux.set(x(), y(), z());
    }

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

    default int hip() {
        return -1;
    }

    default float muAlpha() {
        return 0;
    }

    default float muDelta() {
        return 0;
    }

    default float radVel() {
        return 0;
    }

    /**
     * @return The distance, in internal units.
     */
    default double distance() {
        return aux3d1.get().set(x(), y(), z()).len();
    }

    /**
     * @return The parallax in mas.
     */
    default double parallax() {
        return 1000d / (distance() * Constants.U_TO_PC);
    }

    /**
     * @return The right ascension, in degrees.
     **/
    default double ra() {
        Vector3D cartPos = pos(aux3d1.get());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * @return The declination, in degrees.
     **/
    default double dec() {
        Vector3D cartPos = pos(aux3d1.get());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    /**
     * @return The ecliptic longitude, in degrees
     */
    default double lambda() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * @return The ecliptic latitude, in degrees
     */
    default double beta() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    /**
     * @return The galactic longitude, in degrees
     */
    default double l() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * @return The galactic latitude, in degrees
     */
    default double b() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    /**
     * @return The effective temperature in K.
     */
    default float tEff() {
        return -1;
    }

    default double epoch() {
        return 0;
    }

    default double semiMajorAxis() {
        return 0;
    }

    default double argOfPericenter() {
        return 0;
    }

    default double meanAnomaly() {
        return 0;
    }

    default double eccentricity() {
        return 0;
    }

    default double inclination() {
        return 0;
    }

    default double ascendingNode() {
        return 0;
    }


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
    default boolean isVariable() {
        return false;
    }

    /**
     * @return The number of variable star samples
     */
    default int nVari() {
        return -1;
    }

    /**
     * @return The variability period, or the Keplerian period, in days.
     */
    default double period() {
        return -1;
    }

    /**
     * @return The vector with the variable star magnitudes.
     */
    default float[] variMags() {
        return null;
    }

    /**
     * @return The vector with the variable star times corresponding to the magnitudes.
     */
    default double[] variTimes() {
        return null;
    }

    /**
     * Gets the attribute with the given name from the given extra map.
     *
     * @param name  The name.
     * @param extra The object map.
     *
     * @return The attribute value.
     */
    static Object getExtraAttribute(String name, ObjectMap<UCD, Object> extra) {
        if (extra != null) {
            ObjectMap.Keys<UCD> ucds = extra.keys();
            for (UCD ucd : ucds) {
                if ((ucd.originalUCD != null && ucd.originalUCD.equals(name)) || (ucd.colName != null && ucd.colName.equals(
                        name))) {
                    return extra.get(ucd);
                }
            }
        }
        return null;
    }

}
