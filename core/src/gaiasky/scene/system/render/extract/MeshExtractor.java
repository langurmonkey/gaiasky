/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;

public class MeshExtractor extends AbstractExtractSystem {

    public MeshExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var render = Mapper.render.get(entity);
        var mesh = Mapper.mesh.get(entity);

        if (mustRender(base) && GaiaSky.instance.isInitialised()) {
            switch (mesh.shading) {
                case ADDITIVE -> addToRender(render, RenderGroup.MODEL_VERT_ADDITIVE);
                case REGULAR -> addToRender(render, RenderGroup.MODEL_PIX_EARLY);
                case DUST -> addToRender(render, RenderGroup.MODEL_PIX_DUST);
            }

            if (Mapper.label.get(entity).renderLabel())
                addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
