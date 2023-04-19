/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import gaiasky.GaiaSky;
import gaiasky.scene.Mapper;
import gaiasky.scene.Scene;
import gaiasky.scene.api.IFocus;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.Octree;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.OctreeNode;

public class OctreeUpdater extends AbstractUpdateSystem {

    private final GraphUpdater graphUpdater;
    private final ParticleSetUpdater particleSetUpdater;

    private final Scene scene;

    public OctreeUpdater(Scene scene, Family family, int priority) {
        super(family, priority);

        this.scene = scene;
        this.graphUpdater = new GraphUpdater(null, 0, GaiaSky.instance.time);
        this.particleSetUpdater = new ParticleSetUpdater(null, 0);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        updateEntity(entity, deltaTime);
    }

    @Override
    public void updateEntity(Entity entity, float deltaTime) {
        var base = Mapper.base.get(entity);
        var graph = Mapper.graph.get(entity);
        var root = Mapper.octant.get(entity);
        var octree = Mapper.octree.get(entity);

        // Fade node visibility applies here
        if (base.isVisible()) {
            // Update octants
            if (!base.copy) {

                // Compute observed octants and fill roulette list
                OctreeNode.nOctantsObserved = 0;
                OctreeNode.nObjectsObserved = 0;

                ICamera camera = GaiaSky.instance.cameraManager;

                // Update root node
                root.octant.update(graph.translation, camera, octree.roulette, base.opacity);

                // Call the update method of all entities in the roulette list.
                updateOctreeObjects(base, graph, octree, deltaTime);

                // Update focus, just in case
                if (camera.hasFocus()) {
                    IFocus focus = camera.getFocus();
                    Entity star = focus.getFirstStarAncestorEntity();
                    OctreeNode parent = octree.parenthood.get(star);
                    if (parent != null && !parent.isObserved()) {
                        var starGraph = Mapper.graph.get(star);
                        Vector3b starParentTranslation = starGraph.translation;

                        scene.updateEntityGraph(star, GaiaSky.instance.time, starParentTranslation, 1);
                        scene.updateEntity(star, (float) GaiaSky.instance.time.getDt());
                    }
                }
            } else {
                // TODO what is this for?
                // Just update children
                //for (SceneGraphNode node : children) {
                //    node.update(time, translation, camera);
                //}
            }
        }
    }

    /**
     * Updates all observed octree objects.
     */
    protected void updateOctreeObjects(Base base, GraphNode graph, Octree octree, float deltaTime) {
        updateGraph(base, graph, octree, GaiaSky.instance.time);
        updateParticleSet(octree, deltaTime);
    }

    private void updateGraph(Base base, GraphNode graph, Octree octree, ITimeFrameProvider time) {
        int size = octree.roulette.size();
        graphUpdater.setCamera(GaiaSky.instance.cameraManager);
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            // Use octant opacity
            var octant = Mapper.octant.get(entity);
            graphUpdater.update(entity, time, graph.translation, base.opacity * octant.octant.opacity);
        }
    }

    private void updateParticleSet(Octree octree, float deltaTime) {
        int size = octree.roulette.size();
        for (int i = 0; i < size; i++) {
            Entity entity = ((OctreeObjectView) octree.roulette.get(i)).getEntity();
            particleSetUpdater.updateEntity(entity, deltaTime);
        }
    }
}
