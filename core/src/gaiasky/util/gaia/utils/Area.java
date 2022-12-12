/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.gaia.utils;

public interface Area {

    /**
     * Determine the minimum angle between a great circle and the Area boundary
     *
     * @param spinAxisPlace great circle pole given as a Place
     *
     * @return minimum angle [rad]
     */
    double altitude(Place spinAxisPlace);

    /**
     * Determine whether a given Place is within the Area
     *
     * @param p the Place
     *
     * @return true if p is within the Area
     */
    boolean contains(Place p);

    /**
     * Determine the weighted mid-point of the Area
     *
     * @return the centre
     */
    Place getMidPoint();

    /**
     * Determine the weight of the Area
     *
     * @return the weight
     */
    double getWeight();
}
