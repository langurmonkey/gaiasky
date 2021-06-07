/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.stars;

import gaiasky.scenegraph.CelestialBody;

/**
 * Interface for catalog filters for celestial bodies.
 */
public interface CatalogFilter {
    /**
     * Implements the filtering.
     * @param s The celestial body.
     * @return True if the celestial body passes the filter and should be added to the final catalog, false otherwise.
     */
    boolean filter(CelestialBody s);
}
