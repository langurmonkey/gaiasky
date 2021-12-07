/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.event.EventManager;
import gaiasky.event.Events;
import gaiasky.scenegraph.camera.ICamera;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3b;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SceneGraph implements ISceneGraph {
    private static final Log logger = Logger.getLogger(SceneGraph.class);

    /** The root of the tree **/
    public SceneGraphNode root;
    /** Quick lookup map. Name to node. **/
    protected final ObjectMap<String, SceneGraphNode> stringToNode;
    /**
     * Map from integer to position with all Hipparcos stars, for the
     * constellations
     **/
    protected final ObjectMap<Integer, IPosition> hipMap;
    /** Number of objects per thread **/
    protected int[] objectsPerThread;
    /** Does it contain an octree **/
    protected boolean hasOctree;
    /** Does it contain a star group **/
    protected boolean hasStarGroup;

    // Number of objects
    private int nObjects = -1;

    // Auxiliary vector
    private final Vector3b aux3b1;

    public SceneGraph(int numNodes) {
        // Id = -1 for root
        root = new SceneGraphNode(-1);
        root.names = new String[] { SceneGraphNode.ROOT_NAME };

        // Objects per thread
        objectsPerThread = new int[1];

        // String-to-node map
        stringToNode = new ObjectMap<>(numNodes);
        // HIP map
        hipMap = new ObjectMap<>();

        aux3b1 = new Vector3b();
    }

    /**
     * Builds the scene graph using the given nodes.
     *
     * @param nodes        The list of nodes
     * @param time         The time provider
     * @param hasOctree    Whether the list of nodes contains an octree
     * @param hasStarGroup Whether the list contains a star group
     */
    @Override
    public void initialize(Array<SceneGraphNode> nodes, ITimeFrameProvider time, boolean hasOctree, boolean hasStarGroup) {
        logger.info(I18n.txt("notif.sg.insert", nodes.size));

        // Octree
        this.hasOctree = hasOctree;
        // Star group
        this.hasStarGroup = hasStarGroup;

        // Initialize stringToNode and starMap maps
        stringToNode.put(root.names[0].toLowerCase().trim(), root);
        for (SceneGraphNode node : nodes) {
            addToIndex(node);

            // Unwrap octree objects
            if (node instanceof AbstractOctreeWrapper) {
                AbstractOctreeWrapper ow = (AbstractOctreeWrapper) node;
                if (ow.children != null)
                    for (SceneGraphNode ownode : ow.children) {
                        addToIndex(ownode);
                    }
            }

            // Star map            
            addToHipMap(node);
        }

        // Insert all the nodes
        for (SceneGraphNode node : nodes) {
            insert(node, false);
        }

        logger.info(I18n.txt("notif.sg.init", root.numChildren));
    }

    public void update(ITimeFrameProvider time, ICamera camera) {
        root.translation.set(camera.getInversePos());
        root.update(time, null, camera);
        objectsPerThread[0] = root.numChildren;

        if (!hasOctree) {
            if (nObjects < 0)
                nObjects = getNObjects();
            EventManager.instance.post(Events.DEBUG_OBJECTS, nObjects, nObjects);
        }
    }

    public void insert(SceneGraphNode node, boolean addToIndex) {
        SceneGraphNode parent = getNode(node.parentName);
        boolean ok = true;
        if (addToIndex) {
            ok = addToIndex(node);
        }
        if (!ok) {
            logger.warn(I18n.txt("error.object.exists", node.getName() + "(" + node.getClass().getSimpleName().toLowerCase() +")"));
        } else {
            if (parent != null) {
                parent.addChild(node, true);
                node.setUp(this);
            } else {
                throw new RuntimeException(I18n.txt("error.parent.notfound", node.names[0], node.parentName));
            }
        }
    }

    public void remove(SceneGraphNode node, boolean removeFromIndex) {
        if (node != null && node.parent != null) {
            node.parent.removeChild(node, true);
        } else {
            throw new RuntimeException("Given node is null");
        }
        if (removeFromIndex) {
            removeFromIndex(node);
        }
    }

    private void addToHipMap(SceneGraphNode node) {
        if (node instanceof AbstractOctreeWrapper) {
            AbstractOctreeWrapper aow = (AbstractOctreeWrapper) node;
            Set<SceneGraphNode> set = aow.parenthood.keySet();
            for (SceneGraphNode ape : set)
                addToHipMap(ape);
        } else {
            synchronized (hipMap) {
                if (node instanceof CelestialBody) {
                    CelestialBody s = (CelestialBody) node;
                    if (s instanceof Star && ((Star) s).hip > 0) {
                        if (hipMap.containsKey(((Star) s).hip)) {
                            logger.debug(I18n.txt("error.id.hip.duplicate", ((Star) s).hip));
                        } else {
                            hipMap.put(((Star) s).hip, s);
                        }
                    }
                } else if (node instanceof StarGroup) {
                    List<IParticleRecord> stars = ((StarGroup) node).data();
                    for (IParticleRecord pb : stars) {
                        if (pb.hip() > 0) {
                            hipMap.put(pb.hip(), new Position(pb.x(), pb.y(), pb.z(), pb.pmx(), pb.pmy(), pb.pmz()));
                        }
                    }
                }
            }
        }
    }

    private void removeFromHipMap(SceneGraphNode node) {
        if (node instanceof AbstractOctreeWrapper) {
            AbstractOctreeWrapper aow = (AbstractOctreeWrapper) node;
            Set<SceneGraphNode> set = aow.parenthood.keySet();
            for (SceneGraphNode ape : set)
                removeFromHipMap(ape);
        } else {
            synchronized (hipMap) {
                if (node instanceof CelestialBody) {
                    CelestialBody s = (CelestialBody) node;
                    if (s instanceof Star && ((Star) s).hip >= 0) {
                        hipMap.remove(((Star) s).hip);
                    }
                } else if (node instanceof StarGroup) {
                    StarGroup sg = (StarGroup) node;
                    List<IParticleRecord> arr = sg.data();
                    if (arr != null) {
                        for (IParticleRecord pb : arr) {
                            if (pb != null && pb.hip() >= 0)
                                hipMap.remove(pb.hip());
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds the given node to the index. Returns false if it was not added due to a naming conflict (name already exists)
     * with the same object (same class and same names).
     *
     * @param node The node to add.
     *
     * @return False if the object already exists.
     */
    protected boolean addToIndex(SceneGraphNode node) {
        boolean ok = true;
        synchronized (stringToNode) {
            if (node.names != null) {
                if (node.mustAddToIndex()) {
                    for (String name : node.names) {
                        String nameLowerCase = name.toLowerCase().trim();
                        if (!stringToNode.containsKey(nameLowerCase)) {
                            stringToNode.put(nameLowerCase, node);
                        } else if (!nameLowerCase.isEmpty()) {
                            SceneGraphNode conflict = stringToNode.get(nameLowerCase);
                            logger.debug(I18n.txt("error.name.conflict", name + " (" + node.getClass().getSimpleName().toLowerCase() + ")", conflict.getName() + " (" + conflict.getClass().getSimpleName().toLowerCase() + ")"));
                            String[] names1 = node.getNames();
                            String[] names2 = conflict.getNames();
                            boolean same = names1.length == names2.length;
                            if (same) {
                                for (int i = 0; i < names1.length; i++) {
                                    same = same && names1[i].equals(names2[i]);
                                }
                            }
                            if (same) {
                                same = node.getClass().equals(conflict.getClass());
                            }
                            ok = !same;
                        }
                    }

                    // Id
                    if (node.id > 0) {
                        String id = String.valueOf(node.id);
                        stringToNode.put(id, node);
                    }
                }

                // Special cases
                node.addToIndex(stringToNode);
            }
        }
        return ok;
    }

    private void removeFromIndex(SceneGraphNode node) {
        synchronized (stringToNode) {
            if (node.names != null) {
                for (String name : node.names) {
                    stringToNode.remove(name.toLowerCase().trim());
                }

                // Id
                if (node.id > 0) {
                    String id = String.valueOf(node.id);
                    stringToNode.remove(id);
                }

                // Special cases
                node.removeFromIndex(stringToNode);
            }
        }
    }

    public synchronized void addNodeAuxiliaryInfo(SceneGraphNode node) {
        // Name index
        addToIndex(node);
        // Star map
        addToHipMap(node);
    }

    public synchronized void removeNodeAuxiliaryInfo(SceneGraphNode node) {
        // Name index
        removeFromIndex(node);
        // Star map
        removeFromHipMap(node);
    }

    public void matchingFocusableNodes(String name, Array<String> results) {
        matchingFocusableNodes(name, results, 10, null);
    }

    public void matchingFocusableNodes(String name, Array<String> results, int maxResults, AtomicBoolean abort) {
        synchronized (stringToNode) {
            ObjectMap.Keys<String> keys = new ObjectMap.Keys<>(stringToNode);
            name = name.toLowerCase().trim();

            int i = 0;
            // Starts with
            for (String key : keys) {
                if (abort != null && abort.get())
                    return;
                SceneGraphNode sgn = stringToNode.get(key);
                if (sgn instanceof IFocus && key.startsWith(name)) {
                    results.add(key);
                    i++;
                }
                if (i >= maxResults)
                    return;
            }
            // Contains
            for (String key : keys) {
                if (abort != null && abort.get())
                    return;
                SceneGraphNode sgn = stringToNode.get(key);
                if (sgn instanceof IFocus && key.contains(name)) {
                    results.add(key);
                    i++;
                }
                if (i >= maxResults)
                    return;
            }
        }
    }

    public boolean containsNode(String name) {
        synchronized (stringToNode) {
            return stringToNode.containsKey(name.toLowerCase().trim());
        }
    }

    public SceneGraphNode getNode(String name) {
        synchronized (stringToNode) {
            name = name.toLowerCase().strip();
            SceneGraphNode node = stringToNode.get(name);
            if (node instanceof StarGroup)
                ((StarGroup) node).getFocus(name);
            return node;
        }
    }

    public Array<SceneGraphNode> getNodes() {
        Array<SceneGraphNode> objects = new Array<>();
        root.addNodes(objects);
        return objects;
    }

    public Array<IFocus> getFocusableObjects() {
        Array<IFocus> objects = new Array<>();
        root.addFocusableObjects(objects);
        return objects;
    }

    public IFocus findFocus(String name) {
        SceneGraphNode node = getNode(name);
        if (!(node instanceof IFocus))
            return null;
        else
            return (IFocus) node;
    }

    public int getSize() {
        return root.getAggregatedChildren();
    }

    public void dispose() {
        root.dispose();
    }

    @Override
    public SceneGraphNode getRoot() {
        return root;
    }

    @Override
    public ObjectMap<Integer, IPosition> getStarMap() {
        return hipMap;
    }

    public int getNObjects() {
        if (!hasStarGroup) {
            return root.numChildren;
        } else {
            int n = root.numChildren - 1;
            // This assumes the star group is in the first level of the scene graph, right below universe
            for (SceneGraphNode sgn : root.children) {
                if (sgn instanceof StarGroup)
                    n += sgn.getStarCount();
            }
            return n;
        }
    }

    @Override
    public double[] getObjectPosition(String name, double[] out) {
        if (out.length >= 3 && name != null) {
            name = name.toLowerCase().trim();
            ISceneGraph sg = GaiaSky.instance.sceneGraph;
            if (sg.containsNode(name)) {
                SceneGraphNode object = sg.getNode(name);
                if (object instanceof IFocus) {
                    IFocus obj = (IFocus) object;
                    obj.getAbsolutePosition(name, aux3b1);
                    out[0] = aux3b1.x.doubleValue();
                    out[1] = aux3b1.y.doubleValue();
                    out[2] = aux3b1.z.doubleValue();
                    return out;
                }
            }
        }
        return null;
    }
}
