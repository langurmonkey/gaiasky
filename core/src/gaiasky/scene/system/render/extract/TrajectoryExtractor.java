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
import gaiasky.scene.entity.EntityUtils;
import gaiasky.scene.entity.TrajectoryUtils;
import gaiasky.util.Settings;
import gaiasky.util.math.MathUtilsDouble;

public class TrajectoryExtractor extends AbstractExtractSystem {

    /**
     * Special overlap factor
     */
    protected static final float SHADER_MODEL_OVERLAP_FACTOR = 20f;

    private final TrajectoryUtils utils;

    public TrajectoryExtractor(Family family, int priority) {
        super(family, priority);
        this.utils = new TrajectoryUtils();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        extractEntity(entity);
    }

    public void extractEntity(Entity entity) {
        addToRenderLists(entity, camera);
    }

    protected void addToRenderLists(Entity entity, ICamera camera) {
        var base = Mapper.base.get(entity);

        if (this.mustRender(base)) {
            var trajectory = Mapper.trajectory.get(entity);
            var body = Mapper.body.get(entity);
            var render = Mapper.render.get(entity);
            var label = Mapper.label.get(entity);

            if (trajectory.bodyRepresentation.isOrbit()) {
                var verts = Mapper.verts.get(entity);

                // If there is overflow, return.
                if (trajectory.body != null && EntityUtils.isCoordinatesTimeOverflow(trajectory.body))
                    return;

                boolean added = false;
                float angleLimit = (float) Settings.settings.scene.renderer.orbitSolidAngleThreshold * camera.getFovFactor();
                var angleLimitUp = angleLimit * SHADER_MODEL_OVERLAP_FACTOR;
                if (body.solidAngle > angleLimit * 0.00001) {
                    // Fade the orbit using its solid angle and the threshold in the settings.
                    if (body.solidAngle < angleLimitUp * 0.00001) {
                        trajectory.alpha = MathUtilsDouble.flint(body.solidAngle, angleLimit, angleLimitUp, 0, body.color[3]);
                    } else {
                        trajectory.alpha = body.color[3];
                    }

                    if (trajectory.body == null) {
                        // There is no body, always render.
                        addToRender(render, verts.renderGroup);
                        added = true;
                    } else {
                        var bodyBody = Mapper.body.get(trajectory.body);
                        // For orbits with a body objects, we fade it out as we move closer to the body, using the distUp and distDown, which are in
                        // body radius units.
                        if (bodyBody.distToCamera > trajectory.distDown) {
                            if (bodyBody.distToCamera < trajectory.distUp)
                                trajectory.alpha *= MathUtilsDouble.lint(bodyBody.distToCamera, trajectory.distDown / camera.getFovFactor(), trajectory.distUp / camera.getFovFactor(), 0, 1);
                            addToRender(render, verts.renderGroup);
                            added = true;
                        }
                    }
                }

                if (verts.pointCloudData == null || added) {
                    utils.refreshOrbit(trajectory, verts, false);
                }
            }
            // Orbital element renderer.
            if (trajectory.body == null && !trajectory.isInOrbitalElementsGroup && base.ct.get(ComponentType.Asteroids.ordinal()) && renderer.isOn(ComponentType.Asteroids)) {
                addToRender(render, RenderGroup.ORBITAL_ELEMENTS_PARTICLE);
            }
            if (label.renderLabel && label.forceLabel) {
                addToRender(render, RenderGroup.FONT_LABEL);
            }
        }

    }
}
