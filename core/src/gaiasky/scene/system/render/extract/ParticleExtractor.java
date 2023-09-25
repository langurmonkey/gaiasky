/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.RenderGroup;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.*;
import gaiasky.scene.view.FocusView;
import gaiasky.util.Settings;

public class ParticleExtractor extends AbstractExtractSystem {

    private final FocusView view;

    public ParticleExtractor(Family family, int priority) {
        super(family, priority);
        view = new FocusView();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        addToRenderLists(entity, camera);
    }

    private void addToRenderLists(Entity entity, ICamera camera) {
        view.setEntity(entity);
        var base = Mapper.base.get(entity);
        camera.checkClosestParticle(view);

        if (this.mustRender(base)) {
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);
            var renderType = Mapper.renderType.get(entity);
            var hip = Mapper.hip.get(entity);


            if (hip == null) {
                addToRenderParticle(camera, entity, body, render, renderType);
            } else {
                addToRenderStar(camera, entity, body, render, renderType);
            }
        }
    }

    private void addToRenderParticle(ICamera camera, Entity entity, Body body, Render render, RenderType renderType) {
        addToRender(render, renderType.renderGroup);

        boolean hasPm = Mapper.pm.has(entity) && Mapper.pm.get(entity).hasPm;
        if (body.solidAngleApparent >= Settings.settings.scene.star.threshold.point / Settings.settings.scene.properMotion.number && hasPm) {
            addToRender(render, RenderGroup.LINE);
        }
        if (renderText(body, Mapper.sa.get(entity), Mapper.extra.get(entity))) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    private void addToRenderStar(ICamera camera, Entity entity, Body body, Render render, RenderType renderType) {
        addToRender(render, RenderGroup.POINT_STAR);

        if (body.solidAngleApparent >= Settings.settings.scene.star.threshold.point) {
            addToRender(render, RenderGroup.BILLBOARD_STAR);
            if (body.distToCamera < Mapper.distance.get(entity).distance * Mapper.modelScaffolding.get(entity).sizeScaleFactor) {
                camera.checkClosestBody(entity);
                addToRender(render, RenderGroup.MODEL_VERT_STAR);
            }
        }
        boolean hasPm = Mapper.pm.has(entity) && Mapper.pm.get(entity).hasPm;
        if (hasPm && body.solidAngleApparent >= Settings.settings.scene.star.threshold.point / Settings.settings.scene.properMotion.number) {
            addToRender(render, RenderGroup.LINE);
        }

        if ((renderText(body, Mapper.sa.get(entity), Mapper.extra.get(entity)))) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }

    }

    private boolean renderText(Body body, SolidAngle sa, ParticleExtra particleExtra) {
        return particleExtra.computedSize > 0 &&
                renderer.isOn(ComponentType.Labels) &&
                body.solidAngleApparent >= (sa.thresholdLabel / camera.getFovFactor());
    }
}
