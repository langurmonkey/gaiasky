/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.render.ComponentTypes;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;

public class Render implements Component, IRenderable {

    public Entity entity;

    public Entity getEntity() {
        return entity;
    }

    @Override
    public ComponentTypes getComponentType() {
        return Mapper.base.get(entity).ct;
    }

    @Override
    public double getDistToCamera() {
        return Mapper.body.get(entity).distToCamera;
    }

    @Override
    public float getOpacity() {
        return Mapper.base.get(entity).opacity;
    }
}
