/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.math.Vector3d;
import gaiasky.util.ucd.UCD;

public record ParticleVector(double[] data) implements IParticleRecord {

    @Override
    public double x() {
        return data[0];
    }

    @Override
    public double y() {
        return data[1];
    }

    @Override
    public double z() {
        return data[2];
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
    public float muAlpha() {
        return 0;
    }

    @Override
    public float muDelta() {
        return 0;
    }

    @Override
    public float radVel() {
        return 0;
    }

    @Override
    public double[] rgb() {
        return data.length >= 7 ? new double[]{data[4], data[5], data[6]} : null;
    }

    @Override
    public boolean hasSize() {
        return false;
    }

    @Override
    public Vector3d pos(Vector3d aux) {
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
    public double distance() {
        return 0;
    }

    @Override
    public double parallax() {
        return 0;
    }

    @Override
    public double ra() {
        return 0;
    }

    @Override
    public double dec() {
        return 0;
    }

    @Override
    public double lambda() {
        return 0;
    }

    @Override
    public double beta() {
        return 0;
    }

    @Override
    public double l() {
        return 0;
    }

    @Override
    public double b() {
        return 0;
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
        return ParticleType.FAKE;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public int nVari() {
        return 0;
    }

    @Override
    public double period() {
        return 0;
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
