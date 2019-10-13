/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaia.cu9.ari.gaiaorbit.data.orbit;

import gaia.cu9.ari.gaiaorbit.assets.OrbitDataLoader.OrbitDataLoaderParameter;
import gaia.cu9.ari.gaiaorbit.data.util.PointCloudData;

public interface IOrbitDataProvider {

    /**
     * Loads the orbit data into the OrbitData object in the internal
     * units.
     * @param file The file path
     * @param source The parameters
     */
    public void load(String file, OrbitDataLoaderParameter source);

    /**
     * Loads the orbit data into the OrbitData object in the internal
     * units.
     * @param file The file path
     * @param source The parameters
     * @param newmethod Use new method (for orbital elements only)
     */
    public void load(String file, OrbitDataLoaderParameter source, boolean newmethod);

    public PointCloudData getData();

}
