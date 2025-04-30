/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.ComponentTypes;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

public class ModelExtractor extends AbstractExtractSystem {

    public ModelExtractor(Family family,
                          int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        extractEntity(entity);
    }

    public void extractEntity(Entity entity) {
        addToRenderLists(entity, camera);
    }

    protected void addToRenderLists(Entity entity,
                                    ICamera camera) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        if (graph.parent == null) {
            return;
        }
        var coord = Mapper.coordinates.get(entity);
        Coordinates parentCoord = Mapper.coordinates.get(graph.parent);

        if (mustRender(base) && isValidPosition(coord) && isValidPosition(parentCoord)) {
            var body = Mapper.body.get(entity);
            var model = Mapper.model.get(entity);
            var sa = Mapper.sa.get(entity);
            var scaffolding = Mapper.modelScaffolding.get(entity);
            var atmosphere = Mapper.atmosphere.get(entity);
            var cloud = Mapper.cloud.get(entity);
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);
            var renderFlags = Mapper.renderFlags.get(entity);

            camera.checkClosestBody(entity);

            if (Mapper.volume.has(entity)) {
                // Volume models.
                addToRender(render, RenderGroup.VOLUME);
                if (renderText(base, body, sa, label)) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }

            } else if (Mapper.tagBillboardGalaxy.has(entity)) {
                // Billboard galaxies.
                double thPoint = (sa.thresholdQuad * camera.getFovFactor()) / scaffolding.sizeScaleFactor;
                if (body.solidAngleApparent >= thPoint) {
                    addToRender(render, RenderGroup.MODEL_DIFFUSE);
                } else if (base.opacity > 0) {
                    addToRender(render, RenderGroup.BILLBOARD_GAL);
                }

                if (renderText(base, body, sa, label)) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }
            } else if (Mapper.tagBillboard.has(entity)) {
                // Simple billboards.
                if (body.solidAngleApparent >= sa.thresholdNone) {
                    addToRender(render, RenderGroup.MODEL_DIFFUSE);
                    if (label.renderLabel) {
                        addToRender(render, RenderGroup.FONT_LABEL);
                    }
                }
            } else {
                // Rest of models.
                double thPoint = (sa.thresholdPoint * camera.getFovFactor()) / scaffolding.sizeScaleFactor;
                if (body.solidAngleApparent >= thPoint) {
                    double thQuad2 = sa.thresholdQuad * camera.getFovFactor() * 2 / scaffolding.sizeScaleFactor;
                    double thQuad1 = thQuad2 / 8.0 / scaffolding.sizeScaleFactor;
                    if (body.solidAngleApparent < thPoint * 4) {
                        scaffolding.fadeOpacity = (float) MathUtilsDouble.flint(body.solidAngleApparent, thPoint, thPoint * 4, 1, 0);
                    } else {
                        scaffolding.fadeOpacity = (float) MathUtilsDouble.flint(body.solidAngleApparent, thQuad1, thQuad2, 0, 1);
                    }

                    if (body.solidAngleApparent < thQuad1) {
                        if (renderFlags == null || renderFlags.renderQuad) {
                            addToRender(render, RenderGroup.BILLBOARD_SSO);
                        }
                    } else if (body.solidAngleApparent > thQuad2) {
                        addToRenderModel(render, model);
                    } else {
                        // Both
                        if (renderFlags == null || renderFlags.renderQuad) {
                            addToRender(render, RenderGroup.BILLBOARD_SSO);
                        }
                        addToRenderModel(render, model);
                    }
                    if (renderText(base, body, sa, label)) {
                        addToRender(render, RenderGroup.FONT_LABEL);
                    }
                }
                if (!isInRender(render, RenderGroup.FONT_LABEL) && label.forceLabel) {
                    addToRender(render, RenderGroup.FONT_LABEL);
                }

                // Atmosphere (only planets)
                if (atmosphere != null &&
                        atmosphere.atmosphere != null &&
                        isInRender(render, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) &&
                        !coord.timeOverflow) {
                    addToRender(render, RenderGroup.MODEL_ATM);
                }

                // Cloud (only planets)
                if (cloud != null &&
                        cloud.cloud != null &&
                        isInRender(render, RenderGroup.MODEL_PIX, RenderGroup.MODEL_PIX_TESS) &&
                        !coord.timeOverflow) {
                    addToRender(render, RenderGroup.MODEL_CLOUD);
                }
            }
        }
    }

    private void addToRenderModel(Render render,
                                  Model model) {
        RenderGroup rg;
        var rt = Mapper.renderType.get(render.entity);
        if (rt != null && rt.renderGroup != null) {
            rg = rt.renderGroup;
        } else {
            rg = renderTessellated(model) ? RenderGroup.MODEL_PIX_TESS : RenderGroup.MODEL_PIX;
        }
        addToRender(render, rg);
    }

    private boolean renderTessellated(Model model) {
        return Settings.settings.scene.renderer.elevation.type.isTessellation() && model.model.isTessellated();
    }

    private boolean renderText(Base base,
                               Body body,
                               SolidAngle sa,
                               Label label) {
        return base.names != null
                && label.renderLabel
                && renderer.isOn(ComponentTypes.ComponentType.Labels)
                && (label.forceLabel || body.solidAngleApparent >= sa.thresholdLabel / label.labelBias);
    }

    private boolean isValidPosition(Coordinates coord) {
        return coord == null || !coord.timeOverflow;
    }
}
