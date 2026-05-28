/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

import net.jafama.FastMath;

public record CircleArea(Place centre, double radius) implements Area {

    private final static double piHalf = FastMath.PI / 2.0;

    private final static double squareDegreesOfSphere = 129600.0 / FastMath.PI;

    /**
     * Creates an instance of a CircleArea about a given centre and radius.
     *
     * @param centre the centre
     * @param radius the radius [rad]
     */
    public CircleArea(Place centre, double radius) {
        this.centre = new Place(centre);
        this.radius = radius;
    }

    /**
     *
     */
    @Override
    public double altitude(Place pole) {
        double absLat = FastMath.abs(piHalf - pole.getAngleTo(centre));
        return FastMath.max(absLat - radius, 0.0);
    }

    /**
     *
     */
    @Override
    public boolean contains(Place p) {
        return (p.getAngleTo(centre) < radius);
    }

    /**
     *
     */
    @Override
    public Place getMidPoint() {
        return new Place(centre);
    }

    /**
     *
     */
    @Override
    public double getWeight() {
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
     * @return the centre
     */
    @Override
    public Place centre() {
        return this.centre;
    }

    /**
     * @return the radius
     */
    @Override
    public double radius() {
        return this.radius;
    }

    /**
     * @return the solid angle [deg^2]
     */
    public double getSquareDegrees() {
        return this.getWeight() * squareDegreesOfSphere;
    }
}
