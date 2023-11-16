/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.render.extract;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.render.api.ISceneRenderer;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.Octree;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.util.Settings;
import gaiasky.util.tree.OctreeNode;

import static gaiasky.render.RenderGroup.LINE;

public class OctreeExtractor extends AbstractExtractSystem {

    private final ParticleSetExtractor particleSetExtractor;

    public OctreeExtractor(Family family, int priority) {
        super(family, priority);
        this.particleSetExtractor = new ParticleSetExtractor(null, 0);
    }

    @Override
    public void setRenderer(ISceneRenderer renderer) {
        super.setRenderer(renderer);
        particleSetExtractor.setRenderer(renderer);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var root = Mapper.octant.get(entity);
        var octree = Mapper.octree.get(entity);

        if (base.isVisible() && !base.copy) {
            // Extract objects.
            extractParticleSet(octree);

            // Clear roulette.
            octree.roulette.clear();

            // Extract octree nodes themselves (render octree wireframes).
            addToRenderLists(base, root.octant, camera);
        }
    }

    /**
     * Extracts all observed octree objects.
     */
    private void extractParticleSet(Octree octree) {
        int size = octree.roulette.size();
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            particleSetExtractor.extractEntity(entity);
        }
    }

    public void addToRenderLists(Base base, OctreeNode octant, ICamera camera) {
        if (mustRender(base) && Settings.settings.runtime.drawOctree) {
            boolean added = addToRender(octant, LINE);

            if (added) {
                for (int i = 0; i < 8; i++) {
                    OctreeNode child = octant.children[i];
                    if (child != null) {
                        addToRenderLists(base, child, camera);
                    }
                }
            }
        }
    }
}
