/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import com.badlogic.ashley.core.Entity;
import gaiasky.data.util.OrbitDataLoader.OrbitDataLoaderParameters;
import gaiasky.data.util.PointCloudData;
import gaiasky.scene.component.Trajectory;

public interface IOrbitDataProvider {

    /**
     * Initializes the provider with the given entity and trajectory component.
     *
     * @param entity     The entity.
     * @param trajectory The trajectory component.
     */
    void initialize(Entity entity, Trajectory trajectory);

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
