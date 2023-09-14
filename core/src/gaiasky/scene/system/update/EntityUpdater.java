/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;

public interface EntityUpdater {

    /**
     * Returns the family for this updater.
     *
     * @return The family.
     */
    Family getFamily();

    /**
     * Updates the entity.
     *
     * @param entity    The entity to update.
     * @param deltaTime The delta time since last frame.
     */
    void updateEntity(Entity entity, float deltaTime);
}
