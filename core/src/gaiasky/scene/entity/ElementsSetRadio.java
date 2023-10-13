/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.entity;

import com.badlogic.ashley.core.Entity;
import gaiasky.event.Event;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.initialize.ElementsSetInitializer;

public class ElementsSetRadio extends EntityRadio {

    private final ElementsSetInitializer initializer;

    public ElementsSetRadio(Entity entity, ElementsSetInitializer initializer) {
        super(entity);
        this.initializer = initializer;
    }

    @Override
    public void notify(Event event, Object source, Object... data) {
        if (event == Event.GPU_DISPOSE_ORBITAL_ELEMENTS) {
            if (source == entity) {
                initializer.initializeOrbitsWithOrbit(Mapper.graph.get(entity), Mapper.orbitElementsSet.get(entity));
            }
        }
    }
}
