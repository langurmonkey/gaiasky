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
import gaiasky.render.RenderGroup;
import gaiasky.render.api.IRenderable;
import gaiasky.scene.Mapper;

public class Render implements Component, IRenderable {

    /** The entity. **/
    public Entity entity;
    /** Must render this element to a half-resolution buffer in a post-pass. **/
    public boolean halfResolutionBuffer = false;
    /** The render group. **/
    public RenderGroup renderGroup = null;

    public void setRenderGroup(String rg) {
        this.renderGroup = RenderGroup.valueOf(rg);
    }

    public void setRendergroup(String rg) {
        setRenderGroup(rg);
    }

    public void setBillboardRenderGroup(String rg) {
        this.renderGroup = RenderGroup.valueOf(rg);
    }

    public Entity getEntity() {
        return entity;
    }

    public void setHalfResolutionBuffer(Boolean halfResolutionBuffer) {
        this.halfResolutionBuffer = halfResolutionBuffer;
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

    @Override
    public boolean isHalfResolutionBuffer() {
        return halfResolutionBuffer;
    }
}
