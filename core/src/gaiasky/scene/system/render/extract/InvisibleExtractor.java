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
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Body;
import gaiasky.scene.component.Label;
import gaiasky.scene.component.SolidAngle;

public class InvisibleExtractor extends AbstractExtractSystem {

    public InvisibleExtractor(Family family, int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        // Label rendering.
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);
        var sa = Mapper.sa.get(entity);
        var render = Mapper.render.get(entity);
        var label = Mapper.label.get(entity);
        if (renderText(base, body, sa, label)) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    private boolean renderText(Base base,
                               Body body,
                               SolidAngle sa,
                               Label label) {
        return base.names != null
                && label.label
                && label.renderLabel
                && mustRender(base)
                && (label.forceLabel || body.solidAngleApparent >= sa.thresholdLabel / label.labelBias);
    }
}
