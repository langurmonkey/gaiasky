/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

import gaiasky.util.coord.Coordinates;
import gaiasky.util.math.Vector3d;
import net.jafama.FastMath;

import java.util.Random;

public class Place {

    protected Vector3d dirICRS = null;
    protected boolean haveAngles = false;
    protected double alpha;
    protected double delta;

    /**
     * Default constructor. Puts the Place at (alpha, delta ) = (0, 0)
     */
    public Place() {
        dirICRS = new Vector3d(1.0, 0.0, 0.0);
        haveAngles = false;
    }

    /**
     * Constructs a Place at the position given by a vector (need not be a unit
     * vector)
     *
     * @param r vector
     */
    public Place(Vector3d r) {
        dirICRS = r.cpy().nor();
        haveAngles = false;
    }

    /**
     * Creates a new place at the given (alpha, delta)
     */
    public Place(double rightAscension, double declination) {
        dirICRS = new Vector3d();
        Coordinates.sphericalToCartesian(rightAscension, declination, 1, dirICRS);
        alpha = rightAscension;
        delta = declination;
        haveAngles = true;
    }

    /**
     * Constructs a Place at a random position
     *
     * @param rnd Random number generator
     */
    public Place(Random rnd) {
        dirICRS = new Vector3d(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).nor();
        haveAngles = false;
    }

    /**
     * Duplicates a Place
     */
    public Place(Place p) {
        dirICRS = p.getDirection();
        haveAngles = false;
    }

    /**
     * Returns the unit vector of the Place
     *
     * @return vector
     */
    public Vector3d getDirection() {
        return dirICRS.cpy();
    }

    /**
     * Sets the position of the Place to that of a given vector (need not be a
     * unit vector)
     *
     * @param r vector
     */
    public Place setDirection(Vector3d r) {
        dirICRS = r.cpy().nor();
        haveAngles = false;
        return this;
    }

    /**
     * Returns the right ascension [rad] of the Place
     *
     * @return The right ascension in radians
     */
    public double getAlpha() {
        if (!haveAngles) {
            calcAngles();
        }

        return this.alpha;
    }

    /**
     * Returns the declination [rad] of the Place
     *
     * @return The declination in radians
     */
    public double getDelta() {
        if (!haveAngles) {
            calcAngles();
        }

        return this.delta;
    }

    /**
     * Sets the position of the Place to given (alpha, delta)
     */
    public Place setAngles(double rightAscension, double declination) {
        Coordinates.sphericalToCartesian(rightAscension, declination, 1, dirICRS);
        alpha = rightAscension;
        delta = declination;
        haveAngles = true;
        return this;
    }

    /**
     * Calculates the angle between this Place and another Place
     *
     * @param p the other Place
     *
     * @return angle between them [rad]
     */
    public double getAngleTo(Place p) {
        Vector3d v1 = this.getDirection();
        Vector3d v2 = p.getDirection();
        Vector3d sum = v1.cpy().add(v2);
        Vector3d dif = v1.sub(v2);

        return 2. * FastMath.atan2(dif.len(), sum.len());
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return "Place at (alpha, delta) = (" + this.getAlpha() + ", " + this.getDelta() + ")";
    }

    /**
     * Moves the current Place to a random position
     */
    public Place moveToRandom(Random rnd) {
        double x = rnd.nextGaussian();
        double y = rnd.nextGaussian();
        double z = rnd.nextGaussian();
        dirICRS = new Vector3d(x, y, z).nor();
        haveAngles = false;
        return this;
    }

    /**
     * Moves the current Place to a random position within a certain radius of a
     * given Place.
     */
    public Place moveToRandom(Random rnd, Place centre, double radius) {

        // construct two unit vectors p and q normal to c:
        Vector3d pole = new Vector3d(0., 0., 1.);
        Vector3d c = centre.getDirection();
        Vector3d p = pole.cpy().crs(c);
        double pNorm = p.len();
        if (pNorm < 0.1) {
            // this pole was close to c; choose another pole:
            pole.set(1., 0., 0.);
            p = pole.cpy().crs(c);
            pNorm = p.len();
        }
        p.scl(1.0 / pNorm);
        Vector3d q = c.cpy().crs(p);

        double tMax = getWeight(radius);
        double t = tMax * rnd.nextDouble();
        double phi = 2.0 * FastMath.PI * rnd.nextDouble();
        double s = 2.0 * FastMath.sqrt(t * (1.0 - t));
        double x = s * FastMath.cos(phi);
        double y = s * FastMath.sin(phi);
        double z = 1.0 - 2.0 * t;
        dirICRS = c.scl(z).scaleAdd(x, p).scaleAdd(y, q);
        haveAngles = false;
        return this;
    }

    private double getWeight(double radius) {
        double w;
        if (radius <= 0.0) {
            w = 0.0;
        } else if (radius < 1.0) {
            w = 0.5 * FastMath.pow(Math.sin(radius), 2) / (1.0 + FastMath.cos(radius));
        } else if (radius < FastMath.PI) {
            w = 0.5 * (1.0 - FastMath.cos(radius));
        } else {
            w = 1.0;
        }
        return w;
    }

    /**
     * Internal conversion from direction to (alpha, delta)
     */
    protected void calcAngles() {
        Vector3d aux = new Vector3d();
        Coordinates.cartesianToSpherical(dirICRS, aux);
        alpha = aux.x;
        delta = aux.y;
        haveAngles = true;
    }

}
