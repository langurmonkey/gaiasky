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
import gaiasky.scene.component.Render;
import gaiasky.scene.component.StarSet;
import gaiasky.util.Settings;
import net.jafama.FastMath;

public class ParticleSetExtractor extends AbstractExtractSystem {
    public ParticleSetExtractor(Family family,
                                int priority) {
        super(family, priority);
    }

    @Override
    protected void processEntity(Entity entity,
                                 float deltaTime) {
        extractEntity(entity);
    }

    public void extractEntity(Entity entity) {
        var base = Mapper.base.get(entity);
        if (mustRender(base)) {
            var render = Mapper.render.get(entity);
            if (Mapper.starSet.has(entity)) {
                addToRenderLists(render, entity, Mapper.starSet.get(entity));
            } else {
                addToRenderLists(render);
            }
        }
    }

    /**
     * For star sets.
     **/
    private void addToRenderLists(Render render,
                                  Entity entity,
                                  StarSet starSet) {
        if (starSet.renderParticles) {
            if (starSet.variableStars) {
                addToRender(render, RenderGroup.VARIABLE_GROUP);
            } else {
                addToRender(render, RenderGroup.STAR_GROUP);
            }
            if (Settings.settings.scene.star.renderStarSpheres) {
                addToRender(render, RenderGroup.MODEL_VERT_STAR);
            }
            if (Settings.settings.scene.star.group.billboard) {
                addToRender(render, RenderGroup.BILLBOARD_STAR);
            }
        }
        if (renderer.isOn(ComponentTypes.ComponentType.VelocityVectors)) {
            if (Mapper.tagOctreeObject.has(entity)) {
                var octant = Mapper.octant.get(entity);
                if (octant.octant.observed && octant.octant.viewAngle > FastMath.toRadians(117)) {
                    addToRender(render, RenderGroup.LINE);
                }
            } else {
                addToRender(render, RenderGroup.LINE);
            }
        }
        if ((starSet.renderParticleLabels && starSet.numLabels > 0) || starSet.renderSetLabel) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }

    /**
     * For particle sets.
     **/
    private void addToRenderLists(Render render) {
        var set = Mapper.particleSet.get(render.entity);
        if (set.renderParticles) {
            if (set.isExtended) {
                if (set.isBillboard()) {
                    addToRender(render, RenderGroup.PARTICLE_GROUP_EXT_BILLBOARD);
                } else {
                    addToRender(render, RenderGroup.PARTICLE_GROUP_EXT_MODEL);
                }
            } else {
                addToRender(render, RenderGroup.PARTICLE_GROUP);
            }
        }
        if ((set.renderParticleLabels && set.numLabels > 0) || set.renderSetLabel) {
            addToRender(render, RenderGroup.FONT_LABEL);
        }
    }
}
