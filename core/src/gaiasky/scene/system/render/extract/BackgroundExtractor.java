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
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;

public class BackgroundExtractor extends AbstractExtractSystem {
    public BackgroundExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity);
    }

    private void addToRenderLists(Entity entity) {
        var base = Mapper.base.get(entity);

        if (mustRender(base)) {
            var render = Mapper.render.get(entity);

            if (Mapper.grid.has(entity)) {
                // UV grid
                addToRender(render, RenderGroup.MODEL_VERT_GRID);
                LabelEntityRenderSystem.resetVerticalOffset();
                addToRender(render, RenderGroup.FONT_LABEL);
            } else {
                // Regular background model (skybox)
                var label = Mapper.label.get(entity);
                var renderType = Mapper.renderType.get(entity);

                addToRender(render, renderType.renderGroup);
                if (label.label) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            }
        }
    }
}
