package gaia.cu9.ari.gaiaorbit.scenegraph;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.IPosition;

/**
 * Defines the interface for any scene graph implementation
 *
 * @author tsagrista
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
     * Gets the index from string to node
     *
     * @return The index
     */
    ObjectMap<String, SceneGraphNode> getStringToNodeMap();

    /**
     * Gets a star map: HIP -&gt; IPosition It only contains the stars with HIP
     * number
     *
     * @return The HIP star map
     */
    IntMap<IPosition> getStarMap();

    Array<SceneGraphNode> getNodes();

    SceneGraphNode getRoot();

    Array<IFocus> getFocusableObjects();

    IFocus findFocus(String name);

    int getSize();

    /**
     * Adds the given node to the index with the given key
     *
     * @param key  The string key
     * @param node The node
     */
    void addToStringToNode(String key, SceneGraphNode node);

    /**
     * Removes the object with the given key from the index
     *
     * @param key The key to remove
     */
    void removeFromStringToNode(String key);

    /**
     * Removes the given object from the index. This operation may take a while
     *
     * @param node The node to remove
     */
    void removeFromStringToNode(SceneGraphNode node);

    /**
     * Gets the current position of the object identified by the given name.
     * The given position is in the internal reference system and corrects stars
     * for proper motions and other objects for their specific motions as well.
     *
     * @param name The name of the object
     * @return The current position, if the object exists and has a position. Null otherwise.
     */
    double[] getObjectPosition(String name);

    /**
     * Same as {@link ISceneGraph#getObjectPosition(String)} but passing a doulbe array
     * of at least 3 slots to store the result.
     *
     * @param name The name of the object
     * @param out  The out double array
     * @return The out double array if the object exists, has a position and out has 3 or more
     * slots. Null otherwise.
     */
    double[] getObjectPosition(String name, double[] out);

}
