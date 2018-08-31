package gaia.cu9.ari.gaiaorbit.scenegraph;

import java.util.Set;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;
import com.badlogic.gdx.utils.ObjectSet;

import gaia.cu9.ari.gaiaorbit.render.system.PixelRenderSystem;
import gaia.cu9.ari.gaiaorbit.scenegraph.StarGroup.StarBean;
import gaia.cu9.ari.gaiaorbit.scenegraph.camera.ICamera;
import gaia.cu9.ari.gaiaorbit.scenegraph.octreewrapper.AbstractOctreeWrapper;
import gaia.cu9.ari.gaiaorbit.util.GlobalConf;
import gaia.cu9.ari.gaiaorbit.util.I18n;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;
import gaia.cu9.ari.gaiaorbit.util.time.ITimeFrameProvider;
import gaia.cu9.ari.gaiaorbit.util.tree.IPosition;

public abstract class AbstractSceneGraph implements ISceneGraph {
    private static Log logger = Logger.getLogger(AbstractSceneGraph.class);
    
    /** The root of the tree **/
    public SceneGraphNode root;
    /** Quick lookup map. Name to node. **/
    ObjectMap<String, SceneGraphNode> stringToNode;
    /**
     * Map from integer to position with all hipparcos stars, for the
     * constellations
     **/
    IntMap<IPosition> hipMap;
    /** Number of objects per thread **/
    protected int[] objectsPerThread;
    /** Does it contain an octree **/
    protected boolean hasOctree;
    /** Does it contain a star group **/
    protected boolean hasStarGroup;

    public AbstractSceneGraph() {
        // Id = -1 for root
        root = new SceneGraphNode(-1);
        root.name = SceneGraphNode.ROOT_NAME;

        // Objects per thread
        objectsPerThread = new int[1];
    }

    /**
     * Builds the scene graph using the given nodes.
     * 
     * @param nodes
     *            The list of nodes
     * @param time
     *            The time provider
     * @param hasOctree
     *            Whether the list of nodes contains an octree
     * @param hasStarGroup
     *            Whether the list contains a star group
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
        stringToNode = new ObjectMap<String, SceneGraphNode>(nodes.size * 2);
        stringToNode.put(root.name, root);
        hipMap = new IntMap<IPosition>();
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
        SceneGraphNode parent = stringToNode.get(node.parentName);
        if (parent != null) {
            parent.addChild(node, true);
            node.setUp();

            if (addToIndex) {
                addToIndex(node, stringToNode);
            }
        } else {
            throw new RuntimeException("Parent of node " + node.name + " not found: " + node.parentName);
        }
    }

    public void remove(SceneGraphNode node, boolean removeFromIndex) {
        if (node != null && node.parent != null) {
            node.parent.removeChild(node, true);

            if (removeFromIndex) {
                removeFromIndex(node, stringToNode);
            }
        } else {
            throw new RuntimeException("Given node is null");
        }
    }

    private void addToHipMap(SceneGraphNode node) {
        if (node instanceof AbstractOctreeWrapper) {
            AbstractOctreeWrapper aow = (AbstractOctreeWrapper) node;
            Set<AbstractPositionEntity> set = aow.parenthood.keySet();
            for (AbstractPositionEntity ape : set)
                addToHipMap(ape);
        } else {
            if (node instanceof CelestialBody) {
                CelestialBody s = (CelestialBody) node;
                if (s instanceof Star && ((Star) s).hip > 0) {
                    if (hipMap.containsKey(((Star) s).hip)) {
                        logger.debug("Duplicated HIP id: " + ((Star) s).hip);
                    } else {
                        hipMap.put(((Star) s).hip, (Star) s);
                    }
                }
            } else if (node instanceof StarGroup) {
                Array<StarBean> stars = ((StarGroup) node).data();
                for (StarBean s : stars) {
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
            Set<AbstractPositionEntity> set = aow.parenthood.keySet();
            for (AbstractPositionEntity ape : set)
                removeFromHipMap(ape);
        } else {
            if (node instanceof CelestialBody) {
                CelestialBody s = (CelestialBody) node;
                if (s instanceof Star && ((Star) s).hip >= 0) {
                    hipMap.remove(((Star) s).hip);
                }
            } else if (node instanceof StarGroup) {
                StarGroup sg = (StarGroup) node;
                for (StarBean sb : sg.data()) {
                    if (sb.hip() >= 0)
                        hipMap.remove(sb.hip());
                }
            }
        }
    }

    private void addToIndex(SceneGraphNode node, ObjectMap<String, SceneGraphNode> map) {
        if (node.name != null && !node.name.isEmpty()) {
            map.put(node.name, node);
            map.put(node.name.toLowerCase(), node);

            // Id
            if (node.id > 0) {
                String id = String.valueOf(node.id);
                map.put(id, node);
            }

            if (node instanceof Star) {
                // Hip
                if (((Star) node).hip > 0) {
                    String hipid = "hip " + ((Star) node).hip;
                    map.put(hipid, node);
                }
                // Tycho
                if (((Star) node).tycho != null && !((Star) node).tycho.isEmpty()) {
                    map.put(((Star) node).tycho, node);
                }

            } else if (node instanceof StarGroup) {
                StarGroup sg = (StarGroup) node;
                if (sg.index != null) {
                    ObjectIntMap.Keys<String> keys = sg.index.keys();
                    for (String key : keys) {
                        map.put(key, sg);
                    }
                }
            }

        }
    }

    private void removeFromIndex(SceneGraphNode node, ObjectMap<String, SceneGraphNode> map) {
        if (node.name != null && !node.name.isEmpty()) {
            map.remove(node.name);
            map.remove(node.name.toLowerCase());

            // Id
            if (node.id > 0) {
                String id = String.valueOf(node.id);
                map.remove(id);
            }

            if (node instanceof Star) {
                // Hip
                if (((Star) node).hip > 0) {
                    String hipid = "hip " + ((Star) node).hip;
                    map.remove(hipid);
                }

                // Tycho
                if (((Star) node).tycho != null && !((Star) node).tycho.isEmpty()) {
                    map.remove(((Star) node).tycho);
                }
            } else if (node instanceof StarGroup) {
                StarGroup sg = (StarGroup) node;
                if (sg.index != null) {
                    ObjectIntMap.Keys<String> keys = sg.index.keys();
                    for (String key : keys) {
                        map.remove(key);
                    }
                }
            }

        }
    }

    public synchronized void addToStringToNode(String key, SceneGraphNode node) {
        stringToNode.put(key, node);
    }

    public synchronized void removeFromStringToNode(String key) {
        stringToNode.remove(key);
    }

    public synchronized void removeFromStringToNode(SceneGraphNode node) {
        Keys<String> keys = stringToNode.keys();
        ObjectSet<String> hits = new ObjectSet<String>();
        for (String key : keys) {
            if (stringToNode.get(key) == node)
                hits.add(key);
        }
        for (String key : hits)
            stringToNode.remove(key);
    }

    @Override
    public void update(ITimeFrameProvider time, ICamera camera) {
        // Check if we need to update the points
        if (GlobalConf.scene.COMPUTE_GAIA_SCAN && time.getDt() != 0) {
            PixelRenderSystem.POINT_UPDATE_FLAG = true;
        }
    }

    public ObjectMap<String, SceneGraphNode> getStringToNodeMap() {
        return stringToNode;
    }

    public synchronized void addNodeAuxiliaryInfo(SceneGraphNode node) {
        // Name index
        addToIndex(node, stringToNode);
        // Star map
        addToHipMap(node);
    }

    public synchronized void removeNodeAuxiliaryInfo(SceneGraphNode node) {
        // Name index
        removeFromIndex(node, stringToNode);
        // Star map
        removeFromHipMap(node);
    }

    public boolean containsNode(String name) {
        return stringToNode.containsKey(name);
    }

    public SceneGraphNode getNode(String name) {
        //return root.getNode(name);
        SceneGraphNode node = stringToNode.get(name);
        if (node instanceof StarGroup)
            ((StarGroup) node).getFocus(name);
        return node;
    }

    public Array<SceneGraphNode> getNodes() {
        Array<SceneGraphNode> objects = new Array<SceneGraphNode>();
        root.addNodes(objects);
        return objects;
    }

    public Array<IFocus> getFocusableObjects() {
        Array<IFocus> objects = new Array<IFocus>();
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
    public IntMap<IPosition> getStarMap() {
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
                    n += ((StarGroup) sgn).getStarCount();
            }
            return n;
        }
    }

}
