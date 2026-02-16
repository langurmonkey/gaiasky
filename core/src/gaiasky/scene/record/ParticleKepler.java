/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.math.Vector3D;
import gaiasky.util.ucd.UCD;

/**
 * Record class to store particles of all kinds.
 *
 * @param id             The particle identifier.
 * @param name           The name or designation.
 * @param epoch          The epoch in JD.
 * @param meanAnomaly    The mean anomaly, in degrees.
 * @param semiMajorAxis  The semi-major axis, in km.
 * @param eccentricity   The eccentricity.
 * @param argOfPericenter The argument of pericenter, in degrees.
 * @param ascendingNode  The ascending node, in degrees.
 * @param inclination    The inclination, in degrees.
 * @param period         The orbital period, in days.
 * @param extra          Map with extra attributes.
 */
public record ParticleKepler(long id,
                             String name,
                             double epoch,
                             double meanAnomaly,
                             double semiMajorAxis,
                             double eccentricity,
                             double argOfPericenter,
                             double ascendingNode,
                             double inclination,
                             double period,
                             ObjectMap<UCD, Object> extra) implements IParticleRecord {

    @Override
    public double x() {
        return Double.NaN;
    }

    @Override
    public double y() {
        return Double.NaN;
    }

    @Override
    public double z() {
        return Double.NaN;
    }

    @Override
    public ParticleType getType() {
        return ParticleType.KEPLER;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public int nVari() {
        return -1;
    }

    /**
     * Orbital period in days.
     * @return The orbital period, in days.
     */
    @Override
    public double period() {
        return period;
    }

    @Override
    public float[] variMags() {
        return null;
    }

    @Override
    public double[] variTimes() {
        return null;
    }

    @Override
    public Vector3D pos(Vector3D aux) {
        return aux;
    }

    @Override
    public boolean hasProperMotion() {
        return false;
    }

    @Override
    public float vx() {
        return Float.NaN;
    }

    @Override
    public float vy() {
        return Float.NaN;
    }

    @Override
    public float vz() {
        return Float.NaN;
    }

    @Override
    public String[] names() {
        return new String[]{name};
    }

    @Override
    public String namesConcat() {
        return name;
    }

    @Override
    public boolean hasName(String candidate) {
        return hasName(candidate,
                       false);
    }

    @Override
    public boolean hasName(String candidate,
                           boolean matchCase) {
        return matchCase ? candidate.equals(name) : candidate.equalsIgnoreCase(name);
    }

    /**
     * Distance in internal units. Beware, does the computation on the fly.
     *
     * @return The distance, in internal units
     */
    @Override
    public double distance() {
        return Double.NaN;
    }

    /**
     * Parallax in mas.
     *
     * @return The parallax in mas.
     */
    @Override
    public double parallax() {
        return Double.NaN;
    }

    /**
     * Declination in degrees. Beware, does the conversion on the fly.
     *
     * @return The declination, in degrees
     **/
    @Override
    public double ra() {
        return Double.NaN;
    }

    @Override
    public double dec() {
        return Double.NaN;
    }

    /**
     * Ecliptic longitude in degrees.
     *
     * @return The ecliptic longitude, in degrees
     */
    @Override
    public double lambda() {
        return Double.NaN;
    }

    /**
     * Ecliptic latitude in degrees.
     *
     * @return The ecliptic latitude, in degrees
     */
    @Override
    public double beta() {
        return Double.NaN;
    }

    /**
     * Galactic longitude in degrees.
     *
     * @return The galactic longitude, in degrees
     */
    @Override
    public double l() {
        return Double.NaN;
    }

    /**
     * Galactic latitude in degrees.
     *
     * @return The galactic latitude, in degrees
     */
    @Override
    public double b() {
        return Double.NaN;
    }

    @Override
    public void setExtraAttributes(ObjectMap<UCD, Object> e) {
        extra.clear();
        extra.putAll(e);
    }

    @Override
    public boolean hasExtra() {
        return extra != null;
    }

    @Override
    public boolean hasExtra(String name) {
        if (hasExtra()) {
            ObjectMap.Keys<UCD> ucds = extra.keys();
            for (UCD ucd : ucds) {
                if ((ucd.originalUCD != null && ucd.originalUCD.equals(name)) || (ucd.colName != null && ucd.colName.equals(name))) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public boolean hasExtra(UCD ucd) {
        return hasExtra() && extra.containsKey(ucd);
    }

    @Override
    public ObjectMap<UCD, Object> getExtra() {
        return extra;
    }

    @Override
    public Object getExtra(String name) {
        if (hasExtra()) {
            ObjectMap.Keys<UCD> ucds = extra.keys();
            for (UCD ucd : ucds) {
                if ((ucd.originalUCD != null && ucd.originalUCD.equals(name)) || (ucd.colName != null && ucd.colName.equals(name))) {
                    return extra.get(ucd);
                }
            }
        }
        return null;
    }


    @Override
    public Object getExtra(UCD ucd) {
        if (hasExtra(ucd)) {
            return extra.get(ucd);
        }
        return null;
    }

    @Override
    public double getExtraNumber(String name) {
        var value = getExtra(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public double getExtraNumber(UCD ucd) {
        var value = getExtra(ucd);
        if (value instanceof Number number) {
            return number.doubleValue();
        } else {
            return Double.NaN;
        }
    }

    @Override
    public ObjectMap.Keys<UCD> extraKeys() {
        return extra.keys();
    }

    /* UNUSED METHODS BELOW */

    @Override
    public float appMag() {
        return Float.NaN;
    }

    @Override
    public float absMag() {
        return Float.NaN;
    }

    @Override
    public boolean hasColor() {
        return false;
    }

    @Override
    public float color() {
        return Float.NaN;
    }

    @Override
    public double[] rgb() {
        return new double[0];
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public float size() {
        return Float.NaN;
    }

    @Override
    public double radius() {
        return Float.NaN;
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public int hip() {
        return -1;
    }

    @Override
    public float muAlpha() {
        return Float.NaN;
    }

    @Override
    public float muDelta() {
        return Float.NaN;
    }

    @Override
    public float radVel() {
        return Float.NaN;
    }

    @Override
    public float tEff() {
        return Float.NaN;
    }

}
