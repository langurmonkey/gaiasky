/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.system.update;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.component.Base;
import gaiasky.scene.component.GraphNode;
import gaiasky.scene.component.tag.TagNoProcessChildren;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A subtree that is no longer traversed keeps the opacity and the distance it last had, and both the
 * extraction pass and the closest-body query read those fields. The bug this covers: the members of a
 * faded-out dataset stayed "visible" (opacity last seen as non-zero) and kept competing for the
 * closest body with a frozen distance, which pinned the camera speed scaling to a constant no matter
 * where the camera was.
 */
public class HideSubtreeTest {

    private static Entity node(float opacity,
                               Entity... children) {
        var entity = new Entity();
        var base = new Base();
        base.opacity = opacity;
        entity.add(base);
        var graph = new GraphNode();
        if (children.length > 0) {
            graph.children = new Array<>(children);
        }
        entity.add(graph);
        return entity;
    }

    private static float opacity(Entity entity) {
        return entity.getComponent(Base.class).opacity;
    }

    @Test
    public void hidesTheWholeChain() {
        var grandChild = node(0.3f);
        var child = node(0.3f, grandChild);
        var root = node(0.3f, child);

        GraphUpdater.hideSubtree(root, 0f);

        assertEquals(0f, opacity(root), 0f);
        assertEquals(0f, opacity(child), 0f);
        assertEquals(0f, opacity(grandChild), 0f);
    }

    @Test
    public void isIdempotent() {
        var child = node(0.2f);
        var root = node(0.2f, child);

        GraphUpdater.hideSubtree(root, 0f);
        GraphUpdater.hideSubtree(root, 0f);

        assertEquals(0f, opacity(root), 0f);
        assertEquals(0f, opacity(child), 0f);
    }

    @Test
    public void doesNotDescendIntoSetsThatProcessTheirOwnChildren() {
        // A particle group's children are octree nodes, managed by their own systems.
        var octant = node(0.5f);
        var group = node(0.5f, octant);
        group.add(new TagNoProcessChildren());

        GraphUpdater.hideSubtree(group, 0f);

        assertEquals(0f, opacity(group), 0f);
        assertEquals(0.5f, opacity(octant), 0f);
    }

    @Test
    public void hidesALeaf() {
        var leaf = node(1f);

        GraphUpdater.hideSubtree(leaf, 0f);

        assertEquals(0f, opacity(leaf), 0f);
    }
}
