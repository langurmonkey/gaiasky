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
import gaiasky.scene.view.LabelView;

public class LocExtractor extends AbstractExtractSystem {

    private final LabelView view;

    public LocExtractor(Family family, int priority) {
        super(family, priority);
        this.view = new LabelView();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        view.setEntity(entity);
        if (!view.renderTextLocation()) {
            return;
        }
        var base = Mapper.base.get(entity);
        if (mustRender(base)) {
            var loc = Mapper.loc.get(entity);
            addToRender(Mapper.render.get(entity), RenderGroup.FONT_LABEL);
            // Only render marker if texture is not 'none'.
            if (!(loc.locationMarkerTexture != null
                    && loc.locationMarkerTexture.equalsIgnoreCase("none"))) {
                addToRender(Mapper.render.get(entity), RenderGroup.SPRITE);
            }
        }
    }

}
