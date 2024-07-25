/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.component;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import gaiasky.util.tree.IOctreeObject;
import gaiasky.util.tree.OctreeNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Octree implements Component {

    /** The list with the currently observed objects. **/
    public List<IOctreeObject> roulette;

    /** Map with the parent for each node. **/
    public Map<Entity, OctreeNode> parenthood;

    /** Is this just a copy? */
    public boolean copy = false;

    /** Creates an empty octree. **/
    public Octree() {
        this.parenthood = new ConcurrentHashMap<>();
    }

    public void removeParenthood(Entity child) {
        if (child != null) {
            parenthood.remove(child);
        }
    }
}
