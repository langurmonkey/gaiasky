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
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;
import gaiasky.util.Settings;

public class VolumeInitializer extends AbstractInitSystem {
    public VolumeInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var model = Mapper.model.get(entity);
        var volume = Mapper.volume.get(entity);
        if (volume.fragmentShader != null && volume.vertexShader != null) {
            // Key is hash code of concatenation.
            volume.key = (volume.fragmentShader + volume.vertexShader).hashCode();
        }
        // Renderer.
        model.renderConsumer = ModelEntityRenderSystem::renderVolume;
    }

    @Override
    public void setUpEntity(Entity entity) {
        var body = Mapper.body.get(entity);
        var model = Mapper.model.get(entity);
        var label = Mapper.label.get(entity);
        var sa = Mapper.sa.get(entity);

        // Set units.
        model.model.setUnits(Constants.KM_TO_U);
        // Set size.
        model.model.updateSize(body.size);

        // Set up label
        sa.thresholdLabel = (Math.toRadians(1e-6) / Settings.settings.scene.label.number) * 5000.0;
        label.textScale = 0.2f;
        label.labelMax = 0.0006f;
        if (label.labelFactor == 0)
            label.labelFactor = 0.5e-3f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextBase;
    }
}
