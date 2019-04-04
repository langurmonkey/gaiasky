/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.stars;

import gaia.cu9.ari.gaiaorbit.scenegraph.CelestialBody;

/**
 * Interface for catalog filters for celestial bodies
 * @author tsagrista
 *
 */
public interface CatalogFilter {
    /**
     * Implements the filtering
     * @param s The celestial body
     * @return True if the celestial body passes the filter and should be added to the final catalog, false otherwise
     */
    boolean filter(CelestialBody s);
}
