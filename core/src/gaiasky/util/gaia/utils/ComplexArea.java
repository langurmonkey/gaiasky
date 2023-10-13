/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

import gaiasky.util.math.Vector3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public class ComplexArea implements Area {

    private final static double piHalf = Math.PI / 2.0;
    private final Collection<Area> listOfAreas;
    private String name;

    private Place p;

    public ComplexArea() {
        listOfAreas = new ArrayList<Area>();
    }

    public void add(Area a) {
        listOfAreas.add(a);
    }

    /**
     *
     */
    @Override
    public double altitude(Place spinAxisPlace) {
        double alt = Math.PI;
        for (Area a : listOfAreas) {
            alt = Math.min(alt, a.altitude(spinAxisPlace));
        }
        return alt;
    }

    /**
     *
     */
    @Override
    public boolean contains(Place p) {
        boolean isIn = false;
        for (Area a : listOfAreas) {
            isIn = (isIn || a.contains(p));
        }
        return isIn;
    }

    /**
     *
     */
    @Override
    public Place getMidPoint() {
        Vector3d sum = new Vector3d();
        for (Area a : listOfAreas) {
            sum.scaleAdd(a.getWeight(), a.getMidPoint().getDirection());
        }
        if (sum.len() == 0.0) {
            //sum.fromSphericalCoordinates(0.0, 0.0);
            sum.set(0, 0, 1);
        }
        return new Place(sum);
    }

    /**
     *
     */
    @Override
    public double getWeight() {
        double sum = 0.0;
        for (Area a : listOfAreas) {
            sum += a.getWeight();
        }
        return sum;
    }

    /**
     * Get a random Place within the ComplexArea
     *
     * @param rnd Random number generator
     *
     * @return random Place
     */
    public Place getRandomPlace(Random rnd) {

        // First find an enclosing circular area:
        Place centre = new Place(this.getMidPoint());
        double radius = piHalf - this.altitude(centre);
        p = new Place(rnd);
        p.moveToRandom(rnd, centre, radius);
        while (!this.contains(p)) {
            p.moveToRandom(rnd, centre, radius);
        }
        return p;
    }

    /**
     * @return the solid angle [deg^2]
     */
    public double getSquareDegrees() {

        // Use Monte Carlo integration to estimate the solid angle. First find
        // an enclosing circular area:
        Place centre = new Place(this.getMidPoint());
        double radius = piHalf - this.altitude(centre);
        CircleArea circle = new CircleArea(centre, radius);
        int nMC = 1000000;
        int in = 0;
        Random rnd = new Random(0L);
        Place p = new Place();
        for (int i = 0; i < nMC; i++) {
            p.moveToRandom(rnd, centre, radius);
            if (this.contains(p))
                in++;
        }
        return circle.getSquareDegrees() * ((double) in / (double) nMC);
    }

    /**
     * @return the listOfAreas
     */
    public Collection<Area> getListOfAreas() {
        return this.listOfAreas;
    }

    /**
     * Get the name of the area
     *
     * @return The name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of the area
     *
     * @param str The string
     */
    public void setName(String str) {
        this.name = str;
    }
}
