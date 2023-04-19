/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.api;

import gaiasky.util.ObjectDoubleMap.Keys;
import gaiasky.util.math.Vector3d;
import gaiasky.util.tree.OctreeNode;
import gaiasky.util.ucd.UCD;

public interface IParticleRecord {

    double[] rawDoubleData();

    float[] rawFloatData();

    double x();

    double y();

    double z();

    void setPos(double x, double y, double z);

    Vector3d pos(Vector3d aux);

    double pmx();

    double pmy();

    double pmz();

    void setVelocityVector(double vx, double vy, double vz);

    String[] names();

    String namesConcat();

    boolean hasName(String candidate);

    boolean hasName(String candidate, boolean matchCase);

    void setNames(String... names);

    void setName(String name);

    void addName(String name);

    void addNames(String... names);

    float appmag();

    float absmag();

    void setMag(float appmag, float absmag);

    boolean hasCol();

    float col();

    void setCol(float col);

    double[] rgb();

    float size();

    void setSize(float size);

    double radius();

    void setId(long id);

    long id();

    void setHip(int hip);

    int hip();

    float mualpha();

    float mudelta();

    float radvel();

    void setProperMotion(float mualpha, float mudelta, float radvel);

    OctreeNode octant();

    void setOctant(OctreeNode octant);

    /**
     * Distance in internal units. Beware, does the computation on the fly.
     *
     * @return The distance, in internal units
     */
    double distance();

    /**
     * Parallax in mas.
     *
     * @return The parallax in mas.
     */
    double parallax();

    /**
     * Right ascension in degrees. Beware, does the conversion on the fly.
     *
     * @return The right ascension, in degrees
     **/
    double ra();

    /**
     * Declination in degrees. Beware, does the conversion on the fly.
     *
     * @return The declination, in degrees
     **/
    double dec();

    /**
     * Ecliptic longitude in degrees.
     *
     * @return The ecliptic longitude, in degrees
     */
    double lambda();

    /**
     * Ecliptic latitude in degrees.
     *
     * @return The ecliptic latitude, in degrees
     */
    double beta();

    /**
     * Galactic longitude in degrees.
     *
     * @return The galactic longitude, in degrees
     */
    double l();

    /**
     * Galactic latitude in degrees.
     *
     * @return The galactic latitude, in degrees
     */
    double b();

    boolean hasExtra();

    boolean hasExtra(String name);

    boolean hasExtra(UCD ucd);

    double getExtra(String name);

    double getExtra(UCD ucd);

    Keys<UCD> extraKeys();
}
