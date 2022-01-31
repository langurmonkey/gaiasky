/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Sort;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Defines the interface for any scene graph implementation
 */
public interface ISceneGraph extends Disposable {
    /**
     * Initializes the scene graph
     *
     * @param nodes        The list of nodes
     * @param time         The time provider
     * @param hasOctree    Whether the list of nodes contains an octree
     * @param hasStarGroup Whether the list contains a star group
     */
    void initialize(Array<SceneGraphNode> nodes, ITimeFrameProvider time, boolean hasOctree, boolean hasStarGroup);

    /**
     * Inserts a node
     *
     * @param node       The node to add
     * @param addToIndex Whether to add the ids of this node to the index
     */
    void insert(SceneGraphNode node, boolean addToIndex);

    /**
     * Removes a node
     *
     * @param node            The node to remove
     * @param removeFromIndex Whether to remove the ids of this node from the index
     */
    void remove(SceneGraphNode node, boolean removeFromIndex);

    /**
     * Updates the nodes of this scene graph
     *
     * @param time   The current time provider
     * @param camera The current camera
     */
    void update(ITimeFrameProvider time, ICamera camera);

    /**
     * Returns focusable nodes matching the given string, to a maximum
     * of 10.
     * @param str The name.
     * @param results The results.
     */
    void matchingFocusableNodes(String str, SortedSet<String> results);

    /**
     * Returns focusable nodes matching the given string, to a maximum
     * of <code>maxResults</code>.
     * @param str The name.
     * @param results The results.
     * @param maxResults The maximum number of results.
     * @param abort To enable abortion mid-computation.
     */
    void matchingFocusableNodes(String str, SortedSet<String> results, int maxResults, AtomicBoolean abort);
    /**
     * Whether this scene graphs contains a node with the given name
     *
     * @param name The name
     * @return True if this scene graph contains the node, false otherwise
     */
    boolean containsNode(String name);

    /**
     * Returns the node with the given name, or null if it does not exist.
     *
     * @param name The name of the node.
     * @return The node with the name.
     */
    SceneGraphNode getNode(String name);

    /**
     * Updates the string to node map and the star map if necessary.
     *
     * @param node The node to add
     */
    void addNodeAuxiliaryInfo(SceneGraphNode node);

    /**
     * Removes the info of the node from the aux lists.
     *
     * @param node The node to remove
     */
    void removeNodeAuxiliaryInfo(SceneGraphNode node);

    /**
     * Gets a star map: HIP -&gt; IPosition It only contains the stars with HIP
     * number
     *
     * @return The HIP star map
     */
    Map<Integer, IPosition> getStarMap();

    Array<SceneGraphNode> getNodes();

    SceneGraphNode getRoot();

    Array<IFocus> getFocusableObjects();

    IFocus findFocus(String name);

    int getSize();

    /**
     * Gets the current position of the object identified by the given name.
     * The given position is in the internal reference system and corrects stars
     * for proper motions and other objects for their specific motions as well.
     *
     * @param name The name of the object
     * @param out  The out double array
     * @return The out double array if the object exists, has a position and out has 3 or more
     * slots. Null otherwise.
     */
    double[] getObjectPosition(String name, double[] out);

}
