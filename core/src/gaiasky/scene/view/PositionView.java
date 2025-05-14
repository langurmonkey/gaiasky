/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.view;

import com.badlogic.ashley.core.Entity;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.Body;
import gaiasky.util.math.Vector3Q;
import gaiasky.util.math.Vector3D;
import gaiasky.util.tree.IPosition;

public non-sealed class PositionView extends AbstractView implements IPosition {

    /** The body component. **/
    private Body body;

    public PositionView(Entity entity) {
        super(entity);
    }

    @Override
    protected boolean componentsCheck(Entity entity) {
        return entity != null && Mapper.body.get(entity) == body;
    }

    @Override
    protected void entityCheck(Entity entity) {
        if (!Mapper.body.has(entity)) {
            throw new RuntimeException("The given entity does not have a " + Body.class.getSimpleName() + " component: Can't be a " + this.getClass().getSimpleName() + ".");
        }
    }

    @Override
    protected void entityChanged() {
        this.body = Mapper.body.get(entity);
    }

    @Override
    protected void entityCleared() {
        this.body = null;
    }

    @Override
    public Vector3Q getPosition() {
        return body.pos;
    }

    @Override
    public Vector3D getVelocity() {
        return null;
    }
}
