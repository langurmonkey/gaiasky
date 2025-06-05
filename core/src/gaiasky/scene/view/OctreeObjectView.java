/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Octant;
import gaiasky.scene.component.StarSet;
import gaiasky.scene.entity.SetUtils;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

public class OctreeObjectView extends PositionView implements IOctreeObject {

    public StarSet set;
    Octant octant;

    public OctreeObjectView(Entity entity) {
        super(entity);
    }

    @Override
    protected void entityCheck(Entity entity) {
        super.entityCheck(entity);
    }

    @Override
    protected void entityChanged() {
        super.entityChanged();
        this.set = Mapper.starSet.get(entity);
        this.octant = Mapper.octant.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.set = null;
        this.octant = null;
    }

    @Override
    public int getStarCount() {
        return set != null && set.pointData != null ? set.pointData.size() : 0;
    }

    @Override
    public void dispose() {
        SetUtils.dispose(entity, set);
    }

    public void setOctant(OctreeNode octreeNode) {
        octant.octant = octreeNode;
    }
}
