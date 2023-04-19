/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.tag.TagSetElement;

public class OrbitElementsSetInitializer extends AbstractInitSystem {

    public OrbitElementsSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
    }

    @Override
    public void setUpEntity(Entity entity) {
        var graph = Mapper.graph.get(entity);
        if (graph != null && graph.children != null) {
            for (Entity child : graph.children) {
                // Tag with set element component
                child.add(new TagSetElement());
            }
        }
    }
}
