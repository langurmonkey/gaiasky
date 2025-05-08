/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.TLV3D;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;

/**
 * Record class to store particles of all kinds.
 *
 * @param id    The particle identifier.
 * @param names The name array.
 * @param x     X component of position vector at epoch.
 * @param y     Y component of position vector at epoch.
 * @param z     Z component of position vector at epoch.
 * @param extra Map with extra attributes.
 */
public record Particle(long id,
                       String[] names,
                       double x,
                       double y,
                       double z,
                       ObjectMap<UCD, Object> extra) implements IParticleRecord {

    // Aux vectors.
    private static final TLV3D aux3d1 = new TLV3D();
    private static final TLV3D aux3d2 = new TLV3D();

    /**
     * Constructor for particles or stars. Pass in the lists directly.
     */
    public Particle(long id,
                    String[] names,
                    double x, double y, double z) {
        this(id,
             names,
             x, y, z, null);
    }


    @Override
    public ParticleType getType() {
        return ParticleType.PARTICLE;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public int nVari() {
        return -1;
    }

    @Override
    public double period() {
        return -1;
    }

    @Override
    public float[] variMags() {
        return new float[0];
    }

    @Override
    public double[] variTimes() {
        return new double[0];
    }

    @Override
    public Vector3D pos(Vector3D aux) {
        return aux.set(x(),
                       y(),
                       z());
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
        return names;
    }

    @Override
    public String namesConcat() {
        return TextUtils.concatenate(Constants.nameSeparator,
                                     names);
    }

    @Override
    public boolean hasName(String candidate) {
        return hasName(candidate,
                       false);
    }

    @Override
    public boolean hasName(String candidate,
                           boolean matchCase) {
        if (names == null) {
            return false;
        } else {
            for (String name : names) {
                if (matchCase) {
                    if (name.equals(candidate))
                        return true;
                } else {
                    if (name.equalsIgnoreCase(candidate))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Distance in internal units. Beware, does the computation on the fly.
     *
     * @return The distance, in internal units
     */
    @Override
    public double distance() {
        return FastMath.sqrt(x() * x() + y() * y() + z() * z());
    }

    /**
     * Parallax in mas.
     *
     * @return The parallax in mas.
     */
    @Override
    public double parallax() {
        return 1000d / (distance() * Constants.U_TO_PC);
    }

    /**
     * Declination in degrees. Beware, does the conversion on the fly.
     *
     * @return The declination, in degrees
     **/
    @Override
    public double ra() {
        Vector3D cartPos = pos(aux3d1.get());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    @Override
    public double dec() {
        Vector3D cartPos = pos(aux3d1.get());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    /**
     * Ecliptic longitude in degrees.
     *
     * @return The ecliptic longitude, in degrees
     */
    @Override
    public double lambda() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * Ecliptic latitude in degrees.
     *
     * @return The ecliptic latitude, in degrees
     */
    @Override
    public double beta() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    /**
     * Galactic longitude in degrees.
     *
     * @return The galactic longitude, in degrees
     */
    @Override
    public double l() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * Galactic latitude in degrees.
     *
     * @return The galactic latitude, in degrees
     */
    @Override
    public double b() {
        Vector3D cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3D sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
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
        if (extra != null) {
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
        return extra != null && extra.containsKey(ucd);
    }

    @Override
    public ObjectMap<UCD, Object> getExtra() {
        return extra;
    }

    @Override
    public Object getExtra(String name) {
        if (extra != null) {
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
