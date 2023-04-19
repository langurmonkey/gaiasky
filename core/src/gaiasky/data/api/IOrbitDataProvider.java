/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;

public interface IOrbitDataProvider {

    /**
     * Loads the orbit data into the OrbitData object in the internal
     * units.
     *
     * @param file   The file path
     * @param source The parameters
     */
    void load(String file, OrbitDataLoaderParameters source);

    /**
     * Loads the orbit data into the OrbitData object in the internal
     * units.
     *
     * @param file      The file path
     * @param source    The parameters
     * @param newMethod Use new method (for orbital elements only)
     */
    void load(String file, OrbitDataLoaderParameters source, boolean newMethod);

    PointCloudData getData();

}
