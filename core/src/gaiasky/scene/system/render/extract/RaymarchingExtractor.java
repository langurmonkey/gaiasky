/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectSet;
import gaiasky.event.Event;
import gaiasky.event.EventManager;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.component.*;
import gaiasky.util.Logger;

/**
 * Extract ray-marching objects for rendering. Importantly, this class activates/deactivates the
 * ray-marching post-processing effects depending on their solid angle w.r.t. the camera.
 */
public class RaymarchingExtractor extends AbstractExtractSystem {
    private static final Logger.Log logger = Logger.getLogger(RaymarchingExtractor.class);

    private static ObjectSet<Raymarching> lastMustRender;
    private static ObjectFloatMap<Raymarching> lastAlpha;

    public RaymarchingExtractor(Family family,
                                int priority) {
        super(family, priority);
        lastMustRender = new ObjectSet<>();
        lastAlpha = new ObjectFloatMap<>();
    }


    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        var base = Mapper.base.get(entity);
        var body = Mapper.body.get(entity);

        // Ray-marching effect.
        var rm = Mapper.raymarching.get(entity);
        rayMarchingState(entity, base, body, rm);

        // Label rendering.
        var sa = Mapper.sa.get(entity);
        var render = Mapper.render.get(entity);
        var label = Mapper.label.get(entity);
        if (renderText(base, body, sa, label)) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    private boolean mustRenderNoOpacity(Base base) {
        return base.initialized && base.opacity > 0 && !base.copy && renderer.allVisible(base.ct) && base.isVisible();
    }

    private void rayMarchingState(Entity entity,
                                  Base base,
                                  Body body,
                                  Raymarching rm) {
        if (rm != null && rm.raymarchingShader != null) {
            camera.checkClosestBody(entity);
            var mustRender = mustRender(base);

            if (mustRender) {
                // Check enable/disable
                double solidAngleThreshold = 0.1e-2;
                if (body.solidAngleApparent > solidAngleThreshold) {
                    activate(base, rm, entity);
                } else {
                    deactivate(base, rm, entity);
                }

                // Alpha.
                var alpha = renderer.alpha(base.ct);
                if (lastAlpha.get(rm, 1f) != alpha) {
                    // Update opacity via event.
                    EventManager.publish(Event.RAYMARCHING_OPACITY_CMD, rm, base.getName(), alpha);
                }

                // Update last alpha.
                lastAlpha.put(rm, alpha);
                // Update last must render.
                lastMustRender.add(rm);
            } else {
                // We went off!
                if (lastMustRender.contains(rm)) {
                    deactivate(base, rm, entity);
                }

                // Update last must render.
                lastMustRender.remove(rm);
            }
        }
    }

    private void activate(Base base,
                          Raymarching rm,
                          Entity entity) {
        if (!rm.isOn) {
            // Turn on
            logger.debug("Ray marching effect enabled: " + base.getName());
            EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), true, entity);
            rm.isOn = true;
        }
    }

    private void deactivate(Base base,
                            Raymarching rm,
                            Entity entity) {
        if (rm.isOn) {
            // Turn off
            logger.debug("Ray marching effect disabled: " + base.getName());
            EventManager.publish(Event.RAYMARCHING_CMD, this, base.getName(), false, entity);
            rm.isOn = false;
        }
    }

    private boolean renderText(Base base,
                               Body body,
                               SolidAngle sa,
                               Label label) {
        return base.names != null
                && label.renderLabel()
                && mustRender(base)
                && (label.forceLabel() || body.solidAngleApparent >= sa.thresholdLabel / label.labelBias);
    }
}
