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
import gaiasky.util.TextUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3D;
import gaiasky.util.ucd.UCD;

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
        return IParticleRecord.getExtraAttribute(name, extra);
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
        return 0;
    }

    @Override
    public float absMag() {
        return 0;
    }

    @Override
    public boolean hasColor() {
        return false;
    }

    @Override
    public float color() {
        return 0;
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
        return 0;
    }

    @Override
    public double radius() {
        return 0;
    }

    @Override
    public long id() {
        return id;
    }

}
