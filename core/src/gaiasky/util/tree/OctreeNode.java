/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.util.tree;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Array;
import gaiasky.data.api.IOctantLoader;
import gaiasky.render.ComponentTypes;
import gaiasky.render.ComponentTypes.ComponentType;
import gaiasky.render.api.ILineRenderable;
import gaiasky.render.system.LineRenderSystem;
import gaiasky.scene.Mapper;
import gaiasky.scene.camera.ICamera;
import gaiasky.scene.view.OctreeObjectView;
import gaiasky.util.Pair;
import gaiasky.util.Settings;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector3b;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import net.jafama.FastMath;

import java.util.*;

public class OctreeNode implements ILineRenderable {
    /**
     * Since OctreeNode is not to be parallelized, these can be static.
     **/
    private static final Vector3d auxD1 = new Vector3d();
    private static final Vector3d auxD2 = new Vector3d();
    private static final Vector3d auxD3 = new Vector3d();
    private static final Vector3d auxD4 = new Vector3d();
    public static int nOctantsObserved = 0;
    public static int nObjectsObserved = 0;
    /**
     * Max depth of the structure this node belongs to.
     **/
    public static int maxDepth;
    /**
     * Contains the bottom-left-front position of the octant.
     **/
    public final Vector3d min;
    /**
     * Contains the top-right-back position of the octant.
     **/
    public final Vector3d max;
    /**
     * The centre of this octant.
     **/
    public final Vector3d centre;
    /**
     * Octant size in x, y and z.
     **/
    public final Vector3d size;
    /**
     * Contains the depth level.
     **/
    public final int depth;
    private final double radius;
    /**
     * The unique page identifier.
     **/
    public long pageId;
    /**
     * Number of objects contained in this node and its descendants.
     **/
    public int numObjectsRec;
    /**
     * Number of objects directly contained in this node.
     **/
    public int numObjects;
    /**
     * Total number of nodes contained in the subtree that has this node at its root.
     **/
    public int numChildrenRec;
    /**
     * Number of direct children nodes of this node.
     **/
    public int numChildren;
    /**
     * The parent, if any.
     **/
    public OctreeNode parent;
    /**
     * Children nodes.
     **/
    public OctreeNode[] children = new OctreeNode[8];
    /**
     * List of objects in this node.
     **/
    public List<IOctreeObject> objects;
    /**
     * The object used to load new octants.
     **/
    public IOctantLoader loader;
    /**
     * If observed, the view angle in radians of this octant.
     **/
    public double viewAngle;
    /**
     * The distance to the camera in units of the center of this octant.
     **/
    public double distToCamera;
    /**
     * Is this octant observed in this frame?
     **/
    public boolean observed;
    /**
     * The opacity of this node.
     **/
    public float opacity;
    ComponentTypes ct = new ComponentTypes(ComponentType.Others);
    com.badlogic.gdx.graphics.Color col = new com.badlogic.gdx.graphics.Color();
    /**
     * The load status of this node.
     **/
    private LoadStatus status;

    /**
     * Constructs an octree node.
     */
    private OctreeNode(double x,
                       double y,
                       double z,
                       double hsx,
                       double hsy,
                       double hsz,
                       int depth) {
        this.min = new Vector3d(x - hsx, y - hsy, z - hsz);
        this.max = new Vector3d(x + hsx, y + hsy, z + hsz);
        this.centre = new Vector3d(x, y, z);
        this.size = new Vector3d(hsx * 2, hsy * 2, hsz * 2);
        this.depth = depth;
        this.observed = false;
        this.status = LoadStatus.NOT_LOADED;
        this.radius = Math.sqrt(hsx * hsx + hsy * hsy + hsz * hsz);
    }

    /**
     * Constructs an octree node.
     */
    public OctreeNode(long pageId,
                      double x,
                      double y,
                      double z,
                      double hsx,
                      double hsy,
                      double hsz,
                      int depth) {
        this.pageId = pageId;
        this.min = new Vector3d(x - hsx, y - hsy, z - hsz);
        this.max = new Vector3d(x + hsx, y + hsy, z + hsz);
        this.centre = new Vector3d(x, y, z);
        this.size = new Vector3d(hsx * 2, hsy * 2, hsz * 2);
        this.depth = depth;
        this.observed = false;
        this.status = LoadStatus.NOT_LOADED;
        this.radius = Math.sqrt(hsx * hsx + hsy * hsy + hsz * hsz);
    }

    /**
     * Constructs an octree node.
     */
    public OctreeNode(double x,
                      double y,
                      double z,
                      double hsx,
                      double hsy,
                      double hsz,
                      int depth,
                      OctreeNode parent,
                      int i) {
        this(x, y, z, hsx, hsy, hsz, depth);
        this.parent = parent;
        parent.children[i] = this;
        this.pageId = computePageId();
    }

    /**
     * Constructs an octree node.
     *
     * @param x             The x coordinate of the center.
     * @param y             The y coordinate of the center.
     * @param z             The z coordinate of the center.
     * @param hsx           The half-size in x.
     * @param hsy           The half-size in y.
     * @param hsz           The half-size in z.
     * @param childrenCount Number of children nodes. Same as non-null positions in
     *                      children vector.
     * @param nObjects      Number of objects contained in this node and its descendants.
     * @param ownObjects    Number of objects contained in this node. Same as
     *                      objects.size().
     */
    public OctreeNode(double x,
                      double y,
                      double z,
                      double hsx,
                      double hsy,
                      double hsz,
                      int childrenCount,
                      int nObjects,
                      int ownObjects,
                      int depth) {
        this(x, y, z, hsx, hsy, hsz, depth);
        this.numChildren = childrenCount;
        this.numObjectsRec = nObjects;
        this.numObjects = ownObjects;
    }

    /**
     * Constructs an octree node.
     *
     * @param pageid        The octant id.
     * @param x             The x coordinate of the center.
     * @param y             The y coordinate of the center.
     * @param z             The z coordinate of the center.
     * @param hsx           The half-size in x.
     * @param hsy           The half-size in y.
     * @param hsz           The half-size in z.
     * @param childrenCount Number of children nodes. Same as non-null positions in
     *                      children vector.
     * @param nObjects      Number of objects contained in this node and its descendants.
     * @param ownObjects    Number of objects contained in this node. Same as
     *                      objects.size().
     */
    public OctreeNode(long pageid,
                      double x,
                      double y,
                      double z,
                      double hsx,
                      double hsy,
                      double hsz,
                      int childrenCount,
                      int nObjects,
                      int ownObjects,
                      int depth) {
        this(pageid, x, y, z, hsx, hsy, hsz, depth);
        this.numChildren = childrenCount;
        this.numObjectsRec = nObjects;
        this.numObjects = ownObjects;
    }

    public static long hash(double x,
                            double y,
                            double z) {
        long result = 3;
        result = result * 31 + hash(x);
        result = result * 31 + hash(y);
        result = result * 31 + hash(z);

        return result;
    }

    /**
     * Returns an integer hash code representing the given double value.
     *
     * @param value the value to be hashed
     * @return the hash code
     */
    public static long hash(double value) {
        long bits = Double.doubleToLongBits(value);
        return (bits ^ (bits >>> 32));
    }

    public long computePageId() {
        StringBuilder id = new StringBuilder();
        computePageIdRec(id);
        return Parser.parseLong(id.toString());
    }

    protected void computePageIdRec(StringBuilder id) {
        if (depth == 0) {
            return;
        }
        if (parent != null) {
            parent.computePageIdRec(id);
            id.append(getParentIndex() + 1);
        }
    }

    /**
     * Gets the index of this node in the parent's list.
     **/
    protected int getParentIndex() {
        if (parent != null) {
            for (int i = 0; i < 8; i++) {
                if (parent.children[i] == this)
                    return i;
            }
        }
        return 0;
    }

    public boolean containsObject(IOctreeObject object) {
        boolean has = this.objects.contains(object);
        if (!has && children != null && numChildren > 0) {
            for (OctreeNode child : children) {
                if (child != null) {
                    if (containsObject(object))
                        return true;
                }
            }
        }
        return has;
    }

    /**
     * Resolves and adds the children of this node using the map. It runs
     * recursively once the children have been added.
     *
     * @param map The children map.
     */
    public void resolveChildren(Map<Long, Pair<OctreeNode, long[]>> map) {
        Pair<OctreeNode, long[]> me = map.get(pageId);
        if (me == null) {
            throw new RuntimeException("OctreeNode with page ID " + pageId + " not found in map.");
        }

        long[] childrenIds = me.getSecond();
        int i = 0;
        for (long childId : childrenIds) {
            if (childId != -1) {
                // Child exists
                OctreeNode child = map.get(childId).getFirst();
                children[i] = child;
                child.parent = this;
            } else {
                // No node in this position
            }
            i++;
        }

        // Recursive running
        for (OctreeNode child : children) {
            if (child != null) {
                child.resolveChildren(map);
            }
        }
    }

    public float getOpacity() {
        return opacity;
    }

    public boolean add(IOctreeObject e) {
        if (objects == null)
            objects = new ArrayList<>(1);
        objects.add(e);

        numObjects = objects.size();
        if (e instanceof OctreeObjectView) {
            var entity = ((OctreeObjectView) e).getEntity();
            if (Mapper.starSet.has(entity)) {
                numObjects = objects.size() - 1 + Mapper.starSet.get(entity).data().size();
            } else if (Mapper.particleSet.has(entity)) {
                numObjects = objects.size() - 1 + Mapper.particleSet.get(entity).data().size();
            }
        }
        return true;
    }

    public boolean addAll(List<IOctreeObject> l) {
        if (objects == null)
            objects = new ArrayList<>(l.size());
        objects.addAll(l);
        numObjects = objects.size();
        return true;
    }

    public void setObjects(List<IOctreeObject> l) {
        this.objects = l;
        numObjects = objects.size();
    }

    public boolean insert(IOctreeObject e,
                          int level) {
        int node = 0;
        if (e.getPosition().y.doubleValue() > min.y + ((max.y - min.y) / 2))
            node += 4;
        if (e.getPosition().z.doubleValue() > min.z + ((max.z - min.z) / 2))
            node += 2;
        if (e.getPosition().x.doubleValue() > min.x + ((max.x - min.x) / 2))
            node += 1;
        if (level == this.depth + 1) {
            return children[node].add(e);
        } else {
            return children[node].insert(e, level);
        }
    }

    public void toTree(TreeSet<IOctreeObject> tree) {
        tree.addAll(objects);
        if (children != null) {
            for (int i = 0; i < 8; i++) {
                children[i].toTree(tree);
            }
        }
    }

    /**
     * Adds all the children of this node and its descendants to the given list.
     *
     * @param list The tree of nodes.
     */
    public void addChildrenToList(ArrayList<OctreeNode> list) {
        if (children != null) {
            for (int i = 0; i < 8; i++) {
                if (children[i] != null) {
                    list.add(children[i]);
                    children[i].addChildrenToList(list);
                }
            }
        }
    }

    /**
     * Adds all the particles of this node and its descendants to the given
     * list.
     *
     * @param particles The list of particles.
     */
    public void addParticlesTo(Array<IOctreeObject> particles) {
        if (this.objects != null) {
            for (IOctreeObject elem : this.objects)
                particles.add(elem);
        }
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                children[i].addParticlesTo(particles);
            }
        }
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean rec) {
        StringBuilder str = new StringBuilder(depth);
        if (rec)
            str.append("  ".repeat(Math.max(0, depth)));

        int idx = parent != null ? Arrays.asList(parent.children).indexOf(this) : 0;
        str.append(idx).append(":L").append(depth).append(" ");
        str.append("id:").append(pageId);
        str.append(" Obj(own/rec):(").append(numObjects).append("/").append(numObjectsRec).append(")");
        str.append(" Nchld:").append(numChildren).append("\n");

        if (numChildren > 0 && rec) {
            for (OctreeNode child : children) {
                if (child != null) {
                    str.append(child.toString(true));
                }
            }
        }
        return str.toString();
    }

    /**
     * Gets some per-level stats on the octree node.
     *
     * @return A [DEPTH,2] matrix with number of octants [i,0] and objects [i,1] per level.
     */
    public int[][] stats() {
        int[][] result = new int[getMaxDepth()][2];
        statsRec(result);
        return result;
    }

    private void statsRec(int[][] mat) {
        mat[this.depth][0] += 1;
        mat[this.depth][1] += this.numObjects;

        for (OctreeNode child : children) {
            if (child != null) {
                child.statsRec(mat);
            }
        }
    }

    /**
     * Removes this octant from the octree.
     */
    public void remove() {
        if (this.parent != null)
            this.parent.removeChild(this);
    }

    /**
     * Removes the child from this octant's descendants.
     *
     * @param child The child node to remove.
     */
    public void removeChild(OctreeNode child) {
        if (children != null)
            for (int i = 0; i < children.length; i++) {
                if (children[i] != null && children[i] == child) {
                    child.parent = null;
                    children[i] = null;
                }
            }
    }

    /**
     * Counts the number of direct children of this node.
     *
     * @return The number of direct children.
     */
    public int numChildren() {
        int num = 0;
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                num++;
            }
        }
        return num;
    }

    /**
     * Counts the number of nodes recursively.
     *
     * @return The number of nodes.
     */
    public int numNodesRec() {
        int numNodes = 1;
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                numNodes += children[i].numNodesRec();
            }
        }
        return numNodes;
    }

    @Override
    public ComponentTypes getComponentType() {
        return ct;
    }

    @Override
    public double getDistToCamera() {
        return 0;
    }

    public boolean contains(double x,
                            double y,
                            double z) {
        return min.x <= x && max.x >= x && min.y <= y && max.y >= y && min.z <= z && max.z >= z;
    }

    public boolean contains(Vector3d v) {
        return min.x <= v.x && max.x >= v.x && min.y <= v.y && max.y >= v.y && min.z <= v.z && max.z >= v.z;
    }

    /**
     * Returns the deepest octant that contains the position.
     *
     * @param position The position.
     * @return The best octant.
     */
    public OctreeNode getBestOctant(Vector3d position) {
        if (!this.contains(position)) {
            return null;
        } else {
            OctreeNode candidate = null;
            for (int i = 0; i < 8; i++) {
                OctreeNode child = children[i];
                if (child != null) {
                    candidate = child.getBestOctant(position);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
            // We could not found a candidate in our children, we use this node.
            return this;
        }
    }

    /**
     * Gets the depth of this subtree, that is, the number of levels of the
     * longest parent-child path starting at this node. If run on the root node,
     * this gives the maximum octree depth.
     *
     * @return The maximum depth of this node.
     */
    public int getMaxDepth() {
        int maxChildrenDepth = 0;
        if (children != null) {
            for (OctreeNode child : children) {
                if (child != null) {
                    int d = child.getMaxDepth();
                    if (d > maxChildrenDepth) {
                        maxChildrenDepth = d;
                    }
                }
            }
        }
        return maxChildrenDepth + 1;
    }

    /**
     * Computes the observed value and the transform of each observed node.
     *
     * @param parentTransform The parent transform.
     * @param cam             The current camera.
     * @param roulette        List where the nodes to be processed are to be added.
     * @param opacity         The opacity to set.
     */
    public void update(Vector3b parentTransform,
                       ICamera cam,
                       List<IOctreeObject> roulette,
                       float opacity) {
        this.opacity = opacity;
        this.observed = false;

        // Compute distance and view angle
        distToCamera = auxD1.set(centre).add(cam.getInversePos()).len();
        // View angle is normalized to 40 degrees when the octant is exactly the size of the screen height, regardless of the camera fov
        viewAngle = Math.atan(radius / distToCamera) * 2;

        float cf = MathUtilsDouble.clamp(cam.getFovFactor() * 2.5f, 0.15f, 1f);
        float th0 = Settings.settings.scene.octree.threshold[0] * cf;
        float th1 = Settings.settings.scene.octree.threshold[1] * cf;

        var isCameraFocus = hasFocusObject(cam);
        if (viewAngle < th0 && !isCameraFocus) {
            // Not observed
            setChildrenObserved(false);
        } else if ((this.observed = computeObserved(cam)) || isCameraFocus) {
            nOctantsObserved++;
            /*
             * Load lists of pages
             */
            if (status == LoadStatus.NOT_LOADED && Settings.settings.runtime.octreeLoadActive) {
                // Add to load and go on
                assert loader != null : "Octant loader is null!";
                loader.queue(this);
            } else if (status == LoadStatus.LOADED) {
                // Visited last!
                assert loader != null : "Octant loader is null!";
                loader.touch(this);

                // Add objects
                addObjectsTo(roulette);
            }  // What do? Move first in queue?

            double alpha = 1;
            if (Settings.settings.scene.octree.fade && viewAngle < th1) {
                alpha = MathUtilsDouble.clamp(MathUtilsDouble.lint(viewAngle, th0, th1, 0d, 1d), 0f, 1f);
            }
            this.opacity *= alpha;

            // Update children
            for (int i = 0; i < 8; i++) {
                OctreeNode child = children[i];
                if (child != null) {
                    child.update(parentTransform, cam, roulette, this.opacity);
                }
            }

        }
    }

    /**
     * Check if this octree node contains the current focus object of the given camera.
     *
     * @param camera The camera.
     * @return True if this octree node contains the camera's focus object.
     */
    private boolean hasFocusObject(ICamera camera) {
        return objects != null && objects.stream().anyMatch((o) -> camera.isFocus(((OctreeObjectView) o).getEntity()));
    }

    private void addObjectsTo(List<IOctreeObject> roulette) {
        if (objects != null) {
            roulette.addAll(objects);
            for (IOctreeObject obj : objects) {
                nObjectsObserved += obj.getStarCount();
            }
        }
    }

    private void setChildrenObserved(boolean observed) {
        for (int i = 0; i < 8; i++) {
            OctreeNode child = children[i];
            if (child != null) {
                child.observed = observed;
            }
        }
    }

    public boolean isObserved() {
        return observed && (parent == null || parent.isObserved());
    }

    /**
     * Second method, which uses a simplification.
     *
     * @param cam The camera
     * @return Whether the octant is observed
     */
    private boolean computeObserved(ICamera cam) {
        return computeObservedFast(cam);
    }

    /**
     * Simplification to compute octant visibility. Angle between camera direction and octant centre
     * must be smaller than fov/2 plus a correction (approximates octants to spheres)
     *
     * @param cam The camera
     * @return Whether the octant is observed
     */
    private boolean computeObservedFast(ICamera cam) {
        // vector from camera to center of box
        Vector3d cpospos = auxD1.set(centre).sub(cam.getPos());
        // auxD2 rotation axis
        Vector3d axis = auxD2.set(cam.getDirection()).crs(centre);
        Vector3d edge = auxD3.set(cam.getDirection()).rotate(axis, cam.getCamera().fieldOfView / 2d);
        // get angle at edge (when far side is radius)
        double angle1 = FastMath.toDegrees(FastMath.atan(radius / cpospos.len()));
        // get actual angle
        double angle2 = edge.angle(cpospos);
        // We're in the containing sphere or centre is in front of us
        return distToCamera <= radius || angle2 < angle1;
    }

    public LoadStatus getStatus() {
        return status;
    }

    public void setStatus(final LoadStatus status) {
        synchronized (this) {
            this.status = status;
        }
    }

    /**
     * Sets the status to this node and its descendants recursively to the given
     * depth level.
     *
     * @param status The new status.
     * @param depth  The depth.
     */
    public void setStatus(LoadStatus status,
                          int depth) {
        if (depth >= this.depth) {
            setStatus(status);
            for (int i = 0; i < 8; i++) {
                OctreeNode child = children[i];
                if (child != null) {
                    child.setStatus(status, depth);
                }
            }
        }
    }

    /**
     * Called when this octant has just been loaded or unloaded. Updates the numbers of this
     * and its ascendants without having to reprocess the whole tree, which is slow for larger trees.
     *
     * @param n The number of stars loaded or unloaded.
     **/
    public synchronized void touch(int n) {
        if (status == LoadStatus.NOT_LOADED) {
            // We unloaded n stars
            this.numObjects = 0;
            this.numObjectsRec = 0;
            if (this.parent != null) {
                this.parent.numChildren--;
                this.parent.touchRec(-n);
            }
        } else if (status == LoadStatus.LOADED) {
            this.numObjects = n;
            this.numObjectsRec = n;
            if (this.parent != null) {
                this.parent.numChildren++;
                this.parent.touchRec(n);
            }
        }
    }

    private synchronized void touchRec(int n) {
        this.numObjectsRec += n;
        if (this.parent != null)
            this.parent.touchRec(n);
    }

    /**
     * Updates the number of objects, own objects and children. This operation
     * runs recursively in depth.
     */
    public void updateCounts() {
        // Number of own objects.
        this.numObjects = 0;
        if (objects != null) {
            for (IOctreeObject ape : objects) {
                this.numObjects += ape.getStarCount();
            }
        }

        // Number of recursive objects.
        this.numObjectsRec = this.numObjects;

        // Children counts.
        this.numChildren = 0;
        this.numChildrenRec = 0;
        for (int i = 0; i < 8; i++) {
            if (children[i] != null) {
                this.numChildren++;
                // Recursive call
                children[i].updateCounts();
                numObjectsRec += children[i].numObjectsRec;
                numChildrenRec += 1 + children[i].numChildrenRec;
            }
        }

    }

    public int countObjects() {
        int n = 0;
        if (objects != null) {
            for (IOctreeObject obj : objects) {
                n += obj.getStarCount();
            }
        }

        if (children != null)
            for (OctreeNode child : children) {
                if (child != null)
                    n += child.countObjects();
            }

        return n;
    }

    public OctreeNode findOctant(long id) {
        if (this.pageId == id)
            return this;
        else {
            if (this.children != null) {
                OctreeNode target = null;
                for (OctreeNode child : children) {
                    if (child != null) {
                        target = child.findOctant(id);
                        if (target != null)
                            return target;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the root of the tree this octant is in by successively
     * checking the parent until it is null.
     *
     * @return The root of the tree
     */
    public OctreeNode getRoot() {
        if (parent == null)
            return this;
        else
            return parent.getRoot();
    }

    @Override
    public void render(LineRenderSystem sr,
                       ICamera camera,
                       float alpha) {
        if (this.observed) {
            this.col.set(ColorUtils.gGreenC);
        } else {
            this.col.set(ColorUtils.gYellowC);
        }
        this.col.a = alpha * opacity;

        if (this.col.a > 0) {
            // Camera correction
            Vector3d loc = auxD4;
            loc.set(this.min).add(camera.getInversePos());

            /*
             * .·------· .' | .'| +---+--·' | | | | | | ,+--+---· |.' | .'
             * +------+'
             */
            line(sr, loc.x, loc.y, loc.z, loc.x + size.x, loc.y, loc.z, this.col);
            line(sr, loc.x, loc.y, loc.z, loc.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x, loc.y, loc.z, loc.x, loc.y, loc.z + size.z, this.col);

            /*
             * .·------· .' | .'| ·---+--+' | | | | | | ,·--+---+ |.' | .'
             * ·------+'
             */
            line(sr, loc.x + size.x, loc.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x + size.x, loc.y, loc.z, loc.x + size.x, loc.y, loc.z + size.z, this.col);

            /*
             * .·------+ .' | .'| ·---+--·' | | | | | | ,+--+---+ |.' | .'
             * ·------·'
             */
            line(sr, loc.x + size.x, loc.y, loc.z + size.z, loc.x, loc.y, loc.z + size.z, this.col);
            line(sr, loc.x + size.x, loc.y, loc.z + size.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);

            /*
             * .+------· .' | .'| ·---+--·' | | | | | | ,+--+---· |.' | .'
             * ·------·'
             */
            line(sr, loc.x, loc.y, loc.z + size.z, loc.x, loc.y + size.y, loc.z + size.z, this.col);

            /*
             * .+------+ .' | .'| +---+--+' | | | | | | ,·--+---· |.' | .'
             * ·------·'
             */
            line(sr, loc.x, loc.y + size.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z, this.col);
            line(sr, loc.x, loc.y + size.y, loc.z, loc.x, loc.y + size.y, loc.z + size.z, this.col);
            line(sr, loc.x, loc.y + size.y, loc.z + size.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);
            line(sr, loc.x + size.x, loc.y + size.y, loc.z, loc.x + size.x, loc.y + size.y, loc.z + size.z, this.col);
        }
    }

    /**
     * Draws a line
     **/
    private void line(LineRenderSystem sr,
                      double x1,
                      double y1,
                      double z1,
                      double x2,
                      double y2,
                      double z2,
                      com.badlogic.gdx.graphics.Color col) {
        sr.addLine(this, (float) x1, (float) y1, (float) z1, (float) x2, (float) y2, (float) z2, col);
    }

    /**
     * Disposes this octree node (and all children nodes recursively)
     */
    public void dispose() {
        if (objects != null)
            for (IOctreeObject object : objects) {
                if (object != null)
                    object.dispose();
            }
        if (children != null)
            for (OctreeNode child : children) {
                if (child != null)
                    child.dispose();
            }
    }

    @Override
    public float getLineWidth() {
        return 1;
    }

    @Override
    public int getGlPrimitive() {
        return GL20.GL_LINE_STRIP;
    }

    /**
     * Sets the octant loader to this node. Optionally, the loader can be set recursively to
     * all children octree nodes of this node.
     *
     * @param loader    The loader to set.
     * @param recursive Whether to set the loader recursively to the children nodes.
     */
    public void setOctantLoader(IOctantLoader loader,
                                boolean recursive) {
        this.loader = loader;
        if (recursive && children != null) {
            for (OctreeNode child : children) {
                if (child != null) {
                    child.setOctantLoader(loader, true);
                }
            }
        }
    }

    /**
     * Sets the octree loader to this node. It does <strong>not</strong> set the loader of
     * children recursively.
     *
     * @param loader The loader to set.
     */
    public void setOctantLoader(IOctantLoader loader) {
        setOctantLoader(loader, false);
    }
}
