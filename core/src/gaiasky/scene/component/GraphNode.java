/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import gaiasky.scene.Mapper;
import gaiasky.scene.system.update.GraphUpdater;
import gaiasky.util.Consumers.Consumer4;
import gaiasky.util.Functions.Function3;
import gaiasky.util.math.Matrix4d;
import gaiasky.util.math.Vector3b;

public class GraphNode implements Component, ICopy {

    /** Reference to the method to update the position of this object. **/
    public Consumer4<GraphUpdater, Entity, Body, GraphNode> positionUpdaterConsumer = GraphUpdater::updatePositionDefault;

    /** Reference to the function that returns whether this object must be updated. **/
    public Function3<GraphUpdater, Entity, GraphNode, Boolean> mustUpdateFunction;

    /**
     * The first name of the parent object.
     */
    public String parentName = null;

    /**
     * The parent entity.
     */
    public Entity parent;

    /**
     * List of children entities.
     */
    public Array<Entity> children;

    /**
     * Cumulative translation object. In contrast to the position in {@link Body#pos}, which contains
     * the position relative to the parent, this contains the absolute position in the
     * floating camera reference system. Add the camera position to this to get the
     * absolute position in the internal reference system.
     */
    public Vector3b translation;

    /**
     * Local transform matrix. Contains the transform matrix and the
     * transformations that will be applied to this object and not to its
     * children.
     */
    public Matrix4 localTransform;

    /**
     * This transform stores only the orientation of the object. For example in
     * planets, it stores their orientation with respect to their equatorial
     * plane, but not other transformations applied to the object such as the
     * size or the rotation angle at the time.
     */
    public Matrix4d orientation;

    /**
     * The total number of descendants under this node.
     */
    public int numChildren;

    /**
     * Sets the name of the parent.
     *
     * @param parentName The parent name.
     */
    public void setParent(String parentName) {
        this.parentName = parentName;
    }

    public int getSceneGraphDepth() {
        if (this.parent == null) {
            return 0;
        } else {
            return Mapper.graph.get(this.parent).getSceneGraphDepth() + 1;
        }
    }

    public void initChildren(int size) {
        children = new Array<>(false, size);
    }

    /**
     * Adds a child to the given node and updates the number of children in this
     * entity and in all ancestors.
     *
     * @param me                  The current entity that owns this graph node.
     * @param child               The child entity to add.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     * @param numChildren         The number of children this will hold.
     */
    public final void addChild(Entity me, Entity child, boolean updateAncestorCount, int numChildren) {
        if (this.children == null) {
            initChildren(numChildren);
        }
        this.children.add(child);
        var childGraph = Mapper.graph.get(child);
        childGraph.parent = me;
        this.numChildren++;

        if (updateAncestorCount) {
            // Update num children in ancestors
            Entity ancestor = this.parent;
            while (ancestor != null) {
                var ancestorGraph = Mapper.graph.get(ancestor);
                ancestorGraph.numChildren++;
                ancestor = ancestorGraph.parent;
            }
        }
    }

    /**
     * Removes the given child from this node, if it exists.
     *
     * @param child               The child node to remove.
     * @param updateAncestorCount Whether to update the ancestors number of children.
     */
    public final void removeChild(Entity child, boolean updateAncestorCount) {
        if (this.children.contains(child, true)) {
            this.children.removeValue(child, true);
            var childGraph = Mapper.graph.get(child);
            childGraph.parent = null;
            numChildren--;
            if (updateAncestorCount) {
                // Update num children in ancestors
                Entity ancestor = this.parent;
                while (ancestor != null) {
                    var ancestorGraph = Mapper.graph.get(ancestor);
                    ancestorGraph.numChildren--;
                    ancestor = ancestorGraph.parent;
                }
            }
        }
    }

    public Entity getRoot(Entity me) {
        if (this.parent == null) {
            return me;
        } else {
            var parentGraph = Mapper.graph.get(this.parent);
            if (parentGraph == null) {
                return this.parent;
            } else {
                return parentGraph.getRoot(this.parent);
            }
        }
    }

    @Override
    public Component getCopy(Engine engine) {
        var copy = engine.createComponent(this.getClass());
        copy.parent = null;
        copy.parentName = parentName;
        copy.translation = new Vector3b(translation);
        if (copy.children != null) {
            copy.children.clear();
        }

        if (localTransform != null) {
            if (copy.localTransform == null) {
                copy.localTransform = new Matrix4();
            }
            copy.localTransform.set(localTransform);
        }
        if (orientation != null) {
            if (copy.orientation == null) {
                copy.orientation = new Matrix4d();
            }
            copy.orientation.set(orientation);
        }
        return copy;
    }

    public Entity getChildByName(String name) {
        int size = children.size;
        for (int i = 0; i < size; i++) {
            Entity child = children.get(i);
            if (child != null) {
                var base = Mapper.base.get(child);
                if (base.getName().equalsIgnoreCase(name)) {
                    return child;
                }
            }
        }
        return null;
    }
}
