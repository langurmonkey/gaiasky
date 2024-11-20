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

public class ConstellationExtractor extends AbstractExtractSystem {

    public ConstellationExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var constel = Mapper.constel.get(entity);

        if (mustRender(base)) {
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);

            addToRender(render, RenderGroup.LINE);
            if (constel.allLoaded && label.renderLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
        }
    }
}
