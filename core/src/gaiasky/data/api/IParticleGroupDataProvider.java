/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import gaiasky.scene.api.IParticleRecord;
import gaiasky.util.math.Matrix4D;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface IParticleGroupDataProvider {
    /**
     * Loads the data as it is.
     *
     * @param file The file to load.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadData(String file);

    /**
     * Loads the data applying a factor using a memory mapped file for improved speed.
     *
     * @param file   The file to load.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadDataMapped(String file, double factor);

    /**
     * Loads the data applying a factor.
     *
     * @param file   The file to load.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadData(String file, double factor);

    /**
     * Loads the data applying a factor.
     *
     * @param is     Input stream to load the data from.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadData(InputStream is, double factor);

    /**
     * Sets a cap on the number of files to load. Set to negative for
     * unlimited.
     *
     * @param cap The file cap number.
     */
    void setFileNumberCap(int cap);

    /**
     * Sets the maximum number of stars to be processed per file. Set to
     * negative for unlimited.
     *
     * @param cap The star cap number.
     */
    void setStarNumberCap(int cap);

    /**
     * Set provider parameters as a map.
     *
     * @param params The parameters map.
     */
    void setProviderParams(Map<String, Object> params);

    /**
     * Sets a transform matrix to apply to all data points.
     * @param matrix The transform matrix.
     */
    void setTransformMatrix(Matrix4D matrix);

}
