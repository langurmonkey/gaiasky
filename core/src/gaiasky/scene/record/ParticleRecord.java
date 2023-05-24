/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.NumberUtils;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.Constants;
import gaiasky.util.ObjectDoubleMap;
import gaiasky.util.ObjectDoubleMap.Keys;
import gaiasky.util.TLV3D;
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.OctreeNode;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;

public class ParticleRecord implements IParticleRecord {

    /**
     * Enumeration to identify the type of record.
     */
    public enum ParticleRecordType {
        PARTICLE(3, 0, new int[] { 0, 1, 2 }, new int[] {}),
        STAR(3, 11, new int[] { 0, 1, 2 }, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }),
        PARTICLE_EXT(3, 10, new int[] { 0, 1, 2 }, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });

        final int doubleArraySize, floatArraySize;
        final int[] doubleIndexIndireciton, floatIndexIndirection;

        ParticleRecordType(int doubleArraySize,
                           int floatArraySize,
                           int[] doubleIndexIndireciton,
                           int[] floatIndexIndirection) {
            this.doubleArraySize = doubleArraySize;
            this.floatArraySize = floatArraySize;
            this.doubleIndexIndireciton = doubleIndexIndireciton;
            this.floatIndexIndirection = floatIndexIndirection;
        }
    }

    public static final int STAR_SIZE_D = 3;
    public static final int STAR_SIZE_F = 11;
    /* INDICES */
    /* doubles */
    public static final int I_X = 0;
    public static final int I_Y = 1;
    public static final int I_Z = 2;
    /* floats (stars) */
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
    /* int */
    public static final int I_FHIP = 10;

    // Aux vectors.
    protected static TLV3D aux3d1 = new TLV3D();
    protected static TLV3D aux3d2 = new TLV3D();

    // Type.
    public final ParticleRecordType type;

    // Particle ID.
    public long id;

    // Double data array.
    public double[] dataD;

    // Float data array.
    public float[] dataF;

    // Particle names (optional).
    public String[] names;

    // Extra attributes (optional).
    public ObjectDoubleMap<UCD> extra;

    // Octant, if in octree.
    public OctreeNode octant;

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD) {
        this.type = type;
        this.dataD = dataD;
        this.dataF = null;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF) {
        this.type = type;
        this.dataD = dataD;
        this.dataF = dataF;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          Long id) {
        this(type, dataD, dataF);
        this.id = id != null ? id : -1;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          String[] names) {
        this(type, dataD, dataF);
        this.names = names;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          Long id,
                          String[] names) {
        this(type, dataD, dataF, id);
        this.names = names;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          Long id,
                          String[] names,
                          ObjectDoubleMap<UCD> extra) {
        this(type, dataD, dataF, id, names);
        this.names = names;
        this.extra = extra;
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          Long id,
                          String name) {
        this(type, dataD, dataF, id, name == null ? new String[] {} : new String[] { name });
    }

    public ParticleRecord(ParticleRecordType type,
                          double[] dataD,
                          float[] dataF,
                          Long id,
                          String name,
                          ObjectDoubleMap<UCD> extra) {
        this(type, dataD, dataF, id, name == null ? new String[] {} : new String[] { name }, extra);
    }

    @Override
    public double[] rawDoubleData() {
        return dataD;
    }

    @Override
    public float[] rawFloatData() {
        return dataF;
    }

    @Override
    public double x() {
        return dataD[type.doubleIndexIndireciton[I_X]];
    }

    @Override
    public double y() {
        return dataD[type.doubleIndexIndireciton[I_Y]];
    }

    @Override
    public double z() {
        return dataD[type.doubleIndexIndireciton[I_Z]];
    }

    @Override
    public void setPos(double x,
                       double y,
                       double z) {
        dataD[type.doubleIndexIndireciton[I_X]] = x;
        dataD[type.doubleIndexIndireciton[I_Y]] = y;
        dataD[type.doubleIndexIndireciton[I_Z]] = z;
    }

    @Override
    public double pmx() {
        return dataF[type.floatIndexIndirection[I_FPMX]];
    }

    @Override
    public double pmy() {
        return dataF[type.floatIndexIndirection[I_FPMY]];
    }

    @Override
    public double pmz() {
        return dataF[type.floatIndexIndirection[I_FPMZ]];
    }

    @Override
    public void setVelocityVector(double vx,
                                  double vy,
                                  double vz) {
        dataF[type.floatIndexIndirection[I_FPMX]] = (float) vx;
        dataF[type.floatIndexIndirection[I_FPMY]] = (float) vy;
        dataF[type.floatIndexIndirection[I_FPMZ]] = (float) vz;
    }

    @Override
    public float mualpha() {
        return dataF[type.floatIndexIndirection[I_FMUALPHA]];
    }

    @Override
    public float mudelta() {
        return dataF[type.floatIndexIndirection[I_FMUDELTA]];
    }

    @Override
    public float radvel() {
        return dataF[type.floatIndexIndirection[I_FRADVEL]];
    }

    @Override
    public void setProperMotion(float mualpha,
                                float mudelta,
                                float radvel) {
        dataF[type.floatIndexIndirection[I_FMUALPHA]] = mualpha;
        dataF[type.floatIndexIndirection[I_FMUDELTA]] = mudelta;
        dataF[type.floatIndexIndirection[I_FRADVEL]] = radvel;

    }

    @Override
    public float appmag() {
        return dataF[type.floatIndexIndirection[I_FAPPMAG]];
    }

    @Override
    public float absmag() {
        return dataF[type.floatIndexIndirection[I_FABSMAG]];
    }

    @Override
    public void setMag(float appmag,
                       float absmag) {
        dataF[type.floatIndexIndirection[I_FAPPMAG]] = appmag;
        dataF[type.floatIndexIndirection[I_FABSMAG]] = absmag;
    }

    @Override
    public boolean hasCol() {
        return dataF != null && dataF.length >= I_FCOL;
    }

    @Override
    public float col() {
        return dataF[type.floatIndexIndirection[I_FCOL]];
    }

    @Override
    public void setCol(float col) {
        dataF[type.floatIndexIndirection[I_FCOL]] = col;
    }

    @Override
    public float size() {
        return dataF[type.floatIndexIndirection[I_FSIZE]];
    }

    @Override
    public void setSize(float size) {
        dataF[type.floatIndexIndirection[I_FSIZE]] = size;
    }

    @Override
    public int hip() {
        return (int) dataF[type.floatIndexIndirection[I_FHIP]];
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void setHip(int hip) {
        dataF[type.floatIndexIndirection[I_FHIP]] = hip;
    }

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public String namesConcat() {
        return TextUtils.concatenate(Constants.nameSeparator, names);
    }

    @Override
    public boolean hasName(String candidate) {
        return hasName(candidate, false);
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
    public void setNames(String... names) {
        this.names = names;
    }

    @Override
    public void setName(String name) {
        if (names != null)
            names[0] = name;
        else
            names = new String[] { name };
    }

    @Override
    public void addName(String name) {
        name = name.strip();
        if (!hasName(name))
            if (names != null) {
                // Extend array
                String[] newNames = new String[names.length + 1];
                System.arraycopy(names, 0, newNames, 0, names.length);
                newNames[names.length] = name;
                names = newNames;
            } else {
                setName(name);
            }
    }

    @Override
    public void addNames(String... names) {
        for (String name : names)
            addName(name);
    }

    @Override
    public double radius() {
        return size() * Constants.STAR_SIZE_FACTOR;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public double[] rgb() {
        Color c = new Color(NumberUtils.floatToIntColor(dataF[type.floatIndexIndirection[I_FCOL]]));
        return new double[] { c.r, c.g, c.b };
    }

    @Override
    public OctreeNode octant() {
        return octant;
    }

    @Override
    public void setOctant(OctreeNode octant) {
        this.octant = octant;
    }

    @Override
    public Vector3d pos(Vector3d aux) {
        return aux.set(x(), y(), z());
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
     * Right ascension in degrees. Beware, does the conversion on the fly.
     *
     * @return The right ascension, in degrees
     **/
    @Override
    public double ra() {
        Vector3d cartPos = pos(aux3d1.get());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartPos, aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.x;
    }

    /**
     * Declination in degrees. Beware, does the conversion on the fly.
     *
     * @return The declination, in degrees
     **/
    @Override
    public double dec() {
        Vector3d cartPos = pos(aux3d1.get());
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartPos, aux3d2.get());
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
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos, aux3d2.get());
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
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos, aux3d2.get());
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
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos, aux3d2.get());
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
        Vector3d sphPos = Coordinates.cartesianToSpherical(cartEclPos, aux3d2.get());
        return MathUtilsDouble.radDeg * sphPos.y;
    }

    @Override
    public Keys<UCD> extraKeys() {
        return extra.keys();
    }

    @Override
    public boolean hasExtra() {
        return extra != null;
    }

    @Override
    public boolean hasExtra(String name) {
        if (extra != null) {
            Keys<UCD> ucds = extra.keys();
            for (UCD ucd : ucds) {
                if (ucd.originalucd.equals(name) || ucd.colname.equals(name)) {
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
    public double getExtra(String name) {
        if (extra != null) {
            Keys<UCD> ucds = extra.keys();
            for (UCD ucd : ucds) {
                if ((ucd.originalucd != null && ucd.originalucd.equals(name)) || (ucd.colname != null && ucd.colname.equals(name))) {
                    return extra.get(ucd, Double.NaN);
                }
            }
        }
        return Double.NaN;
    }

    @Override
    public double getExtra(UCD ucd) {
        return hasExtra(ucd) ? extra.get(ucd, Double.NaN) : Double.NaN;

    }

}
