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
import gaiasky.scene.Scene;
import gaiasky.scene.component.GraphNode;
import gaiasky.util.i18n.I18n;

public class SceneGraphBuilderSystem extends AbstractInitSystem {

    /** The scene. **/
    private final Scene scene;

    public SceneGraphBuilderSystem(Scene scene, Family family, int priority) {
        super(false, family, priority);
        this.scene = scene;
    }

    @Override
    public void initializeEntity(Entity entity) {
        var graph = entity.getComponent(GraphNode.class);
        if (graph.parentName != null) {
            // Look for entity with parentName in the scene. Must be in index.
            var parent = scene.getEntity(graph.parentName);
            if (parent != null) {
                // Index hit, just add.
                addChild(parent, entity, true);
            } else {
                // We did not find it in the index. Traverse the scene instead.
                parent = scene.getNonIndexEntity(graph.parentName);
                if (parent != null) {
                    addChild(parent, entity, true);
                } else {
                    logger.error(I18n.msg("error.parent.notfound", Mapper.base.get(entity).getName(), graph.parentName));
                }
            }
        }

    }

    @Override
    public void setUpEntity(Entity entity) {

    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * node and in all ancestors.
     *
     * @param child               The child node to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void addChild(Entity parent, Entity child, boolean updateAncestorCount) {
        var parentGraph = Mapper.graph.get(parent);
        parentGraph.addChild(parent, child, updateAncestorCount, parentGraph.parent == null ? 100 : 1);
    }

    /**
     * Adds the given {@link Entity} list as children to this node.
     *
     * @param children The children nodes to add.
     */
    public final void add(Entity parent, Entity... children) {
        var parentGraph = Mapper.graph.get(parent);
        if (parentGraph.children == null) {
            parentGraph.initChildren(parentGraph.parent == null || Mapper.octree.has(parent) ? 300000 : children.length * 5);
        }
        for (Entity child : children) {
            var graph = Mapper.graph.get(child);
            parentGraph.children.add(child);
            graph.parent = parent;
        }
        parentGraph.numChildren += children.length;
    }
}
