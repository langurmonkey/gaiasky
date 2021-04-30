/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scenegraph;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.GaiaSky;
import gaiasky.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaiasky.scenegraph.particle.IParticleRecord;
import gaiasky.util.I18n;
import gaiasky.util.Logger;
import gaiasky.util.Logger.Log;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.time.ITimeFrameProvider;
import gaiasky.util.tree.IPosition;

import java.util.*;

public abstract class AbstractSceneGraph implements ISceneGraph {
    private static final Log logger = Logger.getLogger(AbstractSceneGraph.class);

    /** The root of the tree **/
    public SceneGraphNode root;
    /** Quick lookup map. Name to node. **/
    protected ObjectMap<String, SceneGraphNode> stringToNode;
    /**
     * Map from integer to position with all Hipparcos stars, for the
     * constellations
     **/
    protected ObjectMap<Integer, IPosition> hipMap;
    /** Number of objects per thread **/
    protected int[] objectsPerThread;
    /** Does it contain an octree **/
    protected boolean hasOctree;
    /** Does it contain a star group **/
    protected boolean hasStarGroup;

    private final Vector3d aux3d1;
    private final Vector3b aux3b1;

    public AbstractSceneGraph() {
        // Id = -1 for root
        root = new SceneGraphNode(-1);
        root.names = new String[] { SceneGraphNode.ROOT_NAME };

        // Objects per thread
        objectsPerThread = new int[1];

        aux3d1 = new Vector3d();
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
        logger.info(I18n.bundle.format("notif.sg.insert", nodes.size));

        // Set the reference
        SceneGraphNode.sg = this;

        // Octree
        this.hasOctree = hasOctree;
        // Star group
        this.hasStarGroup = hasStarGroup;

        // Initialize stringToNode and starMap maps
        stringToNode = new ObjectMap<>(nodes.size);
        stringToNode.put(root.names[0].toLowerCase().trim(), root);
        hipMap = new ObjectMap<>();
        for (SceneGraphNode node : nodes) {
            addToIndex(node, stringToNode);

            // Unwrap octree objects
            if (node instanceof AbstractOctreeWrapper) {
                AbstractOctreeWrapper ow = (AbstractOctreeWrapper) node;
                if (ow.children != null)
                    for (SceneGraphNode ownode : ow.children) {
                        addToIndex(ownode, stringToNode);
                    }
            }

            // Star map            
            addToHipMap(node);
        }

        // Insert all the nodes
        for (SceneGraphNode node : nodes) {
            insert(node, false);
        }

        logger.info(I18n.bundle.format("notif.sg.init", root.numChildren));
    }

    public void insert(SceneGraphNode node, boolean addToIndex) {
        SceneGraphNode parent = getNode(node.parentName);
        if (addToIndex) {
            addToIndex(node, stringToNode);
        }
        if (parent != null) {
            parent.addChild(node, true);
            node.setUp();
        } else {
            throw new RuntimeException("Parent of node " + node.names[0] + " not found: " + node.parentName);
        }
    }

    public void remove(SceneGraphNode node, boolean removeFromIndex) {
        if (node != null && node.parent != null) {
            node.parent.removeChild(node, true);
        } else {
            throw new RuntimeException("Given node is null");
        }
        if (removeFromIndex) {
            removeFromIndex(node, stringToNode);
        }
    }

    private void addToHipMap(SceneGraphNode node) {
        if (node instanceof AbstractOctreeWrapper) {
            AbstractOctreeWrapper aow = (AbstractOctreeWrapper) node;
            Set<SceneGraphNode> set = aow.parenthood.keySet();
            for (SceneGraphNode ape : set)
                addToHipMap(ape);
        } else {
            if (node instanceof CelestialBody) {
                CelestialBody s = (CelestialBody) node;
                if (s instanceof Star && ((Star) s).hip > 0) {
                    if (hipMap.containsKey(((Star) s).hip)) {
                        logger.debug("Duplicated HIP id: " + ((Star) s).hip);
                    } else {
                        hipMap.put(((Star) s).hip, s);
                    }
                }
            } else if (node instanceof StarGroup) {
                List<IParticleRecord> stars = ((StarGroup) node).data();
                for (IParticleRecord pb : stars) {
                    IParticleRecord s = pb;
                    if (s.hip() > 0) {
                        hipMap.put(s.hip(), new Position(s.x(), s.y(), s.z(), s.pmx(), s.pmy(), s.pmz()));
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
                        IParticleRecord sb = pb;
                        if (sb != null && sb.hip() >= 0)
                            hipMap.remove(sb.hip());
                    }
                }
            }
        }
    }

    protected void addToIndex(SceneGraphNode node, ObjectMap<String, SceneGraphNode> map) {
        synchronized (map) {
            if (node.names != null) {
                if (node.mustAddToIndex()) {
                    for (String name : node.names) {
                        String namelc = name.toLowerCase().trim();
                        if (!map.containsKey(namelc)) {
                            map.put(namelc, node);
                        } else if (!namelc.isEmpty()) {
                            SceneGraphNode conflict = map.get(namelc);
                            logger.warn("Name conflict: " + name + " (" + node.getClass().getSimpleName().toLowerCase() + ") conflicts with " + conflict.getName() + " (" + conflict.getClass().getSimpleName().toLowerCase() + ")");
                        }
                    }

                    // Id
                    if (node.id > 0) {
                        String id = String.valueOf(node.id);
                        map.put(id, node);
                    }
                }

                // Special cases
                node.addToIndex(map);
            }
        }
    }

    private void removeFromIndex(SceneGraphNode node, ObjectMap<String, SceneGraphNode> map) {
        synchronized(map) {
            if (node.names != null) {
                for (String name : node.names) {
                    map.remove(name.toLowerCase().trim());
                }

                // Id
                if (node.id > 0) {
                    String id = String.valueOf(node.id);
                    map.remove(id);
                }

                // Special cases
                node.removeFromIndex(map);
            }
        }
    }

    public synchronized void addNodeAuxiliaryInfo(SceneGraphNode node) {
        synchronized (stringToNode) {
            // Name index
            addToIndex(node, stringToNode);
            // Star map
            addToHipMap(node);
        }
    }

    public synchronized void removeNodeAuxiliaryInfo(SceneGraphNode node) {
        synchronized (stringToNode) {
            // Name index
            removeFromIndex(node, stringToNode);
            // Star map
            removeFromHipMap(node);
        }
    }

    public void matchingFocusableNodes(String name, Array<String> results) {
        matchingFocusableNodes(name, results, 10);
    }

    public void matchingFocusableNodes(String name, Array<String> results, int maxResults) {
        synchronized (stringToNode) {
            ObjectMap.Keys<String> keys = new ObjectMap.Keys<>(stringToNode);
            name = name.toLowerCase().trim();

            int i = 0;
            // Starts with
            for (String key : keys) {
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
        synchronized(stringToNode) {
            return stringToNode.containsKey(name.toLowerCase().trim());
        }
    }

    public SceneGraphNode getNode(String name) {
        synchronized (stringToNode) {
            name = name.toLowerCase().strip();
            SceneGraphNode node = stringToNode.get(name);
            if (node != null && node instanceof StarGroup)
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
        if (node == null || !(node instanceof IFocus))
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
            ISceneGraph sg = GaiaSky.instance.sg;
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
