/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.component.Raymarching;
import gaiasky.scene.entity.FocusActive;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Settings;

/**
 * Performs the initialization of objects of the Invisible archetype. It initializes their components
 * (graph, focus, label, etc.).
 */
public class InvisibleInitializer extends AbstractInitSystem {

    public InvisibleInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var graph = Mapper.graph.get(entity);
        var focus = Mapper.focus.get(entity);

        // Focus active
        focus.activeFunction = FocusActive::isFocusActiveTrue;

        if (graph.parentName == null) {
            graph.parentName = Scene.ROOT_NAME;
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        // Remove raymarching tag if needed.
        var rm = Mapper.raymarching.get(entity);
        boolean raymarching = true;
        if (rm.raymarchingShader == null) {
            entity.remove(Raymarching.class);
            raymarching = false;
        }

        // Set up label
        var label = Mapper.label.get(entity);
        var sa = Mapper.sa.get(entity);
        if (raymarching) {
            label.textScale = 0.2f;
            label.labelMax = 1.8f;
            if (label.labelFactor == 0)
                label.labelFactor = 0.5e-3f;
        } else {
            label.textScale = 0.15f;
            label.labelMax = 1.2f;
            if (label.labelFactor == 0)
                label.labelFactor = 0.3e-3f;
        }
        sa.thresholdLabel = (Math.toRadians(1e-6) * Constants.DISTANCE_SCALE_FACTOR / Settings.settings.scene.label.number) * 60.0;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextBase;

    }
}
