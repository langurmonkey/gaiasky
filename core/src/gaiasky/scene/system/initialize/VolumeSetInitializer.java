/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;

public class VolumeSetInitializer extends AbstractInitSystem {
    public VolumeSetInitializer(boolean setUp, Family family, int priority) {
        super(setUp, family, priority);
    }

    @Override
    public void initializeEntity(Entity entity) {
        var label = Mapper.label.get(entity);

        label.label = true;
        label.textScale = 3;
        label.labelMax = (float) (2e-3 / Constants.DISTANCE_SCALE_FACTOR);
        label.labelFactor = 0.6f;
        label.renderConsumer = LabelEntityRenderSystem::renderBillboardSet;
        label.renderFunction = LabelView::renderTextBase;
        label.depthBufferConsumer = LabelView::noTextDepthBuffer;

        var volume = Mapper.volumeSet.get(entity);
        if (volume != null && volume.datasets != null) {
            for (var vol : volume.datasets) {
                vol.initialize();
            }
        }
    }

    @Override
    public void setUpEntity(Entity entity) {
        var volume = Mapper.volumeSet.get(entity);
        if (volume != null && volume.datasets != null) {
            for (var vol : volume.datasets) {
                vol.doneLoading();
            }
        }
    }
}
