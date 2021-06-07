/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.comp;

import gaiasky.scenegraph.SceneGraphNode;

import java.util.Comparator;

/**
 * Compares entities. Further entities go first, nearer entities go last.
 */
public class DistToCameraComparator<T> implements Comparator<T> {

    @Override
    public int compare(T o1, T o2) {
        return -Double.compare(((SceneGraphNode) o1).distToCamera, ((SceneGraphNode) o2).distToCamera);
    }

}
