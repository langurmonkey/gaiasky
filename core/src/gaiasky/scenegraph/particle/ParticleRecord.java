package gaiasky.scenegraph.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.NumberUtils;
import gaiasky.util.*;
import gaiasky.util.ObjectDoubleMap.Keys;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsd;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.OctreeNode;
import gaiasky.util.ucd.UCD;
import net.jafama.FastMath;

public class ParticleRecord implements IParticleRecord {
    protected static TLV3D aux3d1 = new TLV3D(), aux3d2 = new TLV3D(), aux3d3 = new TLV3D();
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

    // Particle ID
    public long id;

    // Double data array
    public double[] dataD;

    // Float data array
    public float[] dataF;

    // Particle names (optional)
    public String[] names;

    // Extra attributes (optional)
    public ObjectDoubleMap<UCD> extra;

    // Octant, if in octree
    public OctreeNode octant;

    public ParticleRecord(double[] dataD) {
        this.dataD = dataD;
        this.dataF = null;
    }

    public ParticleRecord(double[] dataD, float[] dataF) {
        this.dataD = dataD;
        this.dataF = dataF;
    }

    public ParticleRecord(double[] dataD, float[] dataF, Long id) {
        this(dataD, dataF);
        this.id = id != null ? id : -1;
    }

    public ParticleRecord(double[] dataD, float[] dataF, String[] names) {
        this(dataD, dataF);
        this.names = names;
    }

    public ParticleRecord(double[] dataD, float[] dataF, Long id, String[] names) {
        this(dataD, dataF, id);
        this.names = names;
    }

    public ParticleRecord(double[] dataD, float[] dataF, Long id, String[] names, ObjectDoubleMap<UCD> extra) {
        this(dataD, dataF, id, names);
        this.names = names;
        this.extra = extra;
    }

    public ParticleRecord(double[] dataD, float[] dataF, Long id, String name) {
        this(dataD, dataF, id, name == null ? new String[] {} : new String[] { name });
    }

    public ParticleRecord(double[] dataD, float[] dataF, Long id, String name, ObjectDoubleMap<UCD> extra) {
        this(dataD, dataF, id, name == null ? new String[] {} : new String[] { name }, extra);
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
        return dataD[I_X];
    }

    @Override
    public double y() {
        return dataD[I_Y];
    }

    @Override
    public double z() {
        return dataD[I_Z];
    }

    @Override
    public void setPos(double x, double y, double z) {
        dataD[I_X] = x;
        dataD[I_Y] = y;
        dataD[I_Z] = z;
    }

    @Override
    public double pmx() {
        return dataF[I_FPMX];
    }

    @Override
    public double pmy() {
        return dataF[I_FPMY];
    }

    @Override
    public double pmz() {
        return dataF[I_FPMZ];
    }

    @Override
    public void setVelocityVector(double vx, double vy, double vz) {
        dataF[I_FPMX] = (float) vx;
        dataF[I_FPMY] = (float) vy;
        dataF[I_FPMZ] = (float) vz;
    }

    @Override
    public float mualpha() {
        return dataF[I_FMUALPHA];
    }

    @Override
    public float mudelta() {
        return dataF[I_FMUDELTA];
    }

    @Override
    public float radvel() {
        return dataF[I_FRADVEL];
    }

    @Override
    public void setProperMotion(float mualpha, float mudelta, float radvel) {
        dataF[I_FMUALPHA] = mualpha;
        dataF[I_FMUDELTA] = mudelta;
        dataF[I_FRADVEL] = radvel;

    }

    @Override
    public float appmag() {
        return dataF[I_FAPPMAG];
    }

    @Override
    public float absmag() {
        return dataF[I_FABSMAG];
    }

    @Override
    public void setMag(float appmag, float absmag) {
        dataF[I_FAPPMAG] = appmag;
        dataF[I_FABSMAG] = absmag;
    }

    @Override
    public boolean hasCol() {
        return dataF != null && dataF.length >= I_FCOL;
    }

    @Override
    public float col() {
        return dataF[I_FCOL];
    }

    @Override
    public void setCol(float col) {
        dataF[I_FCOL] = col;
    }

    @Override
    public float size() {
        return dataF[I_FSIZE];
    }

    @Override
    public void setSize(float size) {
        dataF[I_FSIZE] = size;
    }

    @Override
    public int hip() {
        return (int) dataF[I_FHIP];
    }

    @Override
    public long id() {
        return id;
    }

    @Override
    public void setHip(int hip) {
        dataF[I_FHIP] = hip;
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
    public boolean hasName(String candidate, boolean matchCase) {
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
        Color c = new Color(NumberUtils.floatToIntColor(dataF[I_FCOL]));
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
        return MathUtilsd.radDeg * sphPos.x;
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
        return MathUtilsd.radDeg * sphPos.y;
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
        return MathUtilsd.radDeg * sphPos.x;
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
        return MathUtilsd.radDeg * sphPos.y;
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
        return MathUtilsd.radDeg * sphPos.x;
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
        return MathUtilsd.radDeg * sphPos.y;
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
