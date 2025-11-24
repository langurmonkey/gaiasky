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

public class BillboardSetExtractor extends AbstractExtractSystem {

    public BillboardSetExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var fade = Mapper.fade.get(entity);

        if (mustRender(base) && (fade.fadeIn == null || fade.currentDistance > fade.fadeIn.x) && (fade.fadeOut == null || fade.currentDistance < fade.fadeOut.y)) {
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);
            var billboard = Mapper.billboardSet.get(entity);

            // Label.
            if (label.label && label.renderLabel()) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }

            // Billboard group.
            if (billboard.procedural) {
                // Procedural galaxy generation.
                addToRender(render, RenderGroup.BILLBOARD_GROUP_PROCEDURAL);
            } else {
                // Galaxy data comes from files.
                addToRender(render, RenderGroup.BILLBOARD_GROUP);
            }
        }
    }
}
