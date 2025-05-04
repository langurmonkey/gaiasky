/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.NumberUtils;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.TLV3D;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;


/**
 * Record class to store extended particles. These are particles with proper motions, magnitudes, colors and sizes.
 *
 * @param id        The particle identifier.
 * @param names     The name array.
 * @param x         X component of position vector at epoch.
 * @param y         Y component of position vector at epoch.
 * @param z         Z component of position vector at epoch.
 * @param muAlpha16 The proper motion in alpha*, in mas/y.
 * @param muDelta16 The proper motion in delta, in mas/y.
 * @param radVel16  The radial velocity, in km/s.
 * @param vx        X component of the velocity vector.
 * @param vy        Y component of the velocity vector.
 * @param vz        Z component of the velocity vector.
 * @param appMag16  Apparent magnitude.
 * @param absMag16  Absolute magnitude.
 * @param color     Packed color.
 * @param size      Size.
 * @param extra     Map with extra attributes.
 */
public record ParticleExt(long id,
                          String[] names,
                          double x,
                          double y,
                          double z,
                          short muAlpha16,
                          short muDelta16,
                          short radVel16,
                          float vx,
                          float vy,
                          float vz,
                          short appMag16,
                          short absMag16,
                          float color,
                          float size,
                          ObjectMap<UCD, Object> extra) implements IParticleRecord {

    // Aux vectors.
    private static final TLV3D aux3d1 = new TLV3D();
    private static final TLV3D aux3d2 = new TLV3D();

    public ParticleExt(long id,
                       String[] names,
                       double x,
                       double y,
                       double z,
                       float muAlpha,
                       float muDelta,
                       float radVel,
                       float vx,
                       float vy,
                       float vz,
                       float appMag16,
                       float absMag16,
                       float color,
                       float size,
                       ObjectMap<UCD, Object> extra) {
        this(id, names, x, y, z, Float.floatToFloat16(muAlpha), Float.floatToFloat16(muDelta), Float.floatToFloat16(radVel),
             vx, vy, vz, Float.floatToFloat16(appMag16), Float.floatToFloat16(absMag16), color, size, extra);
    }


    @Override
    public ParticleType getType() {
        return ParticleType.PARTICLE_EXT;
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
    public Vector3d pos(Vector3d aux) {
        return aux.set(x(),
                       y(),
                       z());
    }

    @Override
    public boolean hasProperMotion() {
        return true;
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

    @Override
    public float appMag() {
        return Float.float16ToFloat(appMag16);
    }

    @Override
    public float absMag() {
        return Float.float16ToFloat(absMag16);
    }

    @Override
    public boolean hasColor() {
        return true;
    }

    @Override
    public double[] rgb() {
        Color c = new Color(NumberUtils.floatToIntColor(color));
        return new double[]{c.r, c.g, c.b};
    }

    @Override
    public boolean hasSize() {
        return true;
    }

    @Override
    public double radius() {
        return size() * Constants.STAR_SIZE_FACTOR;
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
        return Float.float16ToFloat(muAlpha16);
    }

    @Override
    public float muDelta() {
        return Float.float16ToFloat(muDelta16);
    }

    @Override
    public float radVel() {
        return Float.float16ToFloat(radVel16);
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
        Vector3d cartPos = pos(aux3d1.get());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    @Override
    public double dec() {
        Vector3d cartPos = pos(aux3d1.get());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartPos,
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
        Vector3d cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos,
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
        Vector3d cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToEcl());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos,
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
        Vector3d cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos,
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
        Vector3d cartEclPos = pos(aux3d1.get()).mul(Coordinates.eqToGal());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos,
                                                           aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    @Override
    public float tEff() {
        return -1;
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
                if ((ucd.originalUCD != null && ucd.originalUCD.equals(name)) || (ucd.colName != null && ucd.colName.equals(
                        name))) {
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
                if ((ucd.originalUCD != null && ucd.originalUCD.equals(name)) || (ucd.colName != null && ucd.colName.equals(
                        name))) {
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
}
