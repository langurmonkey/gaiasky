/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;

public interface EntityInitializer {

    /**
     * Contains the initialization of this entity before the scene graph
     * structure has been constructed, or the entity is in the index.
     * Typically, this adds resources to load via the asset manager, and
     * initializes basic attributes.
     *
     * @param entity The entity.
     */
    void initializeEntity(Entity entity);

    /**
     * Contains the set-up of this entity, after the entity has been
     * added to the scene graph, it is in the index, and assets have been loaded.
     * Typically, this fetches loaded assets from the asset manager, and
     * completes the initialization.
     *
     * @param entity The entity.
     */
    void setUpEntity(Entity entity);
}
