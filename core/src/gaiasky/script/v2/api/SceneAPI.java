/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import com.badlogic.ashley.core.Entity;
import gaiasky.script.v2.impl.SceneModule;

/**
 * API definition for the scene module, {@link SceneModule}.
 * <p>
 * The scene module contains calls and methods to access, modify, and query the internal scene.
 */
public interface SceneAPI {
    /**
     * Get the reference to an entity given its name.
     *
     * @param name Entity name.
     **/
    Entity get_entity(String name);

    /**
     * Get the reference to an entity given its name and a timeout in seconds.
     * If the entity is not in the scene after the timeout has passed, ths method
     * returns null.
     *
     * @param name           Entity name.
     * @param timeOutSeconds Timeout time, in seconds.
     **/
    Entity get_entity(String name, double timeOutSeconds);

    /**
     * Get a focus object from the scene given its name.
     *
     * @param name The name of the focus object.
     *
     * @return The reference to the object if it exists, null otherwise.
     */
    Entity get_focus(String name);
}
