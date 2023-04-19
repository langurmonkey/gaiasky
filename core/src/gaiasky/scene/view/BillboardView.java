/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Billboard;
import gaiasky.scene.component.Celestial;
import gaiasky.scene.component.GraphNode;

public class BillboardView extends BaseView {

    public Billboard billboard;
    public GraphNode graph;
    public Celestial celestial;

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
        check(entity, Mapper.billboard, Billboard.class);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        billboard = Mapper.billboard.get(entity);
        graph = Mapper.graph.get(entity);
        celestial = Mapper.celestial.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.billboard = null;
        this.graph = null;
        this.celestial = null;
    }
}
