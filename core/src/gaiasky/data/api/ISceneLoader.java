/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Scene;
import uk.ac.starlink.util.DataSource;

import java.io.FileNotFoundException;
import java.util.Map;

public interface ISceneLoader {

    void initialize(String[] files, Scene scene) throws RuntimeException;

    void initialize(String[] files, String dsLocation, Scene scene) throws RuntimeException;

    void initialize(DataSource ds, Scene scene);

    /**
     * Performs the loading and returns an array with the entities loaded.
     *
     * @return The loaded entities.
     */
    Array<Entity> loadData() throws FileNotFoundException;

    void setName(String name);

    void setDescription(String description);

    void setParams(Map<String, Object> params);

    void setIndex(Map<String, Entity> index);

}
