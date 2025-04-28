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
import gaiasky.util.math.Vector3d;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;

/**
 * Record class to store particles of all kinds.
 * @param id The particle identifier.
 * @param names The name array.
 * @param d Double-precision floating point values. See indices starting at {@link Particle#I_X} for contents.
 * @param f Single-precision floating point values. See indices starting at {@link Particle#I_FPMX} for contents.
 * @param extra Map with extra attributes.
 * @param variable Variable stars data.
 */
public record Particle(long id,
                       String[] names,
                       double[] d,
                       float[] f,
                       ObjectMap<UCD, Object> extra,
                       Variable variable) implements IParticleRecord {

    // Aux vectors.
    private static final TLV3D aux3d1 = new TLV3D();
    private static final TLV3D aux3d2 = new TLV3D();

    /* Double data type indices in double indirection table. */
    public static final int I_X = 0;
    public static final int I_Y = 1;
    public static final int I_Z = 2;
    /* Float data type indices in float indirection table. */
    public static final int I_FPMX = 0;
    public static final int I_FPMY = 1;
    public static final int I_FPMZ = 2;
    public static final int I_FMUALPHA = 3;
    public static final int I_FMUDELTA = 4;
    public static final int I_FRADVEL = 5;
    public static final int I_FAPPMAG = 6;
    public static final int I_FABSMAG = 7;
    public static final int I_FCOL = 8;
    public static final int I_FSIZE = 9;
    /* HIP number is still in float array. */
    public static final int I_FHIP = 10;
    public static final int I_FTEFF = 11;

    /**
     * Constructor for particles or stars. Pass in the lists directly.
     */
    public Particle(long id,
                    String[] names,
                    double[] d,
                    float[] f) {
        this(id,
             names,
             d,
             f,
             null,
             null);
    }

    /** Constructor for base particles {@link ParticleType#PARTICLE}. **/
    public Particle(long id,
                    String[] names,
                    double x,
                    double y,
                    double z) {
        this(id,
             names,
             new double[]{x, y, z},
             null,
             null,
             null);
    }

    /** Constructor for base particles {@link ParticleType#PARTICLE}, with extra attributes. **/
    public Particle(long id,
                    String[] names,
                    double x,
                    double y,
                    double z,
                    ObjectMap<UCD, Object> extra) {
        this(id,
             names,
             new double[]{x, y, z},
             null,
             extra,
             null);
    }

    /** Constructor for extended particles, {@link ParticleType#PARTICLE_EXT}. **/
    public Particle(long id,
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
                    float appMag,
                    float absMag,
                    float color,
                    float size) {
        this(id,
             names,
             new double[]{x, y, z},
             new float[]{vx, vy, vz, muAlpha, muDelta, radVel, appMag, absMag, color, size},
             null,
             null);
    }

    /** Constructor for extended particles, {@link ParticleType#PARTICLE_EXT}, with extra attributes. **/
    public Particle(long id,
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
                    float appMag,
                    float absMag,
                    float color,
                    float size,
                    ObjectMap<UCD, Object> extra) {
        this(id,
             names,
             new double[]{x, y, z},
             new float[]{vx, vy, vz, muAlpha, muDelta, radVel, appMag, absMag, color, size},
             extra,
             null);
    }

    /** Constructor for stars, {@link ParticleType#STAR}. **/
    public Particle(long id,
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
                    float appMag,
                    float absMag,
                    float color,
                    float size,
                    int hip,
                    float tEff,
                    ObjectMap<UCD, Object> extra) {
        this(id,
             names,
             new double[]{x, y, z},
             new float[]{vx, vy, vz, muAlpha, muDelta, radVel, appMag, absMag, color, size, (float) hip, tEff},
             extra,
             null);
    }

    /** Constructor for variable stars, {@link ParticleType#STAR}, with an attached {@link Variable}. **/
    public Particle(long id,
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
                    float appMag,
                    float absMag,
                    float color,
                    float size,
                    int hip,
                    float tEff,
                    ObjectMap<UCD, Object> extra,
                    Variable vari) {
        this(id,
             names,
             new double[]{x, y, z},
             new float[]{vx, vy, vz, muAlpha, muDelta, radVel, appMag, absMag, color, size, (float) hip, tEff},
             extra,
             vari);
    }

    @Override
    public ParticleType getType() {
        if (f == null) {
            return ParticleType.PARTICLE;
        } else if (f.length == 10) {
            return ParticleType.PARTICLE_EXT;
        } else if (f.length == 12) {
            return ParticleType.STAR;
        } else {
            return ParticleType.FAKE;
        }
    }

    @Override
    public boolean isVariable() {
        return variable != null;
    }

    public double x() {
        return d[I_X];
    }

    public double y() {
        return d[I_Y];
    }

    public double z() {
        return d[I_Z];
    }

    @Override
    public double[] rawDoubleData() {
        return d;
    }

    @Override
    public float[] rawFloatData() {
        return f;
    }

    @Override
    public void setPos(double x,
                       double y,
                       double z) {
        d[I_X] = x;
        d[I_Y] = y;
        d[I_Z] = z;
    }

    @Override
    public Vector3d pos(Vector3d aux) {
        return aux.set(x(),
                       y(),
                       z());
    }

    @Override
    public boolean hasProperMotion() {
        return f != null && f.length > I_FPMZ;
    }

    @Override
    public double pmx() {
        return f[I_FPMX];
    }

    @Override
    public double pmy() {
        return f[I_FPMY];
    }

    @Override
    public double pmz() {
        return f[I_FPMZ];
    }

    @Override
    public void setVelocityVector(double vx,
                                  double vy,
                                  double vz) {
        f[I_FPMX] = (float) vx;
        f[I_FPMY] = (float) vx;
        f[I_FPMZ] = (float) vx;
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
        return f[I_FAPPMAG];
    }

    @Override
    public float absMag() {
        return f[I_FABSMAG];
    }

    @Override
    public boolean hasColor() {
        return f != null && f.length > I_FCOL;
    }

    @Override
    public float col() {
        return f[I_FCOL];
    }

    @Override
    public void setCol(float col) {
        f[I_FCOL] = col;
    }

    @Override
    public double[] rgb() {
        return new double[0];
    }

    @Override
    public boolean hasSize() {
        return f != null && f.length > I_FSIZE;
    }

    @Override
    public float size() {
        return f[I_FSIZE];
    }

    @Override
    public void setSize(float size) {
        f[I_FSIZE] = size;
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
    public void setHip(int hip) {
        f[I_FHIP] = hip;
    }

    @Override
    public int hip() {
        return (int) f[I_FHIP];
    }

    @Override
    public float mualpha() {
        return f[I_FMUALPHA];
    }

    @Override
    public float mudelta() {
        return f[I_FMUDELTA];
    }

    @Override
    public float radvel() {
        return f[I_FRADVEL];
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
    public void setTeff(float tEff) {
        f[I_FTEFF] = tEff;
    }

    @Override
    public float teff() {
        return f[I_FTEFF];
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
}
