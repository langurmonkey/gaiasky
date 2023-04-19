/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class VRDeviceExtractor extends AbstractExtractSystem {

    public VRDeviceExtractor(Family family, int priority) {
        super(family, priority);
    }

    public void setRenderGroup(RenderGroup renderGroup) {
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        if (this.mustRender(base)) {
            var vr = Mapper.vr.get(entity);
            if(vr.device != null && vr.device.isActive()) {
                var render = Mapper.render.get(entity);
                addToRender(render, RenderGroup.MODEL_PIX);
                if (Mapper.line.has(entity)) {
                    addToRender(render, RenderGroup.LINE_LATE);
                }
            }
        }
    }
}
