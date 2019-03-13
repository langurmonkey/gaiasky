package gaia.cu9.ari.gaiaorbit.scenegraph;

import gaia.cu9.ari.gaiaorbit.util.math.Vector3d;

public interface IStarFocus extends IFocus, IProperMotion {

    /**
     * Gets the catalog source of this star. Possible values are:
     * <ul>
     * <li>-1: Unknown</li>
     * <li>1: Gaia</li>
     * <li>2: Hipparcos (HYG)</li>
     * <li>3: Tycho</li>
     * <li>4: Other</li>
     * </ul>
     *
     * @return The catalog source number
     */
    int getCatalogSource();

    /**
     * Returns the identifier
     *
     * @return The identifier of this star
     */
    long getId();

    /**
     * Returns the HIP number of this star, or negative if it has no HIP number
     *
     * @return The HIP number
     */
    int getHip();

    /**
     * Returns the TYC string identifier or null if it does not have one
     *
     * @return The TYC string
     */
    String getTycho();

    /**
     * Returns the closest star distance to the camera
     *
     * @return Distance of closest star to camera
     */
    double getClosestDist();

    /**
     * Returns the size of the closest star
     *
     * @return The size of the closest star
     */
    double getClosestSize();

    /**
     * Name of closest star to camera
     *
     * @return The name of the closest star to the camera
     */
    String getClosestName();

    /**
     * Returns the position of the closest star in camera coordinates
     *
     * @param out The out vector
     * @return The out vector with the position
     */
    Vector3d getClosestPos(Vector3d out);

    /**
     * Returns the color of the closest star
     *
     * @return The color in rgb
     */
    float[] getClosestCol();

}
