/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
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
 * A particle record that only contains positions, stored in a double array.
 *
 * @param data The data array containing the position.
 */
public record ParticleVector(double[] data) implements IParticleRecord {

    public void setPos(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    @Override
    public double x() {
        return data[0];
    }

    public void setX(double x) {
        data[0] = x;
    }

    @Override
    public double y() {
        return data[1];
    }

    public void setY(double y) {
        data[1] = y;
    }

    @Override
    public double z() {
        return data[2];
    }

    public void setZ(double z) {
        data[2] = z;
    }

    @Override
    public String[] names() {
        return null;
    }

    @Override
    public String namesConcat() {
        return null;
    }

    @Override
    public boolean hasName(String candidate) {
        return false;
    }

    @Override
    public boolean hasName(String candidate, boolean matchCase) {
        return false;
    }

    @Override
    public float vx() {
        return 0;
    }

    @Override
    public float vy() {
        return 0;
    }

    @Override
    public float vz() {
        return 0;
    }

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
        return data.length >= 7;
    }

    @Override
    public float color() {
        return 0;
    }

    @Override
    public float size() {
        return data.length >= 4 ? (float) data[3] : 0;
    }

    public void setSize(double size) {
        if (data.length >= 4) {
            data[3] = size;
        }
    }

    @Override
    public double radius() {
        return 0;
    }

    @Override
    public long id() {
        return 0;
    }

    @Override
    public int hip() {
        return 0;
    }

    @Override
    public double[] rgb() {
        return data.length >= 7 ? new double[]{data[4], data[5], data[6]} : null;
    }

    public void setRGB(double r, double g, double b) {
        if (data.length >= 7) {
            data[4] = r;
            data[5] = g;
            data[6] = b;
        }
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public Vector3D pos(Vector3D aux) {
        aux.x = data[0];
        aux.y = data[1];
        aux.z = data[2];
        return aux;
    }

    @Override
    public boolean hasProperMotion() {
        return false;
    }

    @Override
    public float tEff() {
        return 0;
    }

    @Override
    public void setExtraAttributes(ObjectMap<UCD, Object> extra) {

    }

    @Override
    public boolean hasExtra() {
        return false;
    }

    @Override
    public ObjectMap.Keys<UCD> extraKeys() {
        return null;
    }

    @Override
    public ParticleType getType() {
        return ParticleType.VECTOR;
    }

    @Override
    public boolean hasExtra(String name) {
        return false;
    }

    @Override
    public boolean hasExtra(UCD ucd) {
        return false;
    }

    @Override
    public ObjectMap<UCD, Object> getExtra() {
        return null;
    }

    @Override
    public Object getExtra(String name) {
        return null;
    }

    @Override
    public Object getExtra(UCD ucd) {
        return null;
    }

    @Override
    public double getExtraNumber(String name) {
        return 0;
    }

    @Override
    public double getExtraNumber(UCD ucd) {
        return 0;
    }
}
