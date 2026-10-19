/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.initialize;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.render.gdx.shader.attribute.IntAttribute;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.render.draw.model.ModelEntityRenderSystem;
import gaiasky.scene.system.render.draw.text.LabelEntityRenderSystem;
import gaiasky.scene.view.LabelView;
import gaiasky.util.Constants;

/**
 * Initializes volume entities, such as those used for volumetric rendering.
 */
public class VolumeInitializer extends AbstractInitSystem {
    /** The shaders clamp the step count to at least 40, so anything lower does nothing useful. */
    private static final int MIN_ITERATIONS = 40;

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
        var volume = Mapper.volume.get(entity);
        var label = Mapper.label.get(entity);
        var sa = Mapper.sa.get(entity);

        // Ray-march iteration count for this graphics quality, bridged into the shader as an attribute.
        int iterations = volume.iterations[GaiaSky.settings().graphics.quality.ordinal()];
        if (iterations < MIN_ITERATIONS) {
            logger.warn("Volume iterations " + iterations + " below " + MIN_ITERATIONS + ", clamping to " + MIN_ITERATIONS);
            iterations = MIN_ITERATIONS;
        }
        if (model.model.env != null) {
            model.model.env.set(new IntAttribute(IntAttribute.VolumeIterations, iterations));
        } else {
            // ModelInitializer runs before this one and builds the environment, so this only
            // happens if that ordering changes - and it would silently leave the shader at its own
            // 90 iterations.
            logger.warn("Volume model has no environment, cannot set the iteration count: " + entity);
        }
        // Set units.
        model.model.setUnits(Constants.KM_TO_U);
        // Set size.
        model.model.updateSize(body.size);

        // Set up label
        sa.thresholdLabel = (Math.toRadians(1e-6) / GaiaSky.settings().scene.label.number) * 5000.0;
        label.textScale = 0.2f;
        label.labelMax = 0.0006f;
        if (label.labelFactor == 0)
            label.labelFactor = 0.5e-3f;
        label.renderConsumer = LabelEntityRenderSystem::renderCelestial;
        label.renderFunction = LabelView::renderTextBase;

    }
}
